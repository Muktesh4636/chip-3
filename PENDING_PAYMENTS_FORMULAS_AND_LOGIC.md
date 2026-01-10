# Pending Payments System - Complete Formulas and Logic Documentation

**Version:** 2.0  
**Last Updated:** January 2025  
**Status:** Production Ready

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Core Definitions](#core-definitions)
3. [Master Formulas](#master-formulas)
4. [Detailed Formula Explanations](#detailed-formula-explanations)
5. [Complete Logic Flows](#complete-logic-flows)
6. [Step-by-Step Calculations](#step-by-step-calculations)
7. [Code Implementation](#code-implementation)
8. [Examples with Real Numbers](#examples-with-real-numbers)
9. [Edge Cases and Special Scenarios](#edge-cases-and-special-scenarios)
10. [Validation Rules](#validation-rules)

---

## System Overview

The Pending Payments System uses a **Masked Share Settlement System** to track and manage settlement amounts between administrators and clients based on trading outcomes.

### Key Principles

1. **Share Locking**: Share amount is locked at the start of each PnL cycle and never changes
2. **Cycle Separation**: Each PnL sign change starts a new cycle with a new locked share
3. **Linear Mapping**: MaskedCapital maps share payments back to PnL linearly
4. **Sign Consistency**: Transaction signs are determined BEFORE balance updates
5. **Remaining Tracking**: Remaining amount is stored positive internally, signed at display time

---

## Core Definitions

### Client_PnL (Client Profit/Loss)

**Definition:** The difference between exchange balance and funding

**Formula:**
```
Client_PnL = ExchangeBalance - Funding
```

**Data Type:** `BigInteger` (can be negative, zero, or positive)

**Interpretation:**
- `Client_PnL > 0`: Client is in profit → You owe client
- `Client_PnL < 0`: Client is in loss → Client owes you
- `Client_PnL = 0`: Trading flat → No settlement needed

**Storage:** NOT stored in database, computed on-demand

**Code:**
```python
def compute_client_pnl(self):
    return self.exchange_balance - self.funding
```

### FinalShare (Your Share Amount)

**Definition:** Your portion of the profit/loss, calculated using floor rounding

**Formula:**
```
IF Client_PnL == 0:
    FinalShare = 0
ELSE:
    SharePercentage = get_share_percentage(Client_PnL)
    ExactShare = |Client_PnL| × SharePercentage / 100
    FinalShare = floor(ExactShare)
```

**Data Type:** `BigInteger` ≥ 0

**Rounding:** Uses `floor()` (round down)

**Share Percentage Selection:**
```
IF Client_PnL < 0 (LOSS):
    SharePercentage = loss_share_percentage IF loss_share_percentage > 0
                     ELSE my_percentage
ELSE IF Client_PnL > 0 (PROFIT):
    SharePercentage = profit_share_percentage IF profit_share_percentage > 0
                     ELSE my_percentage
ELSE:
    SharePercentage = 0
```

**Storage:** Locked as `locked_initial_final_share` when cycle starts

**Code:**
```python
def compute_my_share(self):
    client_pnl = self.compute_client_pnl()
    if client_pnl == 0:
        return 0
    share_pct = self.get_share_percentage(client_pnl)
    exact_share = abs(client_pnl) * share_pct / 100
    return floor(exact_share)
```

### RemainingRaw (Core Remaining Amount)

**Definition:** How much settlement is still pending (raw value, always ≥ 0)

**Formula:**
```
RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)
```

**Where:**
- `LockedInitialFinalShare`: Share locked at cycle start (never changes)
- `TotalSettled`: Sum of all settlements in current cycle only

**Data Type:** `BigInteger` ≥ 0

**Cycle Filtering:**
```
IF cycle_start_date exists:
    TotalSettled = Sum(Settlement.amount WHERE date >= cycle_start_date)
ELSE:
    TotalSettled = Sum(all Settlement.amount)  # Backward compatibility
```

**Storage:** Computed on-demand, returned by `get_remaining_settlement_amount()`

**Code:**
```python
def get_remaining_settlement_amount(self):
    self.lock_initial_share_if_needed()
    
    if self.cycle_start_date:
        total_settled = self.settlements.filter(
            date__gte=self.cycle_start_date
        ).aggregate(total=Sum('amount'))['total'] or 0
    else:
        total_settled = self.settlements.aggregate(
            total=Sum('amount')
        )['total'] or 0
    
    initial_final_share = self.locked_initial_final_share or 0
    remaining = max(0, initial_final_share - total_settled)
    
    return {
        'remaining': remaining,
        'overpaid': max(0, total_settled - initial_final_share),
        'initial_final_share': initial_final_share,
        'total_settled': total_settled
    }
```

### DisplayRemaining (Signed Remaining for Display)

**Definition:** Remaining amount with correct sign for UI display

**Formula:**
```
IF Client_PnL < 0 (LOSS):
    DisplayRemaining = +RemainingRaw  (client owes you)
ELSE IF Client_PnL > 0 (PROFIT):
    DisplayRemaining = -RemainingRaw  (you owe client)
ELSE:
    DisplayRemaining = 0
```

**Data Type:** `BigInteger` (signed)

**Key Point:** `RemainingRaw` is always positive (internal), sign is applied ONLY at display time

**Code:**
```python
def calculate_display_remaining(client_pnl, remaining_amount):
    if client_pnl > 0:
        return -remaining_amount  # You owe client (negative)
    else:
        return remaining_amount  # Client owes you (positive)
```

### MaskedCapital

**Definition:** The amount deducted from balance when payment is recorded

**Formula:**
```
MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
```

**Where:**
- `SharePayment`: Amount being paid (always positive)
- `LockedInitialPnL`: PnL when share was locked (use absolute value)
- `LockedInitialFinalShare`: Share locked at cycle start

**Data Type:** `BigInteger` ≥ 0

**Purpose:** Maps share payment back to PnL linearly

**Balance Impact:**
```
IF Client_PnL < 0 (LOSS):
    Funding = Funding - MaskedCapital
ELSE IF Client_PnL > 0 (PROFIT):
    ExchangeBalance = ExchangeBalance - MaskedCapital
```

**Code:**
```python
def compute_masked_capital(self, share_payment):
    settlement_info = self.get_remaining_settlement_amount()
    initial_final_share = settlement_info['initial_final_share']
    locked_initial_pnl = self.locked_initial_pnl
    
    if initial_final_share == 0 or locked_initial_pnl is None:
        return 0
    
    return int((share_payment * abs(locked_initial_pnl)) / initial_final_share)
```

### Transaction Sign

**Definition:** Sign of transaction amount in RECORD_PAYMENT transactions

**Formula:**
```
# CRITICAL: Calculate Client_PnL BEFORE balance update
Client_PnL_before = compute_client_pnl()

IF Client_PnL_before > 0:
    Transaction.amount = -SharePayment  (you paid client)
ELSE IF Client_PnL_before < 0:
    Transaction.amount = +SharePayment  (client paid you)
ELSE:
    Transaction.amount = 0
```

**Golden Rule:** Transaction sign must be decided BEFORE balance mutation

**Code:**
```python
# CORRECT ORDER
client_pnl_before = account.compute_client_pnl()

if client_pnl_before > 0:
    transaction_amount = -paid_amount  # You paid client
elif client_pnl_before < 0:
    transaction_amount = paid_amount  # Client paid you
else:
    transaction_amount = 0

# THEN update balances
apply_masked_capital()
```

---

## Master Formulas

### Formula 1: Client PnL Calculation

```
Client_PnL = ExchangeBalance - Funding
```

**When:** Computed on-demand  
**Returns:** `BigInteger` (can be negative)  
**Used For:** Determining profit/loss direction, share calculation

### Formula 2: Share Percentage Selection

```
IF Client_PnL < 0:
    SharePercentage = loss_share_percentage IF loss_share_percentage > 0
                     ELSE my_percentage
ELSE IF Client_PnL > 0:
    SharePercentage = profit_share_percentage IF profit_share_percentage > 0
                     ELSE my_percentage
ELSE:
    SharePercentage = 0
```

**When:** Called before share calculation  
**Returns:** `Integer` (0-100)  
**Used For:** Determining which percentage to use for share calculation

### Formula 3: Final Share Calculation

```
IF Client_PnL == 0:
    FinalShare = 0
ELSE:
    SharePercentage = get_share_percentage(Client_PnL)
    ExactShare = |Client_PnL| × SharePercentage / 100
    FinalShare = floor(ExactShare)
```

**When:** First compute per PnL cycle  
**Returns:** `BigInteger` ≥ 0  
**Used For:** Locking initial share

### Formula 4: Remaining Amount (Raw)

```
RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)
```

**Where:**
- `LockedInitialFinalShare`: Share locked at cycle start
- `TotalSettled`: Sum of settlements in current cycle (filtered by `cycle_start_date`)

**When:** Computed on-demand  
**Returns:** `BigInteger` ≥ 0  
**Used For:** Internal calculations

### Formula 5: Display Remaining (Signed)

```
IF Client_PnL < 0:
    DisplayRemaining = +RemainingRaw  (client owes you)
ELSE IF Client_PnL > 0:
    DisplayRemaining = -RemainingRaw  (you owe client)
ELSE:
    DisplayRemaining = 0
```

**When:** Applied at display time  
**Returns:** `BigInteger` (signed)  
**Used For:** UI display, user-facing values

### Formula 6: Masked Capital

```
MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
```

**When:** Computed during payment recording  
**Returns:** `BigInteger` ≥ 0  
**Used For:** Mapping share payment to PnL reduction

### Formula 7: Settlement Impact on Balance

```
IF Client_PnL < 0 (LOSS):
    Funding = Funding - MaskedCapital
ELSE IF Client_PnL > 0 (PROFIT):
    ExchangeBalance = ExchangeBalance - MaskedCapital
```

**When:** Applied during payment recording  
**Impact:** Reduces the appropriate balance by masked capital

### Formula 8: Transaction Sign Logic

```
# CRITICAL: Calculate BEFORE balance update
Client_PnL_before = compute_client_pnl()

IF Client_PnL_before > 0:
    Transaction.amount = -SharePayment  (you paid client)
ELSE IF Client_PnL_before < 0:
    Transaction.amount = +SharePayment  (client paid you)
ELSE:
    Transaction.amount = 0
```

**Golden Rule:** Transaction sign must be decided BEFORE balance mutation

### Formula 9: Overpaid Amount

```
Overpaid = max(0, TotalSettled - LockedInitialFinalShare)
```

**When:** Computed when `TotalSettled > LockedInitialFinalShare`  
**Returns:** `BigInteger` ≥ 0  
**Used For:** Tracking overpayment

---

## Detailed Formula Explanations

### Formula 1: Client_PnL = ExchangeBalance - Funding

**Explanation:**
- This is the fundamental profit/loss calculation
- Positive result means client made profit (you owe them)
- Negative result means client made loss (they owe you)
- Zero means trading flat (no settlement needed)

**Example:**
- Exchange Balance: ₹290
- Funding: ₹100
- Client_PnL = 290 - 100 = **+₹190** (Profit)

**Example:**
- Exchange Balance: ₹10
- Funding: ₹100
- Client_PnL = 10 - 100 = **-₹90** (Loss)

### Formula 2: Share Percentage Selection

**Explanation:**
- System supports separate percentages for loss and profit cases
- Falls back to `my_percentage` if specific percentage not set
- Returns 0 for zero PnL (no share)

**Example:**
- `loss_share_percentage = 10`
- `profit_share_percentage = 20`
- `my_percentage = 15`
- Client_PnL = -90 (Loss) → Uses **10%**
- Client_PnL = +190 (Profit) → Uses **20%**
- Client_PnL = 0 → Returns **0%**

### Formula 3: FinalShare = floor(|Client_PnL| × SharePercentage / 100)

**Explanation:**
- Uses absolute value of PnL (always positive for calculation)
- Multiplies by share percentage
- Divides by 100 to get percentage
- Uses `floor()` to round down (always integer ≥ 0)

**Example:**
- Client_PnL = -90 (Loss)
- SharePercentage = 10%
- ExactShare = 90 × 10 / 100 = 9.0
- FinalShare = floor(9.0) = **9**

**Example:**
- Client_PnL = +190 (Profit)
- SharePercentage = 20%
- ExactShare = 190 × 20 / 100 = 38.0
- FinalShare = floor(38.0) = **38**

**Example (Rounding):**
- Client_PnL = -95 (Loss)
- SharePercentage = 10%
- ExactShare = 95 × 10 / 100 = 9.5
- FinalShare = floor(9.5) = **9** (not 10)

### Formula 4: RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)

**Explanation:**
- Uses locked share (never changes after locking)
- Subtracts total settled in current cycle only
- Uses `max(0, ...)` to prevent negative values

**Example:**
- LockedInitialFinalShare = 9
- TotalSettled = 5
- RemainingRaw = max(0, 9 - 5) = **4**

**Example (Overpaid):**
- LockedInitialFinalShare = 9
- TotalSettled = 12
- RemainingRaw = max(0, 9 - 12) = **0**
- Overpaid = max(0, 12 - 9) = **3**

### Formula 5: DisplayRemaining = sign(Client_PnL) × RemainingRaw

**Explanation:**
- RemainingRaw is always positive (internal calculation)
- Sign is applied based on PnL direction
- Loss case: Positive (client owes you)
- Profit case: Negative (you owe client)

**Example:**
- RemainingRaw = 4
- Client_PnL = -90 (Loss)
- DisplayRemaining = +4 (client owes you)

**Example:**
- RemainingRaw = 23
- Client_PnL = +190 (Profit)
- DisplayRemaining = -23 (you owe client)

### Formula 6: MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare

**Explanation:**
- Maps share payment linearly back to PnL
- Ensures consistent settlement ratio
- Prevents exponential growth

**Example:**
- SharePayment = 3
- LockedInitialPnL = -90
- LockedInitialFinalShare = 9
- MaskedCapital = (3 × 90) / 9 = 270 / 9 = **30**

**Example:**
- SharePayment = 15
- LockedInitialPnL = +190
- LockedInitialFinalShare = 38
- MaskedCapital = (15 × 190) / 38 = 2850 / 38 = **75**

### Formula 7: Balance Update

**Explanation:**
- Loss case: Reduces funding (money given to client)
- Profit case: Reduces exchange balance (client's profit)

**Example (Loss):**
- Funding: ₹100
- MaskedCapital: ₹30
- New Funding = 100 - 30 = **₹70**

**Example (Profit):**
- Exchange Balance: ₹290
- MaskedCapital: ₹75
- New Exchange Balance = 290 - 75 = **₹215**

### Formula 8: Transaction Sign

**Explanation:**
- Sign represents money direction from YOUR point of view
- Positive: Client paid you (your profit)
- Negative: You paid client (your loss)
- Must be calculated BEFORE balance update

**Example:**
- Client_PnL_before = -90 (Loss)
- SharePayment = 3
- Transaction.amount = **+3** (client paid you)

**Example:**
- Client_PnL_before = +190 (Profit)
- SharePayment = 15
- Transaction.amount = **-15** (you paid client)

---

## Complete Logic Flows

### Flow 1: Initial Share Locking

```
1. User creates/updates ClientExchangeAccount
   ↓
2. System computes Client_PnL = ExchangeBalance - Funding
   ↓
3. IF Client_PnL == 0:
      → Skip locking (no share)
   ELSE:
      → Continue to step 4
   ↓
4. System calls lock_initial_share_if_needed()
   ↓
5. Check if locked_initial_final_share exists
   ↓
6. IF NOT EXISTS:
      → Compute FinalShare = floor(|Client_PnL| × SharePercentage / 100)
      → Lock FinalShare as locked_initial_final_share
      → Lock SharePercentage as locked_share_percentage
      → Lock Client_PnL as locked_initial_pnl
      → Set cycle_start_date = now()
      → Set locked_initial_funding = current_funding
      → Save account
   ELSE:
      → Check for cycle changes:
         - PnL sign flip?
         - PnL magnitude reduction?
         - Funding change?
      → IF cycle changed:
           → Reset all locks
           → Start new cycle (go to step 6)
      → ELSE:
           → Keep existing locks
```

### Flow 2: Remaining Amount Calculation

```
1. System calls get_remaining_settlement_amount()
   ↓
2. Lock share if needed (Flow 1)
   ↓
3. Filter settlements by cycle:
   IF cycle_start_date exists:
      → TotalSettled = Sum(Settlement.amount WHERE date >= cycle_start_date)
   ELSE:
      → TotalSettled = Sum(all Settlement.amount)
   ↓
4. Get LockedInitialFinalShare
   ↓
5. Calculate RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)
   ↓
6. Calculate Overpaid = max(0, TotalSettled - LockedInitialFinalShare)
   ↓
7. Return {
      'remaining': RemainingRaw,
      'overpaid': Overpaid,
      'initial_final_share': LockedInitialFinalShare,
      'total_settled': TotalSettled
   }
```

### Flow 3: Payment Recording

```
1. User clicks "Record Payment"
   ↓
2. System loads account (no locking for GET)
   ↓
3. System calculates:
   - Client_PnL = compute_client_pnl()
   - Settlement info = get_remaining_settlement_amount()
   - DisplayRemaining = calculate_display_remaining(Client_PnL, RemainingRaw)
   ↓
4. User enters payment amount and submits
   ↓
5. System validates:
   - Paid amount > 0
   - Client_PnL != 0
   - InitialFinalShare > 0
   - Paid amount ≤ RemainingRaw
   ↓
6. System locks account row (select_for_update)
   ↓
7. System calculates Client_PnL_before (BEFORE any updates)
   ↓
8. System decides transaction sign:
   IF Client_PnL_before > 0:
      → transaction_amount = -paid_amount
   ELSE:
      → transaction_amount = +paid_amount
   ↓
9. System locks share if needed
   ↓
10. System gets settlement info (with locked account)
    ↓
11. System validates payment amount ≤ remaining
    ↓
12. System calculates MaskedCapital:
    MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
    ↓
13. System validates balance won't go negative:
    IF Client_PnL < 0:
        → Check: Funding - MaskedCapital >= 0
    ELSE:
        → Check: ExchangeBalance - MaskedCapital >= 0
    ↓
14. System updates balance:
    IF Client_PnL < 0:
        → Funding = Funding - MaskedCapital
    ELSE:
        → ExchangeBalance = ExchangeBalance - MaskedCapital
    ↓
15. System saves account
    ↓
16. System creates Settlement record:
    Settlement.amount = SharePayment (always positive)
    ↓
17. System creates Transaction record:
    Transaction.amount = transaction_amount (signed)
    Transaction.type = 'RECORD_PAYMENT'
    ↓
18. System commits transaction
    ↓
19. System redirects user
```

### Flow 4: Display Remaining Calculation

```
1. System gets RemainingRaw from get_remaining_settlement_amount()
   ↓
2. System gets Client_PnL = compute_client_pnl()
   ↓
3. System applies sign:
   IF Client_PnL > 0:
      → DisplayRemaining = -RemainingRaw
   ELSE:
      → DisplayRemaining = +RemainingRaw
   ↓
4. System displays DisplayRemaining in UI
```

---

## Step-by-Step Calculations

### Example 1: Loss Case - Complete Settlement

**Initial State:**
- Funding: ₹100
- Exchange Balance: ₹10
- Loss Share Percentage: 10%

**Step 1: Calculate Client_PnL**
```
Client_PnL = ExchangeBalance - Funding
Client_PnL = 10 - 100
Client_PnL = -₹90 (LOSS)
```

**Step 2: Select Share Percentage**
```
Client_PnL < 0 → Use loss_share_percentage
loss_share_percentage = 10%
SharePercentage = 10%
```

**Step 3: Calculate Final Share**
```
ExactShare = |Client_PnL| × SharePercentage / 100
ExactShare = 90 × 10 / 100
ExactShare = 9.0
FinalShare = floor(9.0)
FinalShare = ₹9
```

**Step 4: Lock Share**
```
locked_initial_final_share = 9
locked_share_percentage = 10
locked_initial_pnl = -90
cycle_start_date = 2025-01-10 10:00:00
locked_initial_funding = 100
```

**Step 5: Calculate Remaining**
```
TotalSettled = 0 (no payments yet)
RemainingRaw = max(0, 9 - 0)
RemainingRaw = ₹9
DisplayRemaining = +₹9 (client owes you)
```

**Step 6: Record Payment of ₹9**
```
SharePayment = ₹9
LockedInitialPnL = -90
LockedInitialFinalShare = 9

MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
MaskedCapital = (9 × 90) / 9
MaskedCapital = ₹90

Funding = Funding - MaskedCapital
Funding = 100 - 90
New Funding = ₹10

ExchangeBalance = ₹10 (unchanged)

New Client_PnL = ExchangeBalance - Funding
New Client_PnL = 10 - 10
New Client_PnL = ₹0 (Settled!)

Transaction.amount = +₹9 (client paid you)

TotalSettled = 0 + 9 = ₹9
RemainingRaw = max(0, 9 - 9)
RemainingRaw = ₹0
DisplayRemaining = ₹0 (Settled)
```

### Example 2: Profit Case - Multiple Payments

**Initial State:**
- Funding: ₹100
- Exchange Balance: ₹290
- Profit Share Percentage: 20%

**Step 1: Calculate Client_PnL**
```
Client_PnL = ExchangeBalance - Funding
Client_PnL = 290 - 100
Client_PnL = +₹190 (PROFIT)
```

**Step 2: Select Share Percentage**
```
Client_PnL > 0 → Use profit_share_percentage
profit_share_percentage = 20%
SharePercentage = 20%
```

**Step 3: Calculate Final Share**
```
ExactShare = |Client_PnL| × SharePercentage / 100
ExactShare = 190 × 20 / 100
ExactShare = 38.0
FinalShare = floor(38.0)
FinalShare = ₹38
```

**Step 4: Lock Share**
```
locked_initial_final_share = 38
locked_share_percentage = 20
locked_initial_pnl = +190
cycle_start_date = 2025-01-10 10:00:00
locked_initial_funding = 100
```

**Step 5: Calculate Remaining**
```
TotalSettled = 0
RemainingRaw = max(0, 38 - 0)
RemainingRaw = ₹38
DisplayRemaining = -₹38 (you owe client)
```

**Step 6: Record Payment 1 of ₹15**
```
SharePayment = ₹15
LockedInitialPnL = +190
LockedInitialFinalShare = 38

MaskedCapital = (15 × 190) / 38
MaskedCapital = ₹75

ExchangeBalance = ExchangeBalance - MaskedCapital
ExchangeBalance = 290 - 75
New ExchangeBalance = ₹215

Funding = ₹100 (unchanged)

New Client_PnL = 215 - 100
New Client_PnL = +₹115

Transaction.amount = -₹15 (you paid client)

TotalSettled = 0 + 15 = ₹15
RemainingRaw = max(0, 38 - 15)
RemainingRaw = ₹23
DisplayRemaining = -₹23 (you owe client)
```

**Step 7: Record Payment 2 of ₹23**
```
SharePayment = ₹23
LockedInitialPnL = +190
LockedInitialFinalShare = 38

MaskedCapital = (23 × 190) / 38
MaskedCapital = ₹115

ExchangeBalance = 215 - 115
New ExchangeBalance = ₹100

Funding = ₹100 (unchanged)

New Client_PnL = 100 - 100
New Client_PnL = ₹0 (Settled!)

Transaction.amount = -₹23 (you paid client)

TotalSettled = 15 + 23 = ₹38
RemainingRaw = max(0, 38 - 38)
RemainingRaw = ₹0
DisplayRemaining = ₹0 (Settled)
```

### Example 3: Partial Payment with Remaining

**Initial State:**
- Funding: ₹100
- Exchange Balance: ₹10
- Loss Share Percentage: 10%
- Locked Share: ₹9

**Step 1: Calculate Remaining**
```
TotalSettled = 0
RemainingRaw = max(0, 9 - 0)
RemainingRaw = ₹9
```

**Step 2: Record Payment of ₹5**
```
SharePayment = ₹5
LockedInitialPnL = -90
LockedInitialFinalShare = 9

MaskedCapital = (5 × 90) / 9
MaskedCapital = ₹50

Funding = 100 - 50
New Funding = ₹50

New Client_PnL = 10 - 50
New Client_PnL = -₹40

Transaction.amount = +₹5 (client paid you)

TotalSettled = 0 + 5 = ₹5
RemainingRaw = max(0, 9 - 5)
RemainingRaw = ₹4
DisplayRemaining = +₹4 (client owes you)
```

**Step 3: Record Payment of ₹4**
```
SharePayment = ₹4
LockedInitialPnL = -90
LockedInitialFinalShare = 9

MaskedCapital = (4 × 90) / 9
MaskedCapital = ₹40

Funding = 50 - 40
New Funding = ₹10

New Client_PnL = 10 - 10
New Client_PnL = ₹0 (Settled!)

Transaction.amount = +₹4 (client paid you)

TotalSettled = 5 + 4 = ₹9
RemainingRaw = max(0, 9 - 9)
RemainingRaw = ₹0
DisplayRemaining = ₹0 (Settled)
```

---

## Code Implementation

### Helper Function: calculate_display_remaining

```python
def calculate_display_remaining(client_pnl, remaining_amount):
    """
    Calculate display remaining amount with correct sign based on Client_PnL direction.
    
    Formula: DisplayRemaining = sign(Client_PnL) × RemainingRaw
    - IF Client_PnL < 0 (LOSS): DisplayRemaining = +RemainingRaw (client owes you)
    - IF Client_PnL > 0 (PROFIT): DisplayRemaining = -RemainingRaw (you owe client)
    
    Args:
        client_pnl: Client PnL value (can be negative, zero, or positive)
        remaining_amount: Raw remaining amount (always ≥ 0)
    
    Returns:
        BigInteger: Signed remaining amount for display
    """
    if client_pnl > 0:
        return -remaining_amount  # You owe client (negative)
    else:
        return remaining_amount  # Client owes you (positive)
```

### Model Method: compute_client_pnl

```python
def compute_client_pnl(self):
    """
    MASTER PROFIT/LOSS FORMULA
    Client_PnL = exchange_balance - funding
    
    Returns: BIGINT (can be negative for loss)
    """
    return self.exchange_balance - self.funding
```

### Model Method: get_share_percentage

```python
def get_share_percentage(self, client_pnl=None):
    """
    Get appropriate share percentage based on PnL direction.
    
    Rules:
    - If client_pnl < 0 (LOSS): Use loss_share_percentage if set and > 0, else my_percentage
    - If client_pnl > 0 (PROFIT): Use profit_share_percentage if set and > 0, else my_percentage
    - If client_pnl == 0: Return 0 (no share on zero PnL)
    """
    if client_pnl is None:
        client_pnl = self.compute_client_pnl()
    
    if client_pnl < 0:
        share_pct = self.loss_share_percentage if self.loss_share_percentage and self.loss_share_percentage > 0 else self.my_percentage
    elif client_pnl > 0:
        share_pct = self.profit_share_percentage if self.profit_share_percentage and self.profit_share_percentage > 0 else self.my_percentage
    else:
        share_pct = 0
    
    return share_pct
```

### Model Method: compute_my_share

```python
def compute_my_share(self):
    """
    Calculate final share amount using floor rounding.
    
    Formula:
    IF Client_PnL == 0:
        return 0
    ELSE:
        SharePercentage = get_share_percentage(Client_PnL)
        return floor(|Client_PnL| × SharePercentage / 100)
    """
    client_pnl = self.compute_client_pnl()
    
    if client_pnl == 0:
        return 0
    
    share_pct = self.get_share_percentage(client_pnl)
    exact_share = abs(client_pnl) * share_pct / 100
    return int(exact_share)  # floor rounding
```

### Model Method: get_remaining_settlement_amount

```python
def get_remaining_settlement_amount(self):
    """
    Calculate remaining settlement amount using LOCKED InitialFinalShare.
    
    Formula: RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)
    
    IMPORTANT: Returns RemainingRaw (always ≥ 0).
    Sign must be applied at display time based on Client_PnL direction.
    """
    self.lock_initial_share_if_needed()
    
    # Filter settlements by cycle
    if self.cycle_start_date:
        total_settled = self.settlements.filter(
            date__gte=self.cycle_start_date
        ).aggregate(total=Sum('amount'))['total'] or 0
    else:
        total_settled = self.settlements.aggregate(
            total=Sum('amount')
        )['total'] or 0
    
    # Get locked share
    if self.locked_initial_final_share is not None:
        initial_final_share = self.locked_initial_final_share
    else:
        current_share = self.compute_my_share()
        if current_share > 0:
            # Lock it now
            self.lock_initial_share_if_needed()
            initial_final_share = self.locked_initial_final_share or current_share
        else:
            return {
                'remaining': 0,
                'overpaid': 0,
                'initial_final_share': 0,
                'total_settled': total_settled
            }
    
    # Calculate remaining
    remaining = max(0, initial_final_share - total_settled)
    overpaid = max(0, total_settled - initial_final_share)
    
    return {
        'remaining': remaining,
        'overpaid': overpaid,
        'initial_final_share': initial_final_share,
        'total_settled': total_settled
    }
```

### Model Method: compute_masked_capital

```python
def compute_masked_capital(self, share_payment):
    """
    Calculate masked capital from share payment.
    
    Formula: MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
    
    Args:
        share_payment: Share payment amount (integer)
    
    Returns:
        int: Masked capital amount
    """
    settlement_info = self.get_remaining_settlement_amount()
    initial_final_share = settlement_info['initial_final_share']
    locked_initial_pnl = self.locked_initial_pnl
    
    if initial_final_share == 0 or locked_initial_pnl is None:
        return 0
    
    return int((share_payment * abs(locked_initial_pnl)) / initial_final_share)
```

### View Function: record_payment (Key Logic)

```python
def record_payment(request, account_id):
    # ... setup code ...
    
    # CRITICAL: Lock account row
    with transaction.atomic():
        account = ClientExchangeAccount.objects.select_for_update().get(
            pk=account_id, client__user=request.user
        )
        
        # CRITICAL: Calculate Client_PnL BEFORE balance update
        client_pnl_before = account.compute_client_pnl()
        
        # Decide transaction sign BEFORE balance update
        if client_pnl_before > 0:
            transaction_amount = -paid_amount  # You paid client
        elif client_pnl_before < 0:
            transaction_amount = paid_amount  # Client paid you
        else:
            transaction_amount = 0
        
        # Lock share if needed
        account.lock_initial_share_if_needed()
        
        # Get settlement info
        settlement_info = account.get_remaining_settlement_amount()
        remaining_amount = settlement_info['remaining']
        
        # Validate payment amount
        if paid_amount > remaining_amount:
            raise ValidationError("Paid amount exceeds remaining")
        
        # Calculate masked capital
        masked_capital = account.compute_masked_capital(paid_amount)
        
        # Update balance
        if client_pnl_before < 0:
            account.funding -= masked_capital
        else:
            account.exchange_balance -= masked_capital
        
        account.save()
        
        # Create records
        Settlement.objects.create(
            client_exchange=account,
            amount=paid_amount
        )
        
        Transaction.objects.create(
            client_exchange=account,
            type='RECORD_PAYMENT',
            amount=transaction_amount  # Signed value
        )
```

---

## Examples with Real Numbers

### Example 1: Complete Loss Settlement

**Scenario:**
- Funding: ₹1,00,000
- Exchange Balance: ₹10,000
- Loss Share Percentage: 15%

**Calculations:**

**Step 1: Client_PnL**
```
Client_PnL = 10,000 - 1,00,000 = -₹90,000 (LOSS)
```

**Step 2: Final Share**
```
SharePercentage = 15%
ExactShare = 90,000 × 15 / 100 = 13,500
FinalShare = floor(13,500) = ₹13,500
```

**Step 3: Lock Share**
```
locked_initial_final_share = ₹13,500
locked_initial_pnl = -₹90,000
```

**Step 4: Remaining**
```
RemainingRaw = ₹13,500
DisplayRemaining = +₹13,500 (client owes you)
```

**Step 5: Record Payment of ₹13,500**
```
MaskedCapital = (13,500 × 90,000) / 13,500 = ₹90,000
Funding = 1,00,000 - 90,000 = ₹10,000
New Client_PnL = 10,000 - 10,000 = ₹0 (Settled!)
Transaction.amount = +₹13,500
RemainingRaw = ₹0
```

### Example 2: Partial Profit Settlement

**Scenario:**
- Funding: ₹50,000
- Exchange Balance: ₹1,50,000
- Profit Share Percentage: 25%

**Calculations:**

**Step 1: Client_PnL**
```
Client_PnL = 1,50,000 - 50,000 = +₹1,00,000 (PROFIT)
```

**Step 2: Final Share**
```
SharePercentage = 25%
ExactShare = 1,00,000 × 25 / 100 = 25,000
FinalShare = floor(25,000) = ₹25,000
```

**Step 3: Lock Share**
```
locked_initial_final_share = ₹25,000
locked_initial_pnl = +₹1,00,000
```

**Step 4: Remaining**
```
RemainingRaw = ₹25,000
DisplayRemaining = -₹25,000 (you owe client)
```

**Step 5: Record Payment 1 of ₹10,000**
```
MaskedCapital = (10,000 × 1,00,000) / 25,000 = ₹40,000
ExchangeBalance = 1,50,000 - 40,000 = ₹1,10,000
New Client_PnL = 1,10,000 - 50,000 = +₹60,000
Transaction.amount = -₹10,000
RemainingRaw = ₹15,000
DisplayRemaining = -₹15,000
```

**Step 6: Record Payment 2 of ₹15,000**
```
MaskedCapital = (15,000 × 1,00,000) / 25,000 = ₹60,000
ExchangeBalance = 1,10,000 - 60,000 = ₹50,000
New Client_PnL = 50,000 - 50,000 = ₹0 (Settled!)
Transaction.amount = -₹15,000
RemainingRaw = ₹0
```

### Example 3: Multiple Cycles

**Cycle 1 (Loss):**
- Funding: ₹100, Exchange Balance: ₹10
- Client_PnL = -₹90, Share = ₹9
- Payment: ₹9 → Settled

**Trading Changes:**
- Exchange Balance: ₹10 → ₹200

**Cycle 2 (Profit):**
- Funding: ₹100, Exchange Balance: ₹200
- Client_PnL = +₹100, Share = ₹20 (new cycle)
- Old cycle settlements NOT counted
- RemainingRaw = ₹20 (from new cycle)

---

## Edge Cases and Special Scenarios

### Edge Case 1: Zero PnL

**Scenario:** `Client_PnL = 0`

**Handling:**
```
FinalShare = 0
RemainingRaw = 0
DisplayRemaining = 0
Show: "N.A"
Block settlement
```

**Code:**
```python
if client_pnl == 0:
    return 0  # No share
```

### Edge Case 2: Zero Share

**Scenario:** `FinalShare = 0` (share percentage too small or PnL too small)

**Handling:**
```
Show: "N.A"
Block settlement
RemainingRaw = 0
```

**Example:**
- Client_PnL = ₹5
- SharePercentage = 10%
- ExactShare = 5 × 10 / 100 = 0.5
- FinalShare = floor(0.5) = 0

### Edge Case 3: Overpayment

**Scenario:** `TotalSettled > LockedInitialFinalShare`

**Handling:**
```
RemainingRaw = 0
Overpaid = TotalSettled - LockedInitialFinalShare
System allows but tracks overpayment
```

**Example:**
- LockedInitialFinalShare = ₹9
- TotalSettled = ₹12
- RemainingRaw = max(0, 9 - 12) = ₹0
- Overpaid = max(0, 12 - 9) = ₹3

### Edge Case 4: PnL Sign Flip

**Scenario:** PnL changes from loss to profit (or vice versa)

**Handling:**
```
Old cycle locks cleared
New cycle starts
New locked share calculated
Old settlements NOT counted in new cycle
```

**Example:**
- Cycle 1: Loss ₹90, Share ₹9, Paid ₹5, Remaining ₹4
- Trading: Exchange Balance increases
- Cycle 2: Profit ₹100, Share ₹20, Paid ₹0, Remaining ₹20
- Both cycles tracked separately

### Edge Case 5: PnL Magnitude Reduction

**Scenario:** `|Current_PnL| < |LockedInitialPnL|`

**Handling:**
```
Cycle resets
New locked share calculated
Old settlements NOT counted
```

**Example:**
- LockedInitialPnL = -₹90
- Current PnL = -₹50
- Magnitude reduced → Cycle resets
- New share calculated from -₹50

### Edge Case 6: Funding Change

**Scenario:** `CurrentFunding != LockedInitialFunding`

**Handling:**
```
Cycle resets
New locked share calculated
Old settlements NOT counted
```

**Example:**
- LockedInitialFunding = ₹100
- Current Funding = ₹150
- Funding changed → Cycle resets
- New share calculated with new funding

---

## Validation Rules

### Validation 1: Zero PnL Check

```
IF Client_PnL == 0:
    Block settlement
    Show message: "Account PnL is zero (trading flat). No settlement needed."
```

### Validation 2: Zero Share Check

```
IF InitialFinalShare == 0:
    Block settlement
    Show message: "No settlement allowed. Initial final share is zero."
```

### Validation 3: Payment Amount Validation

```
IF PaidAmount <= 0:
    Raise ValidationError: "Paid amount must be greater than zero."

IF PaidAmount > RemainingRaw:
    Raise ValidationError: "Paid amount cannot exceed remaining settlement amount."
```

### Validation 4: Balance Validation

```
IF Client_PnL < 0:
    IF Funding - MaskedCapital < 0:
        Raise ValidationError: "Funding would become negative."

IF Client_PnL > 0:
    IF ExchangeBalance - MaskedCapital < 0:
        Raise ValidationError: "Exchange balance would become negative."
```

### Validation 5: Concurrent Payment Prevention

```
Use database row locking:
account = ClientExchangeAccount.objects.select_for_update().get(...)
```

---

## Formula Summary Table

| # | Formula | When Used | Returns |
|---|---------|-----------|---------|
| 1 | `Client_PnL = ExchangeBalance - Funding` | On-demand | `BigInteger` (signed) |
| 2 | `SharePercentage = get_share_percentage(Client_PnL)` | Before share calc | `Integer` (0-100) |
| 3 | `FinalShare = floor(\|Client_PnL\| × SharePercentage / 100)` | First compute per cycle | `BigInteger` ≥ 0 |
| 4 | `RemainingRaw = max(0, LockedShare - TotalSettled)` | On-demand | `BigInteger` ≥ 0 |
| 5 | `DisplayRemaining = sign(Client_PnL) × RemainingRaw` | Display time | `BigInteger` (signed) |
| 6 | `MaskedCapital = (SharePayment × \|LockedPnL\|) / LockedShare` | Payment recording | `BigInteger` ≥ 0 |
| 7 | `Funding = Funding - MaskedCapital` (if loss) | Payment recording | Updated balance |
| 7 | `ExchangeBalance = ExchangeBalance - MaskedCapital` (if profit) | Payment recording | Updated balance |
| 8 | `Transaction.amount = -SharePayment` (if profit) | Payment recording | `BigInteger` (signed) |
| 8 | `Transaction.amount = +SharePayment` (if loss) | Payment recording | `BigInteger` (signed) |
| 9 | `Overpaid = max(0, TotalSettled - LockedShare)` | On-demand | `BigInteger` ≥ 0 |

---

## Key Rules and Constraints

### Rule 1: Share Locking
- Share is locked at first compute per PnL cycle
- Share NEVER changes after locking
- Share is decided by trading, NOT by payments

### Rule 2: Cycle Separation
- Each PnL sign change starts a new cycle
- Settlements from different cycles must NOT mix
- Use `cycle_start_date` to filter settlements

### Rule 3: Transaction Sign
- Sign must be decided BEFORE balance update
- Calculate `Client_PnL_before` first
- Then decide sign based on `Client_PnL_before`

### Rule 4: Remaining Display
- RemainingRaw is always positive (internal)
- Sign is applied ONLY at display time
- Based on current `Client_PnL` direction

### Rule 5: Masked Capital
- Maps share payment to PnL linearly
- Ensures consistent settlement ratio
- Prevents exponential growth

---

## Complete Calculation Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    INITIAL SETUP                            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Set Funding & Exchange Balance   │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Compute Client_PnL               │
        │  = ExchangeBalance - Funding      │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  IF Client_PnL == 0:              │
        │     → Show N.A, Block settlement  │
        │  ELSE:                            │
        │     → Continue                    │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Get Share Percentage             │
        │  (Based on PnL direction)         │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Calculate Final Share            │
        │  = floor(|PnL| × Share% / 100)    │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Lock Share                       │
        │  - locked_initial_final_share      │
        │  - locked_share_percentage         │
        │  - locked_initial_pnl              │
        │  - cycle_start_date                │
        │  - locked_initial_funding          │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Calculate Remaining               │
        │  = max(0, LockedShare - Settled)   │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Apply Display Sign                │
        │  IF PnL > 0: -Remaining           │
        │  ELSE: +Remaining                  │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Display in UI                    │
        └───────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    PAYMENT RECORDING                         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Lock Account Row                 │
        │  (select_for_update)              │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Calculate Client_PnL_before       │
        │  (BEFORE any updates)              │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Decide Transaction Sign           │
        │  IF PnL > 0: -SharePayment         │
        │  ELSE: +SharePayment               │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Validate Payment                  │
        │  - Amount > 0                      │
        │  - Amount ≤ Remaining              │
        │  - PnL != 0                        │
        │  - Share > 0                       │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Calculate MaskedCapital           │
        │  = (Payment × |PnL|) / Share      │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Validate Balance                  │
        │  - Won't go negative               │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Update Balance                    │
        │  IF Loss: Funding -= Capital       │
        │  ELSE: ExchangeBalance -= Capital  │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Create Settlement Record          │
        │  (amount = SharePayment)           │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Create Transaction Record         │
        │  (amount = signed SharePayment)    │
        └───────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────┐
        │  Save Account                      │
        └───────────────────────────────────┘
```

---

## Summary

This document covers all formulas and logic for the Pending Payments System:

✅ **9 Master Formulas** - Complete with explanations  
✅ **Step-by-Step Calculations** - Real number examples  
✅ **Complete Logic Flows** - Visual flow diagrams  
✅ **Code Implementation** - Actual Python code  
✅ **Edge Cases** - All special scenarios handled  
✅ **Validation Rules** - All checks documented  

**Key Takeaways:**
- Share is locked and never changes
- Transaction sign is decided BEFORE balance update
- Remaining is stored positive, signed at display time
- Cycles are separated to prevent mixing
- All formulas are mathematically consistent

---

**End of Documentation**

