# Pending Payments System - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Data Model](#data-model)
4. [Formulas and Calculations](#formulas-and-calculations)
5. [Business Logic](#business-logic)
6. [Settlement System](#settlement-system)
7. [Display Logic](#display-logic)
8. [CSV Export](#csv-export)
9. [Implementation Details](#implementation-details)

---

## Overview

The Pending Payments system tracks outstanding financial obligations between the admin and clients based on trading performance. It calculates profit/loss shares and tracks settlement progress using a **Masked Share Settlement System** with locked share amounts.

### Key Features
- **Two-Section Display**: "Clients Owe You" (losses) and "You Owe Clients" (profits)
- **Locked Share System**: Share amounts are locked at first calculation to prevent shrinking after payments
- **Cycle-Based Tracking**: Separate settlement cycles for profit and loss periods
- **N.A. Handling**: Displays "N.A" for zero PnL or zero share cases
- **CSV Export**: Export functionality matching the UI display

---

## Core Concepts

### 1. Client PnL (Profit/Loss)
The fundamental calculation that determines whether a client owes money or is owed money.

**Formula:**
```
Client_PnL = Exchange_Balance - Funding
```

**Interpretation:**
- `Client_PnL < 0`: Client is in LOSS → Client owes admin
- `Client_PnL > 0`: Client is in PROFIT → Admin owes client
- `Client_PnL = 0`: Neutral (no profit/loss) → Show as N.A

### 2. Share Percentage
The percentage of profit/loss that belongs to the admin.

**Rules:**
- **Loss Case** (`Client_PnL < 0`): Uses `loss_share_percentage` if set and > 0, else falls back to `my_percentage`
- **Profit Case** (`Client_PnL > 0`): Uses `profit_share_percentage` if set and > 0, else falls back to `my_percentage`
- **Zero Case** (`Client_PnL = 0`): Returns 0 (no share on zero PnL)

**Formula:**
```python
if client_pnl < 0:
    share_pct = loss_share_percentage if loss_share_percentage > 0 else my_percentage
elif client_pnl > 0:
    share_pct = profit_share_percentage if profit_share_percentage > 0 else my_percentage
else:
    share_pct = 0
```

### 3. Final Share (My Share)
The admin's share amount calculated from the client's PnL.

**Formula:**
```
Exact_Share = |Client_PnL| × (Share_Percentage / 100)
Final_Share = floor(Exact_Share)  # Round DOWN (floor)
```

**Important Notes:**
- Uses **floor rounding** (round down) for final share
- Always returns a positive value (uses absolute value of PnL)
- Returns 0 if `Client_PnL = 0`

---

## Data Model

### ClientExchangeAccount Fields

#### Core Money Fields
- `funding` (BigInteger): Total real money given to client
- `exchange_balance` (BigInteger): Current balance on exchange

#### Percentage Fields
- `my_percentage` (Decimal 5,2): Default admin share percentage (0-100, decimals allowed)
- `loss_share_percentage` (Integer): Admin share percentage for losses (0-100)
- `profit_share_percentage` (Integer): Admin share percentage for profits (0-100)

#### Locked Share Fields (Cycle Management)
- `locked_initial_final_share` (BigInteger): Initial FinalShare locked at start of PnL cycle
- `locked_share_percentage` (Integer): Share percentage locked at start of PnL cycle
- `locked_initial_pnl` (BigInteger): Initial PnL when share was locked
- `cycle_start_date` (DateTime): Timestamp when current PnL cycle started
- `locked_initial_funding` (BigInteger): Funding amount when share was locked

### Settlement Model
- `amount` (Decimal): Settlement payment amount
- `date` (Date): Settlement date
- `notes` (Text): Optional notes

---

## Formulas and Calculations

### 1. Client PnL Calculation

**Method:** `compute_client_pnl()`

**Formula:**
```python
Client_PnL = exchange_balance - funding
```

**Example:**
- Funding: ₹10,000,000
- Exchange Balance: ₹9,000,492
- Client_PnL = 9,000,492 - 10,000,000 = **-999,508** (Loss)

### 2. Share Percentage Selection

**Method:** `get_share_percentage(client_pnl)`

**Logic:**
```python
if client_pnl < 0:  # LOSS
    if loss_share_percentage > 0:
        return loss_share_percentage
    else:
        return my_percentage
elif client_pnl > 0:  # PROFIT
    if profit_share_percentage > 0:
        return profit_share_percentage
    else:
        return my_percentage
else:  # ZERO
    return 0
```

**Example:**
- `loss_share_percentage = 15`
- `my_percentage = 12`
- `client_pnl = -999,508` (Loss)
- Result: **15%** (uses loss_share_percentage)

### 3. Final Share Calculation

**Method:** `compute_my_share()`

**Formula:**
```python
if Client_PnL == 0:
    return 0

Share_Percentage = get_share_percentage(Client_PnL)
Exact_Share = abs(Client_PnL) × (Share_Percentage / 100.0)
Final_Share = floor(Exact_Share)  # Round DOWN
```

**Example:**
- Client_PnL = -999,508 (Loss)
- Share_Percentage = 15%
- Exact_Share = 999,508 × 0.15 = 149,926.2
- Final_Share = floor(149,926.2) = **149,926**

### 4. Remaining Settlement Amount

**Method:** `get_remaining_settlement_amount()`

**Formula:**
```python
# Lock share if needed (first time calculation)
lock_initial_share_if_needed()

# Get locked share
Initial_Final_Share = locked_initial_final_share

# Sum settlements from current cycle only
if cycle_start_date exists:
    Total_Settled = SUM(settlements WHERE date >= cycle_start_date)
else:
    Total_Settled = SUM(all settlements)

# Calculate remaining
Remaining_Raw = max(0, Initial_Final_Share - Total_Settled)
Overpaid = max(0, Total_Settled - Initial_Final_Share)
```

**Important:**
- Uses **locked share**, not current calculated share
- Only counts settlements from **current cycle**
- Returns raw value (always ≥ 0)
- Sign is applied at display time based on PnL direction

**Example:**
- Initial_Final_Share = 149,926 (locked)
- Total_Settled = 9,995 (from current cycle)
- Remaining_Raw = max(0, 149,926 - 9,995) = **139,931**

### 5. Display Remaining Amount

**Method:** `calculate_display_remaining(client_pnl, remaining_amount)`

**Formula:**
```python
if Client_PnL > 0:  # PROFIT - You owe client
    Display_Remaining = -Remaining_Raw  # Negative
else:  # LOSS - Client owes you
    Display_Remaining = +Remaining_Raw  # Positive
```

**Example:**
- Client_PnL = -999,508 (Loss)
- Remaining_Raw = 139,931
- Display_Remaining = **+139,931** (positive, client owes you)

---

## Business Logic

### 1. Share Locking System

**Purpose:** Prevent share from shrinking after payments. Share is decided by trading outcome, not by settlement.

**When Share is Locked:**
1. **First Calculation**: When `locked_initial_final_share` is None and `compute_my_share() > 0`
2. **PnL Cycle Change**: When PnL sign flips (LOSS → PROFIT or PROFIT → LOSS)
3. **Funding Change**: When funding amount changes (new exposure = new cycle)
4. **PnL Magnitude Reduction**: When PnL magnitude reduces significantly (trading reduced exposure)

**Lock Process:**
```python
def lock_initial_share_if_needed():
    client_pnl = compute_client_pnl()
    
    # Check for cycle reset conditions
    if funding_changed or pnl_magnitude_reduced:
        reset_cycle()
    
    # Check for PnL sign flip
    if locked_initial_pnl exists and sign_flipped:
        lock_new_share()
    
    # First time lock
    if locked_initial_final_share is None:
        final_share = compute_my_share()
        if final_share > 0:
            lock_initial_final_share = final_share
            locked_share_percentage = get_share_percentage(client_pnl)
            locked_initial_pnl = client_pnl
            cycle_start_date = now()
            locked_initial_funding = funding
```

### 2. Cycle Management

**PnL Cycle:** A period where PnL maintains the same sign (all profit or all loss).

**Cycle Reset Conditions:**
1. **Sign Flip**: PnL changes from profit to loss or vice versa
2. **Funding Change**: New funding added (new exposure)
3. **PnL Magnitude Reduction**: Significant reduction in PnL magnitude (trading reduced exposure)
4. **Zero PnL with Full Settlement**: PnL becomes zero and all settlements are complete

**Cycle Tracking:**
- `cycle_start_date`: Timestamp when current cycle started
- Settlements are filtered by `cycle_start_date` to only count current cycle payments
- Old cycle settlements don't mix with new cycle shares

### 3. N.A. Display Logic

**When to Show N.A:**
1. **Zero PnL**: `Client_PnL = 0` (neutral case)
2. **Zero Final Share**: `Final_Share = 0` (even if PnL ≠ 0)

**Display Rules:**
- **PROFIT(+)/LOSS(-)**: Show "N.A" if `Client_PnL = 0` or `show_na = True`
- **MY SHARE**: Show "N.A" if `Final_Share = 0` or `show_na = True`
- **MY%**: Show actual percentage even when PnL = 0 (use `my_percentage` directly, not `get_share_percentage()`)

**Example:**
- Client_PnL = 0
- Funding = 0
- Exchange Balance = 0
- MY% = 12.00 (still shows 12.00%, not 0%)
- PROFIT(+)/LOSS(-) = "N.A"
- MY SHARE = "N.A"

### 4. Section Classification

**Clients Owe You Section:**
- Condition: `Client_PnL < 0` (Loss case)
- Interpretation: Client owes admin money
- Display Remaining: Positive value
- Includes: Zero PnL cases (shown as N.A)

**You Owe Clients Section:**
- Condition: `Client_PnL > 0` (Profit case)
- Interpretation: Admin owes client money
- Display Remaining: Negative value (shown as positive in UI)
- Includes: Zero Final Share cases (shown as N.A)

---

## Settlement System

### Settlement Calculation

**Remaining Amount Formula:**
```
Remaining_Raw = max(0, Locked_Initial_Final_Share - Total_Settled)
```

**Overpaid Amount Formula:**
```
Overpaid = max(0, Total_Settled - Locked_Initial_Final_Share)
```

### Settlement Filtering

**Current Cycle Only:**
- Only settlements with `date >= cycle_start_date` are counted
- Prevents mixing old cycle settlements with new cycle shares
- When cycle resets, old settlements are excluded

**Example:**
- Cycle started: 2026-01-01
- Settlement 1: 2025-12-15 (old cycle) → **NOT counted**
- Settlement 2: 2026-01-10 (current cycle) → **Counted**
- Settlement 3: 2026-01-20 (current cycle) → **Counted**

### Settlement Display

**Display Logic:**
```python
if Client_PnL < 0:  # LOSS
    Display_Remaining = +Remaining_Raw  # Client owes you
elif Client_PnL > 0:  # PROFIT
    Display_Remaining = -Remaining_Raw  # You owe client
else:  # ZERO
    Display_Remaining = 0  # Show as N.A
```

**UI Display:**
- Always shows absolute value (positive) in UI
- Sign interpretation is handled by section (Clients Owe You vs You Owe Clients)

---

## Display Logic

### Pending Summary View

**Data Structure:**
```python
clients_owe_list = [
    {
        "client": Client,
        "exchange": Exchange,
        "account": ClientExchangeAccount,
        "client_pnl": -999508,  # Negative (loss)
        "amount_owed": 999508,  # Absolute value
        "my_share_amount": 149926,  # Final share
        "remaining_amount": 139931,  # Absolute value for display
        "share_percentage": 15.00,
        "show_na": False
    }
]

you_owe_list = [
    {
        "client": Client,
        "exchange": Exchange,
        "account": ClientExchangeAccount,
        "client_pnl": 50000,  # Positive (profit)
        "amount_owed": 50000,  # Profit amount
        "my_share_amount": 7500,  # Final share
        "remaining_amount": 5000,  # Absolute value for display
        "share_percentage": 15.00,
        "show_na": False
    }
]
```

### Column Display

**Table Columns:**
1. **Client**: Client name (link to detail page)
2. **U_CODE**: Client code
3. **Master**: Exchange name (and code if available)
4. **OPENING POINTS**: `funding` (formatted with currency)
5. **AVL.POINTS(CLOSING POINTS)**: `exchange_balance` (formatted with currency)
6. **PROFIT(+)/LOSS(-)**: 
   - If `show_na = True`: "N.A"
   - Else: `client_pnl` (formatted with currency, shows sign)
7. **MY SHARE**: 
   - If `show_na = True`: "N.A"
   - Else: `my_share_amount` (formatted with currency)
8. **MY%**: `share_percentage` (always shows percentage, even if N.A)

### Totals Calculation

**Total Clients Owe:**
```python
total_clients_owe = SUM(item["amount_owed"] for item in clients_owe_list)
total_my_share_clients_owe = SUM(item["remaining_amount"] for item in clients_owe_list)
```

**Total You Owe:**
```python
total_you_owe = SUM(item["amount_owed"] for item in you_owe_list)
total_my_share_you_owe = SUM(item["remaining_amount"] for item in you_owe_list)
```

### Sorting

**Sort Key Function:**
```python
def get_sort_key(item):
    if item["show_na"]:
        return 0  # N.A items sort to bottom
    if "my_share_amount" in item:
        return abs(item["my_share_amount"])
    elif "amount_owed" in item:
        return abs(item["amount_owed"])
    elif "client_pnl" in item:
        return abs(item["client_pnl"])
    else:
        return 0
```

**Sort Order:** Descending (largest amounts first, N.A items at bottom)

---

## CSV Export

### Export Format

**File Name:** `pending_payments_YYYYMMDD.csv`

**Columns:**
1. **Period**: Date (only in first row, empty in subsequent rows)
2. **U_CODE**: Client code
3. **Master**: Exchange name
4. **OPENING POINTS**: Funding amount
5. **AVL.POINTS(CLOSING POINTS)**: Exchange balance
6. **PROFIT(+)/LOSS(-)**: Client PnL (or "N.A")
7. **MY SHARE**: Final share amount (or "N.A")
8. **MY%**: Share percentage

### CSV Generation Logic

**Header Row:**
```python
headers = [
    'Period',
    'U_CODE',
    'Master',
    'OPENING POINTS',
    'AVL.POINTS(CLOSING POINTS)',
    'PROFIT(+)/LOSS(-)',
    'MY SHARE',
    'MY%'
]
```

**Data Rows:**
```python
first_row = True
for item in clients_owe_list + you_owe_list:
    row_data = [
        today_date if first_row else '',  # Period only in first row
        item["client"].code or '',
        item["exchange"].name or '',
        int(item["account"].funding),
        int(item["account"].exchange_balance),
        'N.A' if item["show_na"] else int(item["client_pnl"]),
        'N.A' if item["show_na"] else int(item["my_share_amount"]),
        item["share_percentage"]
    ]
    writer.writerow(row_data)
    first_row = False
```

**Example CSV Output:**
```csv
Period,U_CODE,Master,OPENING POINTS,AVL.POINTS(CLOSING POINTS),PROFIT(+)/LOSS(-),MY SHARE,MY%
2026-01-12,VIJ77&EXC,VIJEXCHV1,10000000,9000492,-999508,149926,15.00
,VIJ77&EXC,VIJETHA77 V2,0,0,N.A,N.A,12.00
```

---

## Implementation Details

### View Function: `pending_summary()`

**Location:** `/root/Chips_dashboard/chip_dashboard/core/views.py`

**Process Flow:**
1. Get date range based on report type (daily/weekly/monthly)
2. Filter client exchanges by user and search query
3. For each client exchange:
   - Calculate `Client_PnL` using `compute_client_pnl()`
   - Determine section (loss/profit/neutral)
   - Lock share if needed using `lock_initial_share_if_needed()`
   - Get remaining settlement using `get_remaining_settlement_amount()`
   - Calculate display remaining using `calculate_display_remaining()`
   - Add to appropriate list (clients_owe_list or you_owe_list)
4. Sort lists by amount (descending)
5. Calculate totals
6. Render template with context

### Helper Functions

**`calculate_display_remaining(client_pnl, remaining_amount)`**
- Applies sign to remaining amount based on PnL direction
- Returns signed value for display

**`get_settlement_info_for_display(client_exchange)`**
- Encapsulates common pattern of locking share and getting settlement info
- Returns dict with settlement details

### Model Methods

**`ClientExchangeAccount.compute_client_pnl()`**
- Returns: `exchange_balance - funding`

**`ClientExchangeAccount.get_share_percentage(client_pnl)`**
- Returns appropriate percentage based on PnL direction

**`ClientExchangeAccount.compute_my_share()`**
- Calculates final share using floor rounding
- Returns: `floor(abs(client_pnl) × (share_pct / 100))`

**`ClientExchangeAccount.lock_initial_share_if_needed()`**
- Locks share at first calculation
- Handles cycle resets
- Prevents share from shrinking

**`ClientExchangeAccount.get_remaining_settlement_amount()`**
- Calculates remaining using locked share
- Filters settlements by cycle
- Returns dict with remaining, overpaid, initial_final_share, total_settled

---

## Examples

### Example 1: Loss Case

**Input:**
- Funding: ₹10,000,000
- Exchange Balance: ₹9,000,492
- Loss Share Percentage: 15%
- Settlements: ₹9,995 (current cycle)

**Calculations:**
1. Client_PnL = 9,000,492 - 10,000,000 = **-999,508** (Loss)
2. Share_Percentage = 15% (uses loss_share_percentage)
3. Exact_Share = 999,508 × 0.15 = 149,926.2
4. Final_Share = floor(149,926.2) = **149,926** (locked)
5. Remaining_Raw = max(0, 149,926 - 9,995) = **139,931**
6. Display_Remaining = +139,931 (positive, client owes you)

**Display:**
- Section: **Clients Owe You**
- PROFIT(+)/LOSS(-): **-₹999,508**
- MY SHARE: **₹149,926**
- Remaining: **₹139,931**
- MY%: **15.00%**

### Example 2: Profit Case

**Input:**
- Funding: ₹5,000,000
- Exchange Balance: ₹5,500,000
- Profit Share Percentage: 15%
- Settlements: ₹2,500 (current cycle)

**Calculations:**
1. Client_PnL = 5,500,000 - 5,000,000 = **+500,000** (Profit)
2. Share_Percentage = 15% (uses profit_share_percentage)
3. Exact_Share = 500,000 × 0.15 = 75,000.0
4. Final_Share = floor(75,000.0) = **75,000** (locked)
5. Remaining_Raw = max(0, 75,000 - 2,500) = **72,500**
6. Display_Remaining = -72,500 (negative, you owe client, shown as positive in UI)

**Display:**
- Section: **You Owe Clients**
- PROFIT(+)/LOSS(-): **+₹500,000**
- MY SHARE: **₹75,000**
- Remaining: **₹72,500**
- MY%: **15.00%**

### Example 3: Zero PnL Case

**Input:**
- Funding: ₹0
- Exchange Balance: ₹0
- My Percentage: 12%

**Calculations:**
1. Client_PnL = 0 - 0 = **0** (Neutral)
2. Share_Percentage = 0 (zero PnL)
3. Final_Share = 0
4. Remaining_Raw = 0
5. Show_NA = True

**Display:**
- Section: **Clients Owe You**
- PROFIT(+)/LOSS(-): **N.A**
- MY SHARE: **N.A**
- Remaining: **N.A**
- MY%: **12.00%** (still shows percentage)

### Example 4: Zero Share Case

**Input:**
- Funding: ₹1,000,000
- Exchange Balance: ₹1,000,000
- Share Percentage: 0%

**Calculations:**
1. Client_PnL = 1,000,000 - 1,000,000 = **0** (Neutral)
2. Share_Percentage = 0%
3. Final_Share = 0
4. Show_NA = True

**Display:**
- Section: **Clients Owe You**
- PROFIT(+)/LOSS(-): **N.A**
- MY SHARE: **N.A**
- Remaining: **N.A**
- MY%: **0.00%**

---

## Important Notes

### 1. Share Locking
- Share is **locked at first calculation** and **never shrinks** after payments
- This ensures share is decided by trading outcome, not by settlement
- Lock resets only when cycle changes (sign flip, funding change, etc.)

### 2. Cycle Management
- Each PnL cycle (profit or loss period) has its own locked share
- Settlements are filtered by cycle start date
- Old cycle settlements don't mix with new cycle shares

### 3. Floor Rounding
- Final share uses **floor rounding** (round down)
- This ensures admin never gets more than calculated share
- Example: 149,926.9 → 149,926 (not 149,927)

### 4. N.A. Display
- Shows "N.A" for zero PnL or zero share cases
- MY% always shows actual percentage, even when N.A
- Clients always appear in pending list, even with N.A

### 5. Display Remaining Sign
- Sign is applied at display time based on PnL direction
- UI always shows absolute value (positive)
- Section determines interpretation (Clients Owe You vs You Owe Clients)

---

## Version History

- **2026-01-12**: Initial documentation created
- Includes all formulas, logic, and implementation details
- Covers Masked Share Settlement System
- Documents cycle management and share locking

---

## Contact

For questions or clarifications about the Pending Payments system, refer to:
- Code location: `/root/Chips_dashboard/chip_dashboard/core/views.py` (pending_summary function)
- Model location: `/root/Chips_dashboard/chip_dashboard/core/models.py` (ClientExchangeAccount class)


