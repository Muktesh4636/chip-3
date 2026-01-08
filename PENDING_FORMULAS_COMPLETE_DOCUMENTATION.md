# Pending Formulas - Complete Pin-to-Pin Documentation

**Version:** 2.0  
**System:** Profit-Loss-Share-Settlement System  
**Last Updated:** 2024-01-07  
**Documentation Type:** Pin-to-Pin Formula Reference

---

## Table of Contents

1. [Overview](#overview)
2. [Core Data Model](#core-data-model)
3. [Master Formulas](#master-formulas)
4. [Pending Calculation Formulas](#pending-calculation-formulas)
5. [Record Payment Formulas](#record-payment-formulas)
6. [Pending Summary View Formulas](#pending-summary-view-formulas)
7. [Share Calculation Formulas](#share-calculation-formulas)
8. [Settlement Detection Formulas](#settlement-detection-formulas)
9. [Code Mapping](#code-mapping)
10. [Examples with Step-by-Step Calculations](#examples-with-step-by-step-calculations)
11. [Edge Cases and Validation](#edge-cases-and-validation)

---

## Overview

This document provides **pin-to-pin** documentation of all pending payment formulas in the system. Every formula is traced from its mathematical definition through to its code implementation.

### Key Principles

1. **Pending = Computed, Never Stored**
   - Pending amounts are calculated on-the-fly from stored values
   - No database fields store pending amounts
   - All pending calculations derive from `funding` and `exchange_balance`

2. **Two Types of Pending**
   - **Clients Owe You**: When `Client_PnL < 0` (loss case)
   - **You Owe Clients**: When `Client_PnL > 0` (profit case)

3. **Settlement = Zero PnL**
   - Account is settled when `Client_PnL = 0`
   - No pending amount exists when settled

---

## Core Data Model

### ClientExchangeAccount Model

**Location:** `core/models.py` lines 50-103

**Stored Fields:**
- `funding` (BIGINT): Total real money given to client
- `exchange_balance` (BIGINT): Current balance on exchange
- `my_percentage` (INT): Partner's share percentage (0-100)

**Key Point:** These are the ONLY stored values. Everything else is computed.

### Formula Methods

**Location:** `core/models.py` lines 81-102

```python
def compute_client_pnl(self):
    """
    MASTER PROFIT/LOSS FORMULA
    Client_PnL = exchange_balance - funding
    
    Returns: BIGINT (can be negative for loss)
    """
    return self.exchange_balance - self.funding

def compute_my_share(self):
    """
    PARTNER SHARE FORMULA
    My_Share = ABS(Client_PnL) × my_percentage / 100
    
    Returns: BIGINT (always positive, integer division)
    """
    client_pnl = abs(self.compute_client_pnl())
    return (client_pnl * self.my_percentage) // 100

def is_settled(self):
    """Check if client is fully settled (PnL = 0)"""
    return self.compute_client_pnl() == 0
```

---

## Master Formulas

### Formula 1: Client Profit/Loss

**Mathematical Definition:**
```
Client_PnL = exchange_balance - funding
```

**Code Location:** `core/models.py:88`

**Python Implementation:**
```python
def compute_client_pnl(self):
    return self.exchange_balance - self.funding
```

**Return Type:** BIGINT (can be negative, zero, or positive)

**Interpretation:**
- `Client_PnL < 0`: Client is in loss (owes money)
- `Client_PnL = 0`: Client is settled (no pending)
- `Client_PnL > 0`: Client is in profit (you owe money)

---

### Formula 2: Partner Share

**Mathematical Definition:**
```
My_Share = ABS(Client_PnL) × my_percentage / 100
```

**Code Location:** `core/models.py:97-98`

**Python Implementation:**
```python
def compute_my_share(self):
    client_pnl = abs(self.compute_client_pnl())
    return (client_pnl * self.my_percentage) // 100
```

**Return Type:** BIGINT (always positive, integer division)

**Key Points:**
- Uses `ABS()` to ensure positive value
- Uses integer division (`//`) for BIGINT compatibility
- Always positive regardless of profit/loss direction

---

### Formula 3: Settlement Check

**Mathematical Definition:**
```
Is_Settled = (Client_PnL == 0)
```

**Code Location:** `core/models.py:100-102`

**Python Implementation:**
```python
def is_settled(self):
    return self.compute_client_pnl() == 0
```

**Return Type:** Boolean

**Interpretation:**
- `True`: Account is fully settled, no pending amount
- `False`: Account has pending amount (either profit or loss)

---

## Pending Calculation Formulas

### Formula 4: Pending Amount (Loss Case - Clients Owe You)

**Mathematical Definition:**
```
IF Client_PnL < 0:
    Pending_Amount = ABS(Client_PnL)
    My_Share_Pending = ABS(Client_PnL) × my_percentage / 100
```

**Code Location:** Calculated in `pending_summary` view (placeholder implementation)

**Python Implementation:**
```python
client_pnl = account.compute_client_pnl()
if client_pnl < 0:
    pending_amount = abs(client_pnl)
    my_share_pending = account.compute_my_share()
```

**When Used:**
- Displayed in "Clients Owe You" section
- Shows amount client owes you
- Shows your share of that amount

**Example:**
```
Funding = 100,000
Exchange Balance = 30,000
Client_PnL = 30,000 - 100,000 = -70,000
Pending_Amount = ABS(-70,000) = 70,000
My_Share (10%) = 70,000 × 10 / 100 = 7,000
```

---

### Formula 5: Pending Amount (Profit Case - You Owe Clients)

**Mathematical Definition:**
```
IF Client_PnL > 0:
    Pending_Amount = Client_PnL
    My_Share_Pending = Client_PnL × my_percentage / 100
```

**Code Location:** Calculated in `pending_summary` view (placeholder implementation)

**Python Implementation:**
```python
client_pnl = account.compute_client_pnl()
if client_pnl > 0:
    pending_amount = client_pnl
    my_share_pending = account.compute_my_share()
```

**When Used:**
- Displayed in "You Owe Clients" section
- Shows amount you owe client
- Shows your share of that amount

**Example:**
```
Funding = 100,000
Exchange Balance = 170,000
Client_PnL = 170,000 - 100,000 = +70,000
Pending_Amount = 70,000
My_Share (10%) = 70,000 × 10 / 100 = 7,000
```

---

### Formula 6: Total Pending (Clients Owe You)

**Mathematical Definition:**
```
Total_Clients_Owe = Σ(ABS(Client_PnL)) for all accounts where Client_PnL < 0
Total_My_Share_Clients_Owe = Σ(My_Share) for all accounts where Client_PnL < 0
```

**Code Location:** `core/views.py:1062` (pending_summary function)

**Python Implementation:**
```python
clients_owe_list = []
for account in client_exchanges:
    client_pnl = account.compute_client_pnl()
    if client_pnl < 0:
        clients_owe_list.append({
            "pending_amount": abs(client_pnl),
            "my_share": account.compute_my_share(),
            # ... other fields
        })

total_pending = sum(item["pending_amount"] for item in clients_owe_list)
total_my_share = sum(item["my_share"] for item in clients_owe_list)
```

**Template Usage:** `core/templates/core/pending/summary.html:23-24`

---

### Formula 7: Total Pending (You Owe Clients)

**Mathematical Definition:**
```
Total_You_Owe = Σ(Client_PnL) for all accounts where Client_PnL > 0
Total_My_Share_You_Owe = Σ(My_Share) for all accounts where Client_PnL > 0
```

**Code Location:** `core/views.py:1063` (pending_summary function)

**Python Implementation:**
```python
you_owe_list = []
for account in client_exchanges:
    client_pnl = account.compute_client_pnl()
    if client_pnl > 0:
        you_owe_list.append({
            "client_profit": client_pnl,
            "my_share": account.compute_my_share(),
            # ... other fields
        })

total_profit = sum(item["client_profit"] for item in you_owe_list)
total_my_share = sum(item["my_share"] for item in you_owe_list)
```

**Template Usage:** `core/templates/core/pending/summary.html:28-29`

---

## Record Payment Formulas

### Formula 8: Record Payment (Loss Case)

**Mathematical Definition:**
```
IF Client_PnL < 0 AND paid_amount > 0 AND paid_amount <= ABS(Client_PnL):
    New_Funding = Old_Funding - paid_amount
    New_Exchange_Balance = Old_Exchange_Balance  (UNCHANGED)
    New_Client_PnL = New_Exchange_Balance - New_Funding
```

**Code Location:** `core/views.py:3207` (record_payment function - TODO implementation)

**Python Implementation:**
```python
account = get_object_or_404(ClientExchangeAccount, pk=account_id)
client_pnl = account.compute_client_pnl()

# Validation
if client_pnl == 0:
    return error("Account already settled")
if paid_amount <= 0:
    return error("Amount must be greater than 0")
if paid_amount > abs(client_pnl):
    return error("Amount exceeds pending")

# Apply payment
if client_pnl < 0:  # Loss case
    account.funding = account.funding - paid_amount
    # exchange_balance remains unchanged
    account.save()
```

**Key Points:**
- Only `funding` is reduced
- `exchange_balance` remains unchanged
- New `Client_PnL` is automatically recalculated

**Example:**
```
Before:
  Funding = 100,000
  Exchange Balance = 30,000
  Client_PnL = -70,000
  Pending = 70,000

Payment: 20,000

After:
  Funding = 100,000 - 20,000 = 80,000
  Exchange Balance = 30,000 (unchanged)
  Client_PnL = 30,000 - 80,000 = -50,000
  Pending = 50,000
```

---

### Formula 9: Record Payment (Profit Case)

**Mathematical Definition:**
```
IF Client_PnL > 0 AND paid_amount > 0 AND paid_amount <= Client_PnL:
    New_Funding = Old_Funding  (UNCHANGED)
    New_Exchange_Balance = Old_Exchange_Balance - paid_amount
    New_Client_PnL = New_Exchange_Balance - New_Funding
```

**Code Location:** `core/views.py:3207` (record_payment function - TODO implementation)

**Python Implementation:**
```python
account = get_object_or_404(ClientExchangeAccount, pk=account_id)
client_pnl = account.compute_client_pnl()

# Validation (same as loss case)
if client_pnl == 0:
    return error("Account already settled")
if paid_amount <= 0:
    return error("Amount must be greater than 0")
if paid_amount > client_pnl:
    return error("Amount exceeds pending")

# Apply payment
if client_pnl > 0:  # Profit case
    account.exchange_balance = account.exchange_balance - paid_amount
    # funding remains unchanged
    account.save()
```

**Key Points:**
- Only `exchange_balance` is reduced
- `funding` remains unchanged
- New `Client_PnL` is automatically recalculated

**Example:**
```
Before:
  Funding = 100,000
  Exchange Balance = 170,000
  Client_PnL = +70,000
  Pending = 70,000

Payment: 30,000

After:
  Funding = 100,000 (unchanged)
  Exchange Balance = 170,000 - 30,000 = 140,000
  Client_PnL = 140,000 - 100,000 = +40,000
  Pending = 40,000
```

---

### Formula 10: Full Settlement Check (After Payment)

**Mathematical Definition:**
```
After Payment:
    New_Client_PnL = New_Exchange_Balance - New_Funding
    
    IF New_Client_PnL == 0:
        Account is SETTLED
        Hide "Record Payment" button
    ELSE:
        Account still PENDING
        Show "Record Payment" button
```

**Code Location:** Template logic in `core/templates/core/pending/summary.html:76`

**Python Implementation:**
```python
# After payment is recorded
new_client_pnl = account.compute_client_pnl()

if new_client_pnl == 0:
    # Account is settled
    # Button visibility handled in template
    pass
else:
    # Account still pending
    # Button remains visible
    pass
```

**Template Logic:**
```django
{% if not account.is_settled %}
    <a href="{% url 'record_payment' account.pk %}">Record Payment</a>
{% endif %}
```

---

## Pending Summary View Formulas

### Formula 11: Filtering Accounts for Pending Summary

**Mathematical Definition:**
```
For each ClientExchangeAccount:
    Client_PnL = exchange_balance - funding
    
    IF Client_PnL < 0:
        Add to "Clients Owe You" list
    ELIF Client_PnL > 0:
        Add to "You Owe Clients" list
    ELSE:
        Skip (account is settled)
```

**Code Location:** `core/views.py:862-1079` (pending_summary function)

**Python Implementation:**
```python
clients_owe_list = []
you_owe_list = []

for client_exchange in client_exchanges:
    client_pnl = client_exchange.compute_client_pnl()
    
    if client_pnl < 0:
        # Loss case - client owes you
        clients_owe_list.append({
            "client": client_exchange.client,
            "exchange": client_exchange.exchange,
            "account": client_exchange,
            "client_pnl": client_pnl,
            "amount_owed": abs(client_pnl),
            "my_share_amount": client_exchange.compute_my_share(),
            # ... other fields
        })
    elif client_pnl > 0:
        # Profit case - you owe client
        you_owe_list.append({
            "client": client_exchange.client,
            "exchange": client_exchange.exchange,
            "account": client_exchange,
            "client_pnl": client_pnl,
            "amount_owed": client_pnl,
            "my_share_amount": client_exchange.compute_my_share(),
            # ... other fields
        })
    # else: skip (settled)
```

---

### Formula 12: Sorting Pending Lists

**Mathematical Definition:**
```
Sort "Clients Owe You" by: ABS(Client_PnL) DESCENDING
Sort "You Owe Clients" by: Client_PnL DESCENDING
```

**Code Location:** `core/views.py:1058-1059`

**Python Implementation:**
```python
# Sort by pending amount (descending)
clients_owe_list.sort(key=lambda x: abs(x["client_pnl"]), reverse=True)
you_owe_list.sort(key=lambda x: x["client_pnl"], reverse=True)
```

---

## Share Calculation Formulas

### Formula 13: My Share Calculation (Detailed)

**Mathematical Definition:**
```
Step 1: Calculate Client_PnL
    Client_PnL = exchange_balance - funding

Step 2: Get absolute value
    ABS_Client_PnL = ABS(Client_PnL)

Step 3: Calculate share
    My_Share = (ABS_Client_PnL × my_percentage) // 100
```

**Code Location:** `core/models.py:90-98`

**Python Implementation:**
```python
def compute_my_share(self):
    client_pnl = abs(self.compute_client_pnl())
    return (client_pnl * self.my_percentage) // 100
```

**Important Notes:**
- Uses integer division (`//`) for BIGINT compatibility
- Always returns positive value (uses `ABS()`)
- Percentage is stored as integer (0-100)

---

### Formula 14: Share Percentage Validation

**Mathematical Definition:**
```
my_percentage MUST satisfy:
    0 <= my_percentage <= 100
```

**Code Location:** `core/models.py:68-72`

**Python Implementation:**
```python
my_percentage = models.IntegerField(
    default=0,
    validators=[MinValueValidator(0), MaxValueValidator(100)],
    help_text="Your total percentage share (0-100)"
)
```

---

## Settlement Detection Formulas

### Formula 15: Settlement Status

**Mathematical Definition:**
```
Is_Settled = (Client_PnL == 0)

Where:
    Client_PnL = exchange_balance - funding
```

**Code Location:** `core/models.py:100-102`

**Python Implementation:**
```python
def is_settled(self):
    return self.compute_client_pnl() == 0
```

**Usage:**
- Controls "Record Payment" button visibility
- Determines if account appears in pending summary
- Used in UI to show/hide payment options

---

### Formula 16: Settlement After Full Payment

**Mathematical Definition:**
```
For Loss Case:
    IF paid_amount == ABS(Client_PnL):
        New_Funding = Old_Funding - ABS(Client_PnL)
        New_Exchange_Balance = Old_Exchange_Balance
        New_Client_PnL = New_Exchange_Balance - New_Funding = 0
        → SETTLED

For Profit Case:
    IF paid_amount == Client_PnL:
        New_Funding = Old_Funding
        New_Exchange_Balance = Old_Exchange_Balance - Client_PnL
        New_Client_PnL = New_Exchange_Balance - New_Funding = 0
        → SETTLED
```

**Example (Loss Case):**
```
Before:
  Funding = 100,000
  Exchange Balance = 30,000
  Client_PnL = -70,000
  Pending = 70,000

Full Payment: 70,000

After:
  Funding = 100,000 - 70,000 = 30,000
  Exchange Balance = 30,000
  Client_PnL = 30,000 - 30,000 = 0
  → SETTLED ✅
```

---

## Code Mapping

### Model Methods → Formulas

| Method | Formula | Location |
|--------|---------|----------|
| `compute_client_pnl()` | Formula 1 | `core/models.py:88` |
| `compute_my_share()` | Formula 13 | `core/models.py:97-98` |
| `is_settled()` | Formula 15 | `core/models.py:100-102` |

### View Functions → Formulas

| Function | Formulas Used | Location |
|----------|---------------|----------|
| `pending_summary()` | Formulas 4, 5, 6, 7, 11, 12 | `core/views.py:862-1079` |
| `record_payment()` | Formulas 8, 9, 10 | `core/views.py:3207` |

### Template Variables → Formulas

| Template Variable | Formula | Location |
|-------------------|---------|----------|
| `clients_owe_you` | Formula 11 | `core/templates/core/pending/summary.html:59` |
| `you_owe_clients` | Formula 11 | `core/templates/core/pending/summary.html:119` |
| `total_clients_owe` | Formula 6 | `core/templates/core/pending/summary.html:23` |
| `total_my_share_clients_owe` | Formula 6 | `core/templates/core/pending/summary.html:24` |
| `total_you_owe` | Formula 7 | `core/templates/core/pending/summary.html:28` |
| `total_my_share_you_owe` | Formula 7 | `core/templates/core/pending/summary.html:29` |

---

## Examples with Step-by-Step Calculations

### Example 1: Loss Case - Full Payment

**Initial State:**
```
Funding = 100,000
Exchange Balance = 30,000
My Percentage = 10%
```

**Step 1: Calculate Client_PnL**
```
Client_PnL = exchange_balance - funding
Client_PnL = 30,000 - 100,000 = -70,000
```

**Step 2: Determine Pending**
```
Since Client_PnL < 0:
    Pending_Amount = ABS(-70,000) = 70,000
    This goes to "Clients Owe You" section
```

**Step 3: Calculate My Share**
```
My_Share = ABS(Client_PnL) × my_percentage / 100
My_Share = 70,000 × 10 / 100 = 7,000
```

**Step 4: Record Full Payment**
```
paid_amount = 70,000

Since Client_PnL < 0 (loss case):
    New_Funding = 100,000 - 70,000 = 30,000
    New_Exchange_Balance = 30,000 (unchanged)
```

**Step 5: Recalculate After Payment**
```
New_Client_PnL = 30,000 - 30,000 = 0
Since New_Client_PnL == 0:
    Account is SETTLED ✅
    "Record Payment" button is HIDDEN
```

---

### Example 2: Profit Case - Partial Payment

**Initial State:**
```
Funding = 100,000
Exchange Balance = 170,000
My Percentage = 10%
```

**Step 1: Calculate Client_PnL**
```
Client_PnL = exchange_balance - funding
Client_PnL = 170,000 - 100,000 = +70,000
```

**Step 2: Determine Pending**
```
Since Client_PnL > 0:
    Pending_Amount = 70,000
    This goes to "You Owe Clients" section
```

**Step 3: Calculate My Share**
```
My_Share = ABS(Client_PnL) × my_percentage / 100
My_Share = 70,000 × 10 / 100 = 7,000
```

**Step 4: Record Partial Payment**
```
paid_amount = 30,000

Since Client_PnL > 0 (profit case):
    New_Funding = 100,000 (unchanged)
    New_Exchange_Balance = 170,000 - 30,000 = 140,000
```

**Step 5: Recalculate After Payment**
```
New_Client_PnL = 140,000 - 100,000 = +40,000
Since New_Client_PnL != 0:
    Account still PENDING
    New_Pending_Amount = 40,000
    New_My_Share = 40,000 × 10 / 100 = 4,000
    "Record Payment" button remains VISIBLE
```

---

### Example 3: Multiple Partial Payments

**Initial State:**
```
Funding = 200,000
Exchange Balance = 50,000
My Percentage = 15%
```

**Payment 1: ₹50,000**
```
Before:
  Client_PnL = 50,000 - 200,000 = -150,000
  Pending = 150,000
  My Share = 150,000 × 15 / 100 = 22,500

Payment: 50,000 (loss case)
  New_Funding = 200,000 - 50,000 = 150,000
  New_Exchange_Balance = 50,000 (unchanged)

After:
  Client_PnL = 50,000 - 150,000 = -100,000
  Pending = 100,000
  My Share = 100,000 × 15 / 100 = 15,000
  Status: Still PENDING
```

**Payment 2: ₹40,000**
```
Before:
  Client_PnL = -100,000
  Pending = 100,000

Payment: 40,000 (loss case)
  New_Funding = 150,000 - 40,000 = 110,000
  New_Exchange_Balance = 50,000 (unchanged)

After:
  Client_PnL = 50,000 - 110,000 = -60,000
  Pending = 60,000
  My Share = 60,000 × 15 / 100 = 9,000
  Status: Still PENDING
```

**Payment 3: ₹60,000**
```
Before:
  Client_PnL = -60,000
  Pending = 60,000

Payment: 60,000 (loss case)
  New_Funding = 110,000 - 60,000 = 50,000
  New_Exchange_Balance = 50,000 (unchanged)

After:
  Client_PnL = 50,000 - 50,000 = 0
  Pending = 0
  My Share = 0
  Status: SETTLED ✅
```

---

## Edge Cases and Validation

### Edge Case 1: Zero Amount Payment

**Validation Rule:**
```
IF paid_amount <= 0:
    ❌ ERROR: "Amount must be greater than 0"
```

**Code Location:** `core/views.py:3207` (record_payment function)

**Mathematical Justification:**
- Payment must represent actual money movement
- Zero or negative amounts are invalid

---

### Edge Case 2: Overpayment Attempt

**Validation Rule:**
```
For Loss Case:
    IF paid_amount > ABS(Client_PnL):
        ❌ ERROR: "Paid amount cannot exceed ABS(Client_PnL)"

For Profit Case:
    IF paid_amount > Client_PnL:
        ❌ ERROR: "Paid amount cannot exceed Client_PnL"
```

**Code Location:** `core/views.py:3207` (record_payment function)

**Mathematical Justification:**
- Cannot pay more than what is pending
- Prevents negative PnL after payment

**Example:**
```
Client_PnL = -70,000
Pending = 70,000
Attempted Payment: 80,000

❌ ERROR: Cannot pay 80,000 when pending is only 70,000
```

---

### Edge Case 3: Payment When Already Settled

**Validation Rule:**
```
IF Client_PnL == 0:
    ❌ ERROR: "Cannot record payment when Client_PnL = 0"
```

**Code Location:** `core/views.py:3207` (record_payment function)

**Mathematical Justification:**
- No pending amount exists when settled
- Payment would create imbalance

**Example:**
```
Funding = 100,000
Exchange Balance = 100,000
Client_PnL = 0
Attempted Payment: 10,000

❌ ERROR: Account is already settled
```

---

### Edge Case 4: Exact Payment Amount

**Scenario:**
```
paid_amount == ABS(Client_PnL)  (for loss case)
paid_amount == Client_PnL       (for profit case)
```

**Result:**
```
New_Client_PnL = 0
Account becomes SETTLED
"Record Payment" button disappears
```

**Mathematical Proof (Loss Case):**
```
Before:
  Funding = F
  Exchange Balance = E
  Client_PnL = E - F < 0
  paid_amount = ABS(E - F) = F - E

After:
  New_Funding = F - (F - E) = E
  New_Exchange_Balance = E
  New_Client_PnL = E - E = 0 ✅
```

---

### Edge Case 5: Negative Exchange Balance

**Scenario:**
```
exchange_balance < 0  (should not happen, but system handles it)
```

**Mathematical Behavior:**
```
Client_PnL = exchange_balance - funding
If exchange_balance < 0:
    Client_PnL will be more negative
    Pending amount increases
```

**System Handling:**
- System allows negative exchange_balance (no validation prevents it)
- All formulas work correctly with negative values
- `ABS()` ensures share calculations remain positive

---

### Edge Case 6: Zero Percentage Share

**Scenario:**
```
my_percentage = 0
```

**Mathematical Behavior:**
```
My_Share = ABS(Client_PnL) × 0 / 100 = 0
```

**Result:**
- Pending amount still calculated (full amount)
- My share is always zero
- Account can still be settled normally

---

## Formula Summary Table

| Formula ID | Name | Mathematical Definition | Code Location |
|------------|------|----------------------|---------------|
| 1 | Client PnL | `Client_PnL = exchange_balance - funding` | `models.py:88` |
| 2 | My Share | `My_Share = ABS(Client_PnL) × my_percentage / 100` | `models.py:97-98` |
| 3 | Settlement Check | `Is_Settled = (Client_PnL == 0)` | `models.py:100-102` |
| 4 | Pending (Loss) | `Pending = ABS(Client_PnL)` when `Client_PnL < 0` | `views.py:862-1079` |
| 5 | Pending (Profit) | `Pending = Client_PnL` when `Client_PnL > 0` | `views.py:862-1079` |
| 6 | Total Clients Owe | `Σ(ABS(Client_PnL))` for all `Client_PnL < 0` | `views.py:1062` |
| 7 | Total You Owe | `Σ(Client_PnL)` for all `Client_PnL > 0` | `views.py:1063` |
| 8 | Record Payment (Loss) | `New_Funding = Old_Funding - paid_amount` | `views.py:3207` |
| 9 | Record Payment (Profit) | `New_Exchange_Balance = Old_Exchange_Balance - paid_amount` | `views.py:3207` |
| 10 | Settlement After Payment | `New_Client_PnL == 0` → Settled | `views.py:3207` |

---

## Quick Reference Card

### Core Calculations
```
Client_PnL = exchange_balance - funding
My_Share = ABS(Client_PnL) × my_percentage / 100
Is_Settled = (Client_PnL == 0)
```

### Pending Amounts
```
IF Client_PnL < 0:
    Pending = ABS(Client_PnL)  (Clients Owe You)
ELIF Client_PnL > 0:
    Pending = Client_PnL  (You Owe Clients)
ELSE:
    Pending = 0  (Settled)
```

### Record Payment
```
Loss Case:
    funding = funding - paid_amount
    exchange_balance = exchange_balance  (unchanged)

Profit Case:
    funding = funding  (unchanged)
    exchange_balance = exchange_balance - paid_amount
```

### Validation Rules
```
1. paid_amount > 0
2. paid_amount <= ABS(Client_PnL)  (for loss case)
3. paid_amount <= Client_PnL  (for profit case)
4. Client_PnL != 0  (cannot pay when settled)
```

---

## Conclusion

This document provides complete pin-to-pin documentation of all pending payment formulas in the system. Every formula is:

1. **Mathematically Defined** - Clear mathematical notation
2. **Code Mapped** - Exact location in codebase
3. **Example Illustrated** - Step-by-step calculations
4. **Edge Case Covered** - All validation rules documented

All formulas derive from the two stored values (`funding` and `exchange_balance`) and are computed on-the-fly. No pending amounts are stored in the database.

---

**Document Version:** 2.0  
**Last Updated:** 2024-01-07  
**System:** Django Profit-Loss-Share-Settlement System  
**Documentation Type:** Pin-to-Pin Formula Reference

