# PENDING PAYMENTS SYSTEM - COMPLETE GUIDE

**Version:** 4.0 (Complete & Comprehensive)  
**Status:** Production Ready  
**Last Updated:** January 2026

---

## TABLE OF CONTENTS

1. [System Overview](#system-overview)
2. [Pending Payments Fundamentals](#pending-payments-fundamentals)
3. [Core Formulas](#core-formulas)
4. [Partial Payments System](#partial-payments-system)
5. [Cycle Management](#cycle-management)
6. [Remaining Amount Calculation](#remaining-amount-calculation)
7. [Settlement Recording](#settlement-recording)
8. [UI Display Logic](#ui-display-logic)
9. [Edge Cases & Scenarios](#edge-cases--scenarios)
10. [Code Reference](#code-reference)
11. [Testing Guide](#testing-guide)

---

## SYSTEM OVERVIEW

The **Pending Payments System** is a masked share settlement system that:

- **Tracks pending settlement amounts** for each client-exchange account
- **Supports partial payments** (multiple payments until fully settled)
- **Separates PnL cycles** (prevents mixing old and new settlements)
- **Locks shares** at first compute to prevent shrinking
- **Hides actual PnL values** from users (shows only shares)
- **Uses floor rounding** (no decimals)

### Key Concepts

| Concept | Meaning |
|---------|---------|
| **Pending Payment** | Amount still owed (remaining settlement) |
| **Partial Payment** | Payment less than full remaining amount |
| **Final Share** | Admin's share (floor rounded, integer) |
| **Remaining** | Final Share - Total Settled (current cycle) |
| **Cycle** | Period between cycle resets (sign flip, funding change, PnL reduction) |

---

## PENDING PAYMENTS FUNDAMENTALS

### What is a Pending Payment?

A **pending payment** is the amount that still needs to be settled between admin and client.

**Two Types:**

1. **Clients Owe You** (Loss Case)
   - Client has a loss
   - Client must pay admin's share
   - Direction: Client → Admin

2. **You Owe Clients** (Profit Case)
   - Client has a profit
   - Admin must pay client's share
   - Direction: Admin → Client

### When Does a Payment Become Pending?

A payment becomes pending when:
- Client PnL ≠ 0 (has profit or loss)
- Final Share > 0 (share percentage results in non-zero share)
- Remaining > 0 (not fully settled)

### When is a Payment No Longer Pending?

A payment is no longer pending when:
- Remaining = 0 (fully settled)
- OR Final Share = 0 (no share to settle)

---

## CORE FORMULAS

### Formula 1: Client PnL

```
Client_PnL = ExchangeBalance − Funding
```

**Returns:** BIGINT (can be negative for loss, positive for profit)

**Example:**
- Funding = 200
- Exchange Balance = 100
- Client PnL = 100 - 200 = **-100** (Loss)

**Example:**
- Funding = 200
- Exchange Balance = 250
- Client PnL = 250 - 200 = **+50** (Profit)

---

### Formula 2: Share Percentage Selection

```
IF Client_PnL < 0:
    Share% = loss_share_percentage (or fallback to my_percentage)
ELSE:
    Share% = profit_share_percentage (or fallback to my_percentage)
```

**Rules:**
- Loss uses `loss_share_percentage`
- Profit uses `profit_share_percentage`
- If percentage is 0, fallback to `my_percentage`

**Example:**
- Client PnL = -100 (Loss)
- loss_share_percentage = 10%
- Share% = **10%**

**Example:**
- Client PnL = +50 (Profit)
- profit_share_percentage = 20%
- Share% = **20%**

---

### Formula 3: Exact Share (Before Rounding)

```
ExactShare = |Client_PnL| × (Share% / 100)
```

**Returns:** Float (full precision, internal only)

**Rules:**
- Uses absolute value of PnL
- Full precision maintained
- Never rounded at this step

**Example:**
- Client PnL = -100
- Share% = 10%
- ExactShare = 100 × (10 / 100) = **10.0**

**Example:**
- Client PnL = +50
- Share% = 20%
- ExactShare = 50 × (20 / 100) = **10.0**

**Example:**
- Client PnL = +1
- Share% = 10%
- ExactShare = 1 × (10 / 100) = **0.1**

---

### Formula 4: Final Share (After Floor Rounding)

```
FinalShare = floor(ExactShare)
```

**Returns:** BIGINT (always positive, integer)

**Rules:**
- **ONLY rounding step** in entire system
- **FLOOR method** (round down)
- Fractional values discarded permanently
- No decimals shown or settled

**Example:**
- ExactShare = 10.0
- FinalShare = floor(10.0) = **10**

**Example:**
- ExactShare = 10.9
- FinalShare = floor(10.9) = **10**

**Example:**
- ExactShare = 0.1
- FinalShare = floor(0.1) = **0**

**Example:**
- ExactShare = 0.9
- FinalShare = floor(0.9) = **0**

---

### Formula 5: Remaining Settlement Amount

```
Remaining = LockedInitialFinalShare − TotalSettled (Current Cycle)
```

**Returns:** BIGINT (always ≥ 0)

**Critical Rules:**
1. **Uses locked share**, NOT current share
2. **Only counts settlements from current cycle** (filtered by `cycle_start_date`)
3. **Share NEVER shrinks** after payments

**Components:**
- `LockedInitialFinalShare`: Share locked when cycle started
- `TotalSettled`: Sum of all settlements in current cycle

**Example:**
- LockedInitialFinalShare = 10
- TotalSettled = 0
- Remaining = 10 - 0 = **10**

**Example:**
- LockedInitialFinalShare = 10
- TotalSettled = 5
- Remaining = 10 - 5 = **5**

**Example:**
- LockedInitialFinalShare = 10
- TotalSettled = 10
- Remaining = 10 - 10 = **0** (Fully settled)

**Example:**
- LockedInitialFinalShare = 10
- TotalSettled = 15
- Remaining = max(0, 10 - 15) = **0**
- Overpaid = 15 - 10 = **5**

---

### Formula 6: Overpaid Amount

```
Overpaid = max(0, TotalSettled − LockedInitialFinalShare)
```

**Returns:** BIGINT (always ≥ 0)

**Meaning:** Amount paid beyond the locked share

**Example:**
- LockedInitialFinalShare = 10
- TotalSettled = 15
- Overpaid = max(0, 15 - 10) = **5**

---

## PARTIAL PAYMENTS SYSTEM

### What is a Partial Payment?

A **partial payment** is a settlement amount that is less than the full remaining amount.

**Key Features:**
- ✅ Multiple partial payments allowed
- ✅ Integer values only
- ✅ Can record until remaining = 0
- ✅ Each payment tracked individually

### Partial Payment Flow

```
Step 1: Initial State
  LockedInitialFinalShare = 10
  TotalSettled = 0
  Remaining = 10

Step 2: Record Partial Payment of 3
  Payment Amount = 3
  TotalSettled = 0 + 3 = 3
  Remaining = 10 - 3 = 7

Step 3: Record Another Partial Payment of 4
  Payment Amount = 4
  TotalSettled = 3 + 4 = 7
  Remaining = 10 - 7 = 3

Step 4: Record Final Payment of 3
  Payment Amount = 3
  TotalSettled = 7 + 3 = 10
  Remaining = 10 - 10 = 0 (Fully settled)
```

### Partial Payment Validations

**Validation 1: Final Share Must Be > 0**
```python
if initial_final_share == 0:
    raise ValidationError("No settlement allowed: Final share is zero")
```

**Validation 2: Remaining Must Be > 0**
```python
if remaining_amount == 0:
    # Already fully settled, no payment needed
```

**Validation 3: Payment Amount Must Be ≤ Remaining**
```python
if paid_amount > remaining_amount:
    raise ValidationError(f"Over-settlement: {paid_amount} > {remaining_amount}")
```

**Validation 4: Payment Amount Must Be > 0**
```python
if paid_amount <= 0:
    raise ValidationError("Paid amount must be greater than zero")
```

### Partial Payment Example

**Scenario:**
- Funding = 100
- Exchange Balance = 10
- Client PnL = -90
- Loss Share % = 10%
- Final Share = 9
- Remaining = 9

**Payment Plan:**
1. **Payment 1:** 3 → Remaining = 6
2. **Payment 2:** 3 → Remaining = 3
3. **Payment 3:** 3 → Remaining = 0 ✅ Settled

**Settlement Records:**
```
Settlement 1: amount=3, date=2026-01-09 08:00
Settlement 2: amount=3, date=2026-01-09 09:00
Settlement 3: amount=3, date=2026-01-09 10:00
Total Settled: 9
```

---

## CYCLE MANAGEMENT

### What is a Cycle?

A **cycle** is a period between cycle resets. Each cycle has:
- Its own locked share
- Its own cycle start date
- Its own settlement tracking

### Cycle Reset Conditions

A cycle resets (new cycle starts) when:

#### 1. PnL Sign Flips

**Condition:**
```
(Current PnL < 0) != (Locked PnL < 0)
```

**Examples:**
- Locked PnL = -100 (Loss) → Current PnL = +50 (Profit) ✅ Reset
- Locked PnL = +100 (Profit) → Current PnL = -50 (Loss) ✅ Reset
- Locked PnL = -100 (Loss) → Current PnL = -50 (Loss) ❌ No reset

**Why:** Different direction = different settlement direction = new cycle

---

#### 2. PnL Magnitude Reduces

**Condition:**
```
abs(Current PnL) < abs(Locked PnL)
```

**Examples:**
- Locked PnL = +100 → Current PnL = +1 ✅ Reset
- Locked PnL = -90 → Current PnL = -10 ✅ Reset
- Locked PnL = +100 → Current PnL = +150 ❌ No reset

**Why:** Trading reduced exposure = new trading outcome = new cycle

**Example Scenario:**
```
Step 1: Profit = +100, Share = 10 (LOCKED)
Step 2: Profit = +1, Share = 0
  → Old lock invalid (share should be 0, not 10)
  → Cycle resets
  → New share = 0
```

---

#### 3. Funding Changes

**Condition:**
```
Current Funding != Locked Initial Funding
```

**Examples:**
- Locked Funding = 100 → Current Funding = 300 ✅ Reset
- Locked Funding = 200 → Current Funding = 200 ❌ No reset

**Why:** New funding = new exposure = new trading cycle

**Example Scenario:**
```
Step 1: Funding = 100, PnL = -90, Share = 9 (LOCKED)
Step 2: Funding = 300, PnL = -200
  → New exposure
  → Cycle resets
  → New share = 20
```

---

### Cycle Tracking Fields

| Field | Purpose | When Set | When Reset |
|-------|---------|----------|------------|
| `locked_initial_final_share` | Share locked at cycle start | First compute or cycle reset | Cycle reset or fully settled |
| `locked_share_percentage` | Percentage used for locked share | Same as above | Same as above |
| `locked_initial_pnl` | PnL at cycle start (for sign detection) | Same as above | Same as above |
| `cycle_start_date` | Timestamp when cycle started | Same as above | Same as above |
| `locked_initial_funding` | Funding at cycle start (for change detection) | Same as above | Same as above |

---

### Settlement Filtering by Cycle

**Critical Rule:** Only count settlements from current cycle

**Implementation:**
```python
if self.cycle_start_date:
    # Only count settlements after cycle started
    total_settled = self.settlements.filter(
        date__gte=self.cycle_start_date
    ).aggregate(total=models.Sum('amount'))['total'] or 0
else:
    # Backward compatibility: count all settlements
    total_settled = self.settlements.aggregate(total=models.Sum('amount'))['total'] or 0
```

**Why:** Prevents old cycle settlements from mixing with new cycle shares

**Example:**
```
Old Cycle:
  - Share = 9
  - Settled = 5
  - Remaining = 4

New Cycle Starts:
  - Share = 10
  - Old settlement (5) NOT counted
  - Remaining = 10 (not 5)
```

---

## REMAINING AMOUNT CALCULATION

### Complete Formula Breakdown

```
Step 1: Lock share if needed
  lock_initial_share_if_needed()

Step 2: Get locked share
  IF locked_initial_final_share exists:
    initial_final_share = locked_initial_final_share
  ELSE:
    IF current_share > 0:
      Lock it now
      initial_final_share = current_share
    ELSE:
      Return remaining = 0

Step 3: Count settlements from current cycle
  IF cycle_start_date exists:
    total_settled = settlements.filter(date >= cycle_start_date).sum()
  ELSE:
    total_settled = settlements.sum()

Step 4: Calculate remaining
  remaining = max(0, initial_final_share - total_settled)
  overpaid = max(0, total_settled - initial_final_share)
```

### Step-by-Step Example

**Scenario:**
- Funding = 200
- Exchange Balance = 100
- Loss Share % = 10%
- Cycle started: 2026-01-09 08:00
- Settlements:
  - 2026-01-09 09:00: 3
  - 2026-01-09 10:00: 4

**Calculation:**
```
Step 1: Calculate PnL
  Client_PnL = 100 - 200 = -100

Step 2: Calculate Share
  ExactShare = 100 × 10% = 10.0
  FinalShare = floor(10.0) = 10

Step 3: Lock Share (if not already locked)
  locked_initial_final_share = 10
  cycle_start_date = 2026-01-09 08:00

Step 4: Count Settlements (from current cycle)
  Settlement 1: 2026-01-09 09:00 (after cycle start) ✅ Count
  Settlement 2: 2026-01-09 10:00 (after cycle start) ✅ Count
  total_settled = 3 + 4 = 7

Step 5: Calculate Remaining
  remaining = max(0, 10 - 7) = 3
  overpaid = max(0, 7 - 10) = 0
```

---

## SETTLEMENT RECORDING

### Settlement Flow

```
1. User enters payment amount
2. System validates:
   - Final share > 0?
   - Remaining > 0?
   - Payment amount ≤ remaining?
   - Payment amount > 0?
3. Lock account row (prevent concurrent payments)
4. Recalculate remaining (with locked row)
5. Calculate MaskedCapital
6. Update funding/exchange_balance
7. Create Settlement record
8. Create Transaction record (audit)
9. Recalculate remaining
10. Show success message
```

### MaskedCapital Formula

**Purpose:** Maps settlement payment back to actual capital change

**Formula:**
```
MaskedCapital = (SharePayment × |LockedInitialPnL|) ÷ LockedInitialFinalShare
```

**Why This Formula:**
- Proportional mapping: 50% of share → 50% of PnL
- Linear relationship, not exponential
- Prevents double-counting percentage

**Example:**
- SharePayment = 5
- LockedInitialPnL = -90
- LockedInitialFinalShare = 9
- MaskedCapital = (5 × 90) ÷ 9 = **50**

**Example:**
- SharePayment = 3
- LockedInitialPnL = -90
- LockedInitialFinalShare = 9
- MaskedCapital = (3 × 90) ÷ 9 = **30**

---

### Settlement Updates

**Loss Case (Client Pays Admin):**
```
Funding = Funding − MaskedCapital
```

**Example:**
- Current Funding = 100
- MaskedCapital = 50
- New Funding = 100 - 50 = **50**

**Profit Case (Admin Pays Client):**
```
ExchangeBalance = ExchangeBalance − MaskedCapital
```

**Example:**
- Current Exchange Balance = 200
- MaskedCapital = 50
- New Exchange Balance = 200 - 50 = **150**

---

### Settlement Record Creation

**Settlement Model:**
```python
Settlement.objects.create(
    client_exchange=account,
    amount=paid_amount,  # Integer, > 0
    notes=notes  # Optional
)
```

**Fields:**
- `client_exchange`: Link to account
- `amount`: Payment amount (BIGINT)
- `date`: Auto-set to now
- `notes`: Optional notes

---

## UI DISPLAY LOGIC

### Pending Payments List

**Two Sections:**

1. **Clients Owe You** (Loss Cases)
   - Shows clients with PnL < 0
   - Displays: Client, Exchange, Funding, Exchange Balance, Final Share, Remaining, Share %, Actions

2. **You Owe Clients** (Profit Cases)
   - Shows clients with PnL > 0
   - Same columns as above

### Display Rules

**Final Share Display:**
```
IF show_na == True:
    Display: "N.A"
ELSE:
    Display: final_share
```

**Remaining Display:**
```
IF show_na == True:
    Display: "N.A"
ELSE IF remaining > 0:
    Display: remaining (highlighted)
ELSE:
    Display: "Settled"
```

**Record Payment Button:**
```
IF show_na == True:
    Hide button
ELSE IF remaining > 0:
    Show "Record Payment" button
ELSE:
    Show "Settled" label
```

### show_na Flag

**When show_na = True:**
- Final Share = 0 (no share to settle)
- Remaining = 0
- No settlement allowed
- Shows "N.A" in UI

**When show_na = False:**
- Final Share > 0
- Can have remaining > 0 or = 0
- Settlement allowed (if remaining > 0)

---

## EDGE CASES & SCENARIOS

### Edge Case 1: Zero Share Account

**Scenario:**
- Funding = 100
- Exchange Balance = 100
- Client PnL = 0
- Final Share = 0

**Behavior:**
- show_na = True
- Remaining = 0
- "Record Payment" button hidden
- Shows "N.A" in pending list

**This is by design, not a bug.**

---

### Edge Case 2: Very Small Share

**Scenario:**
- Funding = 100
- Exchange Balance = 95
- Client PnL = -5
- Loss Share % = 1%
- ExactShare = 0.05
- FinalShare = floor(0.05) = 0

**Behavior:**
- Same as zero share account
- Shows "N.A"

---

### Edge Case 3: Partial Payment Sequence

**Scenario:**
- LockedInitialFinalShare = 10
- Remaining = 10

**Payment Sequence:**
1. Pay 3 → Remaining = 7
2. Pay 4 → Remaining = 3
3. Pay 2 → Remaining = 1
4. Pay 1 → Remaining = 0 ✅ Settled

**Each payment:**
- Creates Settlement record
- Updates funding/exchange_balance
- Recalculates remaining
- Shows updated remaining in UI

---

### Edge Case 4: Over-Settlement Attempt

**Scenario:**
- Remaining = 5
- User tries to pay 10

**Validation:**
```python
if paid_amount > remaining_amount:
    raise ValidationError("Over-settlement not allowed")
```

**Result:**
- Payment blocked
- Error message shown
- Remaining unchanged

---

### Edge Case 5: Cycle Reset During Partial Payments

**Scenario:**
```
Old Cycle:
  - Share = 9
  - Paid 5
  - Remaining = 4

PnL Sign Flips (New Cycle):
  - New Share = 10
  - Old settlement (5) NOT counted
  - Remaining = 10 (not 4)
```

**Behavior:**
- Old cycle closes
- New cycle starts
- Old settlement preserved but not counted
- Remaining recalculates from new share

---

### Edge Case 6: Funding Change During Settlement

**Scenario:**
```
Step 1: Funding = 100, PnL = -90, Share = 9
Step 2: Pay 5, Remaining = 4
Step 3: Funding = 300, PnL = -200
```

**Behavior:**
- Funding change detected
- Cycle resets
- New Share = 20
- Old settlement (5) NOT counted
- Remaining = 20

---

### Edge Case 7: PnL Reduction During Settlement

**Scenario:**
```
Step 1: Profit = +100, Share = 10
Step 2: Pay 5, Remaining = 5
Step 3: Profit = +1, Share = 0
```

**Behavior:**
- PnL magnitude reduction detected
- Cycle resets
- New Share = 0
- Old settlement (5) NOT counted
- Remaining = 0
- Shows "Settled"

---

### Edge Case 8: Concurrent Payments

**Scenario:**
- Two users try to record payment simultaneously
- Remaining = 10

**Protection:**
```python
account = ClientExchangeAccount.objects.select_for_update().get(pk=account_id)
```

**Behavior:**
- Database row locking prevents race condition
- First payment processes
- Second payment sees updated remaining
- No double-counting

---

### Edge Case 9: Negative Balance Prevention

**Scenario:**
- Funding = 50
- Remaining = 9
- Payment would reduce funding to -10

**Validation:**
```python
if account.funding - masked_capital < 0:
    raise ValidationError("Funding would become negative")
```

**Result:**
- Payment blocked
- Error shown
- Balance unchanged

---

### Edge Case 10: Multiple Partial Payments Across Days

**Scenario:**
- Day 1: Pay 3
- Day 2: Pay 4
- Day 3: Pay 2
- Day 4: Pay 1 → Settled

**Settlement Records:**
```
Settlement 1: date=2026-01-09, amount=3
Settlement 2: date=2026-01-10, amount=4
Settlement 3: date=2026-01-11, amount=2
Settlement 4: date=2026-01-12, amount=1
```

**All counted in same cycle** (if cycle_start_date < all dates)

---

## CODE REFERENCE

### Model: ClientExchangeAccount

**Location:** `core/models.py`

**Key Methods:**

#### `compute_client_pnl()`
```python
def compute_client_pnl(self):
    """
    Client_PnL = exchange_balance - funding
    Returns: BIGINT (can be negative)
    """
    return self.exchange_balance - self.funding
```

#### `compute_my_share()`
```python
def compute_my_share(self):
    """
    Calculates Final Share using floor rounding
    Returns: BIGINT (always positive)
    """
    client_pnl = self.compute_client_pnl()
    if client_pnl == 0:
        return 0
    
    # Select share percentage
    if client_pnl < 0:
        share_pct = self.loss_share_percentage if self.loss_share_percentage > 0 else self.my_percentage
    else:
        share_pct = self.profit_share_percentage if self.profit_share_percentage > 0 else self.my_percentage
    
    # Calculate
    exact_share = abs(client_pnl) * (share_pct / 100.0)
    final_share = math.floor(exact_share)
    
    return int(final_share)
```

#### `lock_initial_share_if_needed()`
```python
def lock_initial_share_if_needed(self):
    """
    Locks share at first compute or cycle reset
    Handles:
    - PnL magnitude reduction
    - Funding changes
    - Sign flips
    """
    # Check for cycle resets
    # Lock share if needed
    # Set cycle_start_date and locked_initial_funding
```

#### `get_remaining_settlement_amount()`
```python
def get_remaining_settlement_amount(self):
    """
    Calculates remaining settlement amount
    Returns: dict with 'remaining', 'overpaid', 'initial_final_share', 'total_settled'
    """
    # Lock share if needed
    # Filter settlements by cycle_start_date
    # Calculate remaining = locked_share - total_settled
    # Return result
```

---

### View: pending_summary

**Location:** `core/views.py` (line ~926)

**Purpose:** Displays pending payments list

**Logic:**
```python
def pending_summary(request):
    # Get all client exchanges
    # For each exchange:
    #   - Calculate PnL
    #   - Lock share if needed
    #   - Get remaining amount
    #   - Add to appropriate list (clients_owe / you_owe)
    # Sort lists
    # Calculate totals
    # Render template
```

**Key Variables:**
- `clients_owe_list`: Clients in loss (owe you)
- `you_owe_list`: Clients in profit (you owe)
- `remaining_amount`: Amount still to settle

---

### View: record_payment

**Location:** `core/views.py` (line ~3257)

**Purpose:** Records a settlement payment

**Flow:**
```python
def record_payment(request, account_id):
    # Lock account row (select_for_update)
    # Lock share if needed
    # Get remaining amount
    # Validate payment amount
    # Calculate MaskedCapital
    # Update funding/exchange_balance
    # Create Settlement record
    # Create Transaction record
    # Redirect with success message
```

**Validations:**
- Final share > 0
- Remaining > 0
- Payment amount ≤ remaining
- Payment amount > 0
- Funding/exchange_balance won't go negative

---

### Template: pending/summary.html

**Location:** `core/templates/core/pending/summary.html`

**Displays:**
- Two sections: "Clients Owe You" and "You Owe Clients"
- Columns: Client, Exchange, Funding, Exchange Balance, Final Share, Remaining, Share %, Actions
- "Record Payment" button (if remaining > 0)
- "Settled" label (if remaining = 0)
- "N.A" label (if share = 0)

---

## TESTING GUIDE

### Test Scenario 1: Basic Loss Settlement

**Setup:**
- Funding: 100
- Exchange Balance: 10
- Loss Share %: 10%

**Expected:**
- PnL: -90
- Final Share: 9
- Remaining: 9

**Test Steps:**
1. Verify pending list shows remaining = 9
2. Record payment of 5
3. Verify remaining = 4
4. Record payment of 4
5. Verify remaining = 0
6. Verify "Settled" shown

---

### Test Scenario 2: Basic Profit Settlement

**Setup:**
- Funding: 50
- Exchange Balance: 100
- Profit Share %: 20%

**Expected:**
- PnL: +50
- Final Share: 10
- Remaining: 10

**Test Steps:**
1. Verify pending list shows remaining = 10
2. Record payment of 10
3. Verify remaining = 0
4. Verify exchange_balance reduced by MaskedCapital

---

### Test Scenario 3: Partial Payments

**Setup:**
- Funding: 100
- Exchange Balance: 10
- Loss Share %: 10%
- Remaining: 9

**Test Steps:**
1. Record payment of 3 → Verify remaining = 6
2. Record payment of 3 → Verify remaining = 3
3. Record payment of 3 → Verify remaining = 0
4. Verify all 3 settlements recorded
5. Verify total settled = 9

---

### Test Scenario 4: Cycle Reset (Sign Flip)

**Setup:**
- Step 1: Funding=100, Exchange=10, PnL=-90, Share=9
- Step 2: Pay 5, Remaining=4
- Step 3: Exchange=100, PnL=+50, NEW CYCLE

**Expected:**
- Step 3: Remaining = 10 (old settlement NOT counted)

**Test Steps:**
1. Record payment of 5 in loss cycle
2. Change exchange balance to 100
3. Verify new cycle starts
4. Verify remaining = 10 (not 5)
5. Verify old settlement (5) NOT counted

---

### Test Scenario 5: Cycle Reset (Funding Change)

**Setup:**
- Step 1: Funding=100, Exchange=10, PnL=-90, Share=9
- Step 2: Funding=300, Exchange=100, PnL=-200

**Expected:**
- Step 2: Remaining = 20 (new share)

**Test Steps:**
1. Verify initial share = 9
2. Change funding to 300
3. Verify cycle resets
4. Verify new share = 20
5. Verify remaining = 20

---

### Test Scenario 6: Cycle Reset (PnL Reduction)

**Setup:**
- Step 1: Funding=200, Exchange=300, PnL=+100, Share=10
- Step 2: Exchange=201, PnL=+1, Share=0

**Expected:**
- Step 2: Remaining = 0 (no share)

**Test Steps:**
1. Verify initial share = 10
2. Change exchange balance to 201
3. Verify cycle resets
4. Verify new share = 0
5. Verify remaining = 0
6. Verify "Settled" shown

---

### Test Scenario 7: Over-Settlement Prevention

**Setup:**
- Remaining: 5

**Test Steps:**
1. Try to record payment of 10
2. Verify error: "Over-settlement not allowed"
3. Verify remaining still = 5

---

### Test Scenario 8: Zero Share Account

**Setup:**
- Funding: 100
- Exchange Balance: 100
- PnL: 0

**Test Steps:**
1. Verify "Record Payment" button NOT shown
2. Verify "N.A" shown in pending list
3. Verify remaining = 0

---

### Test Scenario 9: Concurrent Payments

**Setup:**
- Remaining: 10

**Test Steps:**
1. User 1: Record payment of 5
2. User 2: Try to record payment of 6 (should see remaining = 5)
3. Verify database locking prevents race condition

---

### Test Scenario 10: Negative Balance Prevention

**Setup:**
- Funding: 50
- Remaining: 9

**Test Steps:**
1. Try to record payment that would make funding negative
2. Verify error: "Funding would become negative"
3. Verify funding unchanged

---

## SUMMARY

### Key Formulas

1. **PnL:** `Client_PnL = ExchangeBalance − Funding`
2. **Exact Share:** `ExactShare = |PnL| × (Share% / 100)`
3. **Final Share:** `FinalShare = floor(ExactShare)`
4. **Remaining:** `Remaining = LockedInitialFinalShare − TotalSettled (Current Cycle)`
5. **MaskedCapital:** `MaskedCapital = (SharePayment × |LockedInitialPnL|) ÷ LockedInitialFinalShare`

### Key Rules

1. **Shares are locked** at first compute per PnL cycle
2. **Cycles reset** when:
   - PnL sign changes (LOSS ↔ PROFIT)
   - PnL magnitude reduces (trading reduced exposure)
   - Funding changes (new exposure = new cycle)
3. **Old cycle settlements** never mix with new cycle shares
4. **Remaining uses locked share**, not current share
5. **Partial payments allowed** until remaining = 0
6. **MaskedCapital maps proportionally** to PnL

### System Guarantees

- ✅ Deterministic math
- ✅ No rounding drift
- ✅ Ledger stability
- ✅ Safe manual control
- ✅ Support for changing profit %
- ✅ No historical corruption
- ✅ Cycle separation
- ✅ Concurrency safety
- ✅ Partial payment support
- ✅ Accurate remaining calculation

---

**END OF DOCUMENTATION**

