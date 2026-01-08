# Pending Payments - Complete Documentation

**Version:** 1.0  
**System:** Profit-Loss-Share-Settlement System  
**Based on:** PIN-TO-PIN Master Document

---

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Pending Payments Calculation](#pending-payments-calculation)
4. [Record Payment Functionality](#record-payment-functionality)
5. [Partial Payments](#partial-payments)
6. [Formulas Reference](#formulas-reference)
7. [Examples](#examples)
8. [UI Behavior](#ui-behavior)
9. [Edge Cases](#edge-cases)
10. [System Rules](#system-rules)

---

## Overview

Pending Payments represent the **unsettled profit/loss** between clients and the partner. The system shows:

1. **Clients Owe You** - When clients are in loss (Client_PnL < 0)
2. **You Owe Clients** - When clients are in profit (Client_PnL > 0)

**Key Principle:** Pending payments are **COMPUTED**, never stored. They are derived from `funding` and `exchange_balance`.

---

## Core Concepts

### What is "Pending"?

**Pending** = Unsettled Profit/Loss

- **NOT** a stored value
- **NOT** a database field
- **COMPUTED** from: `Client_PnL = exchange_balance - funding`

### When is Payment "Pending"?

Payment is pending when:
```
Client_PnL ≠ 0
```

Payment is **NOT** pending when:
```
Client_PnL = 0  (Fully settled)
```

### Two Types of Pending

#### 1. Clients Owe You (LOSS Case)
- **Condition:** `Client_PnL < 0`
- **Meaning:** Client is in loss, owes you money
- **Amount:** `ABS(Client_PnL)` = Full loss amount
- **Your Share:** `ABS(Client_PnL) × my_percentage / 100`

#### 2. You Owe Clients (PROFIT Case)
- **Condition:** `Client_PnL > 0`
- **Meaning:** Client is in profit, you owe them money
- **Amount:** `Client_PnL` = Full profit amount
- **Your Share:** `Client_PnL × my_percentage / 100` (what you pay)

---

## Pending Payments Calculation

### Master Formula

```
Client_PnL = exchange_balance - funding
```

### Pending Amount Calculation

#### For "Clients Owe You" Section:
```python
if Client_PnL < 0:
    pending_amount = ABS(Client_PnL)
    my_share_pending = ABS(Client_PnL) × my_percentage / 100
```

#### For "You Owe Clients" Section:
```python
if Client_PnL > 0:
    pending_amount = Client_PnL
    my_share_pending = Client_PnL × my_percentage / 100
```

### Totals Calculation

```python
total_clients_owe = SUM(ABS(Client_PnL) for all accounts where Client_PnL < 0)
total_my_share_clients_owe = SUM(my_share for all accounts where Client_PnL < 0)

total_you_owe = SUM(Client_PnL for all accounts where Client_PnL > 0)
total_my_share_you_owe = SUM(my_share for all accounts where Client_PnL > 0)
```

---

## Record Payment Functionality

### Purpose

**Record Payment** is a balance-adjustment action that reflects actual money movement. It is **NOT** a settlement flag or payment log.

### When Button Appears

**Show "Record Payment" button IF AND ONLY IF:**
```
Client_PnL ≠ 0
```

**Hide "Record Payment" button IF:**
```
Client_PnL = 0  (Account is settled)
```

### Input Required

| Field | Type | Rule | Description |
|-------|------|------|-------------|
| `paid_amount` | BIGINT | `> 0` | Amount paid |
| | | `<= ABS(Client_PnL)` | Cannot exceed pending amount |
| `notes` | TEXT | Optional | Payment notes |

### Validation Rules

```python
if paid_amount <= 0:
    ❌ ERROR: "Amount must be greater than 0"

if paid_amount > ABS(Client_PnL):
    ❌ ERROR: "Paid amount cannot exceed ABS(Client_PnL)"

if Client_PnL == 0:
    ❌ ERROR: "Cannot record payment when Client_PnL = 0"
```

---

## Record Payment Logic

### CASE 1: Client in LOSS (Client_PnL < 0)

**Meaning:** Client owes money and has paid (full or partial).

**Action:**
```python
funding = funding - paid_amount
exchange_balance = exchange_balance  # UNCHANGED
```

**Formula:**
```
New Funding = Old Funding - Paid Amount
New Exchange Balance = Old Exchange Balance (no change)
New Client_PnL = New Exchange Balance - New Funding
```

**Example - Full Payment:**

**Before:**
```
Funding = 100,000
Exchange Balance = 30,000
Client_PnL = 30,000 - 100,000 = -70,000
```

**Client pays ₹70,000**

**After:**
```
Funding = 100,000 - 70,000 = 30,000
Exchange Balance = 30,000 (unchanged)
Client_PnL = 30,000 - 30,000 = 0 ✅ SETTLED
```

**Example - Partial Payment:**

**Before:**
```
Funding = 100,000
Exchange Balance = 30,000
Client_PnL = -70,000
```

**Client pays ₹20,000**

**After:**
```
Funding = 100,000 - 20,000 = 80,000
Exchange Balance = 30,000 (unchanged)
Client_PnL = 30,000 - 80,000 = -50,000
Pending remains: ₹50,000
```

---

### CASE 2: Client in PROFIT (Client_PnL > 0)

**Meaning:** Client is owed money, you paid (full or partial), client withdrew from exchange.

**Action:**
```python
funding = funding  # UNCHANGED
exchange_balance = exchange_balance - paid_amount
```

**Formula:**
```
New Funding = Old Funding (no change)
New Exchange Balance = Old Exchange Balance - Paid Amount
New Client_PnL = New Exchange Balance - New Funding
```

**Example - Full Payment:**

**Before:**
```
Funding = 100,000
Exchange Balance = 170,000
Client_PnL = 170,000 - 100,000 = +70,000
```

**You pay ₹70,000 to client**

**After:**
```
Funding = 100,000 (unchanged)
Exchange Balance = 170,000 - 70,000 = 100,000
Client_PnL = 100,000 - 100,000 = 0 ✅ SETTLED
```

**Example - Partial Payment:**

**Before:**
```
Funding = 100,000
Exchange Balance = 170,000
Client_PnL = +70,000
```

**You pay ₹30,000**

**After:**
```
Funding = 100,000 (unchanged)
Exchange Balance = 170,000 - 30,000 = 140,000
Client_PnL = 140,000 - 100,000 = +40,000
Pending remains: ₹40,000
```

---

## Partial Payments

### How Partial Payments Work

Partial payments are **repeated Record Payment actions** until `Client_PnL = 0`.

### Process Flow

1. **Initial State:** `Client_PnL ≠ 0`
2. **User clicks "Record Payment"**
3. **User enters partial amount** (e.g., ₹20,000 when pending is ₹70,000)
4. **System applies payment logic** (reduces funding or exchange_balance)
5. **System recomputes Client_PnL**
6. **If Client_PnL ≠ 0:** Button remains visible, user can record another payment
7. **If Client_PnL = 0:** Button disappears, account is settled

### Example: Multiple Partial Payments

**Initial State:**
```
Funding = 100,000
Exchange Balance = 30,000
Client_PnL = -70,000
Pending: ₹70,000
```

**Payment 1: ₹20,000**
```
Funding = 100,000 - 20,000 = 80,000
Exchange Balance = 30,000
Client_PnL = 30,000 - 80,000 = -50,000
Pending: ₹50,000 ✅ Button still visible
```

**Payment 2: ₹30,000**
```
Funding = 80,000 - 30,000 = 50,000
Exchange Balance = 30,000
Client_PnL = 30,000 - 50,000 = -20,000
Pending: ₹20,000 ✅ Button still visible
```

**Payment 3: ₹20,000**
```
Funding = 50,000 - 20,000 = 30,000
Exchange Balance = 30,000
Client_PnL = 30,000 - 30,000 = 0
Pending: ₹0 ✅ SETTLED, Button hidden
```

### Validation for Partial Payments

```python
# Each payment must satisfy:
paid_amount > 0
paid_amount <= ABS(Client_PnL)  # Current pending amount

# After payment:
new_Client_PnL = exchange_balance - funding

# If new_Client_PnL == 0:
    # Account settled, button disappears
# Else:
    # Account still pending, button remains visible
```

---

## Formulas Reference

### Core Formulas

#### 1. Client Profit/Loss
```
Client_PnL = exchange_balance - funding
```

#### 2. My Share (Partner Share)
```
My_Share = ABS(Client_PnL) × my_percentage / 100
```

#### 3. Pending Amount
```
if Client_PnL < 0:
    Pending = ABS(Client_PnL)
    
if Client_PnL > 0:
    Pending = Client_PnL
```

#### 4. Settlement Check
```
if Client_PnL == 0:
    Status = "Fully Settled"
    Button = HIDDEN
else:
    Status = "Action Required"
    Button = VISIBLE
```

### Record Payment Formulas

#### LOSS Case (Client_PnL < 0)
```
New_Funding = Old_Funding - paid_amount
New_Exchange_Balance = Old_Exchange_Balance
New_Client_PnL = New_Exchange_Balance - New_Funding
```

#### PROFIT Case (Client_PnL > 0)
```
New_Funding = Old_Funding
New_Exchange_Balance = Old_Exchange_Balance - paid_amount
New_Client_PnL = New_Exchange_Balance - New_Funding
```

---

## Examples

### Example 1: Full Payment - Loss Case

**Initial Account:**
- Funding: ₹100,000
- Exchange Balance: ₹30,000
- My Percentage: 10%

**Calculations:**
```
Client_PnL = 30,000 - 100,000 = -70,000
Pending Amount = ABS(-70,000) = ₹70,000
My Share = 70,000 × 10 / 100 = ₹7,000
```

**User records payment: ₹70,000**

**After Payment:**
```
Funding = 100,000 - 70,000 = ₹30,000
Exchange Balance = ₹30,000 (unchanged)
Client_PnL = 30,000 - 30,000 = 0 ✅ SETTLED
Pending Amount = ₹0
My Share = ₹0
Button = HIDDEN
```

---

### Example 2: Partial Payment - Profit Case

**Initial Account:**
- Funding: ₹100,000
- Exchange Balance: ₹170,000
- My Percentage: 10%

**Calculations:**
```
Client_PnL = 170,000 - 100,000 = +70,000
Pending Amount = ₹70,000
My Share = 70,000 × 10 / 100 = ₹7,000
```

**User records payment: ₹30,000**

**After Payment:**
```
Funding = ₹100,000 (unchanged)
Exchange Balance = 170,000 - 30,000 = ₹140,000
Client_PnL = 140,000 - 100,000 = +40,000
Pending Amount = ₹40,000
My Share = 40,000 × 10 / 100 = ₹4,000
Button = VISIBLE (still pending)
```

**User records another payment: ₹40,000**

**After Second Payment:**
```
Funding = ₹100,000 (unchanged)
Exchange Balance = 140,000 - 40,000 = ₹100,000
Client_PnL = 100,000 - 100,000 = 0 ✅ SETTLED
Pending Amount = ₹0
My Share = ₹0
Button = HIDDEN
```

---

### Example 3: Multiple Partial Payments - Loss Case

**Initial Account:**
- Funding: ₹200,000
- Exchange Balance: ₹50,000
- My Percentage: 15%

**Calculations:**
```
Client_PnL = 50,000 - 200,000 = -150,000
Pending Amount = ₹150,000
My Share = 150,000 × 15 / 100 = ₹22,500
```

**Payment 1: ₹50,000**
```
Funding = 200,000 - 50,000 = ₹150,000
Exchange Balance = ₹50,000
Client_PnL = 50,000 - 150,000 = -100,000
Pending: ₹100,000
My Share: ₹15,000
```

**Payment 2: ₹40,000**
```
Funding = 150,000 - 40,000 = ₹110,000
Exchange Balance = ₹50,000
Client_PnL = 50,000 - 110,000 = -60,000
Pending: ₹60,000
My Share: ₹9,000
```

**Payment 3: ₹60,000**
```
Funding = 110,000 - 60,000 = ₹50,000
Exchange Balance = ₹50,000
Client_PnL = 50,000 - 50,000 = 0 ✅ SETTLED
Pending: ₹0
My Share: ₹0
```

---

## UI Behavior

### Pending Payments Page

#### Section: "Clients Owe You"
- **Shows:** Accounts where `Client_PnL < 0`
- **Columns:**
  - Client, Exchange
  - Funding, Exchange Balance (stored values)
  - Client PnL (computed)
  - Amount Owed (computed: ABS(Client_PnL))
  - My Share (computed)
  - My % (stored)
  - Actions: "Record Payment" button + "View Account"

#### Section: "You Owe Clients"
- **Shows:** Accounts where `Client_PnL > 0`
- **Columns:**
  - Client, Exchange
  - Funding, Exchange Balance (stored values)
  - Client PnL (computed)
  - Client Profit (computed: Client_PnL)
  - My Share (You Pay) (computed)
  - My % (stored)
  - Actions: "Record Payment" button + "View Account"

### Account Detail Page

- **Shows:** "Record Payment" button **only when** `Client_PnL ≠ 0`
- **Hides:** Button when `Client_PnL = 0` (settled)

### Record Payment Form

- **Shows:** Current account status (Funding, Exchange Balance, Client PnL)
- **Shows:** Case-specific instructions:
  - LOSS Case: "Will reduce funding"
  - PROFIT Case: "Will reduce exchange_balance"
- **Shows:** Maximum allowed amount: `ABS(Client_PnL)`
- **Validates:** Amount must be `> 0` and `<= ABS(Client_PnL)`

---

## Edge Cases

### Edge Case 1: Exact Payment Amount

**Scenario:** User pays exactly `ABS(Client_PnL)`

**Result:**
```
Client_PnL becomes exactly 0
Account is fully settled
Button disappears
```

### Edge Case 2: Overpayment Attempt

**Scenario:** User tries to pay more than `ABS(Client_PnL)`

**Result:**
```
❌ Validation Error: "Paid amount cannot exceed ABS(Client_PnL)"
Form is not submitted
Account remains unchanged
```

### Edge Case 3: Payment When Already Settled

**Scenario:** User tries to record payment when `Client_PnL = 0`

**Result:**
```
❌ Error: "Cannot record payment when Client_PnL = 0"
Redirected to account detail page
```

### Edge Case 4: Zero Amount Payment

**Scenario:** User tries to pay ₹0

**Result:**
```
❌ Validation Error: "Amount must be greater than 0"
Form is not submitted
```

### Edge Case 5: Multiple Rapid Payments

**Scenario:** User records multiple payments quickly

**Result:**
```
Each payment is processed independently
Each payment reduces pending amount
System recomputes Client_PnL after each payment
Button disappears when Client_PnL = 0
```

---

## System Rules

### Rule 1: No Stored Pending Values

```
❌ Do NOT store:
- pending_amount
- settlement_status
- paid/unpaid flags
- pending flags

✅ DO compute:
- Client_PnL = exchange_balance - funding
- Pending = ABS(Client_PnL) if Client_PnL ≠ 0
```

### Rule 2: Button Visibility

```
Show "Record Payment" IF AND ONLY IF:
Client_PnL ≠ 0

Hide "Record Payment" IF:
Client_PnL = 0
```

### Rule 3: Payment Validation

```
paid_amount > 0
paid_amount <= ABS(Client_PnL)
Client_PnL ≠ 0
```

### Rule 4: Payment Logic

```
IF Client_PnL < 0:
    funding = funding - paid_amount
    exchange_balance = exchange_balance  # UNCHANGED

ELSE IF Client_PnL > 0:
    funding = funding  # UNCHANGED
    exchange_balance = exchange_balance - paid_amount
```

### Rule 5: After Payment

```
ALWAYS recompute:
New_Client_PnL = exchange_balance - funding

IF New_Client_PnL == 0:
    Account is SETTLED
    Hide button
ELSE:
    Account still PENDING
    Show button
```

### Rule 6: Audit Trail

```
Each payment creates a Transaction record:
- Type: RECORD_PAYMENT
- Amount: paid_amount
- Exchange Balance After: new exchange_balance
- Notes: Payment details

Note: Transactions are AUDIT ONLY, never used for logic
```

---

## Implementation Details

### Backend Logic (Python)

```python
def record_payment(request, account_id):
    account = get_account(account_id)
    client_pnl = account.compute_client_pnl()
    
    # Validate
    if client_pnl == 0:
        return error("Account already settled")
    
    paid_amount = form.cleaned_data['paid_amount']
    
    if paid_amount > abs(client_pnl):
        return error("Amount exceeds pending")
    
    # Apply payment logic
    if client_pnl < 0:  # LOSS
        account.funding -= paid_amount
    elif client_pnl > 0:  # PROFIT
        account.exchange_balance -= paid_amount
    
    account.save()
    
    # Recompute
    new_client_pnl = account.compute_client_pnl()
    
    # Create audit entry
    Transaction.objects.create(
        type='RECORD_PAYMENT',
        amount=paid_amount,
        exchange_balance_after=account.exchange_balance,
        notes=f"Payment recorded. Old Funding: {old_funding}"
    )
    
    return success()
```

### Frontend Logic (Template)

```django
{% if not is_settled %}
    <a href="{% url 'record_payment' account.pk %}">Record Payment</a>
{% endif %}
```

---

## Summary

### Key Points

1. **Pending = Computed, Not Stored**
   - Derived from `Client_PnL = exchange_balance - funding`
   - Never stored in database

2. **Two Types of Pending**
   - Clients Owe You (Loss): `Client_PnL < 0`
   - You Owe Clients (Profit): `Client_PnL > 0`

3. **Record Payment Logic**
   - Loss: Reduces `funding`
   - Profit: Reduces `exchange_balance`
   - Never changes both values

4. **Partial Payments**
   - Multiple payments allowed
   - Each payment reduces pending
   - Button disappears when settled

5. **Validation**
   - Amount must be `> 0`
   - Amount must be `<= ABS(Client_PnL)`
   - Cannot pay when `Client_PnL = 0`

6. **Settlement**
   - Account settled when `Client_PnL = 0`
   - Button automatically hidden
   - Status shows "N.A"

---

## Quick Reference

### Formulas Cheat Sheet

```
Client_PnL = exchange_balance - funding

Pending (Loss) = ABS(Client_PnL) when Client_PnL < 0
Pending (Profit) = Client_PnL when Client_PnL > 0

My Share = ABS(Client_PnL) × my_percentage / 100

Record Payment (Loss):
    funding = funding - paid_amount

Record Payment (Profit):
    exchange_balance = exchange_balance - paid_amount

Settled Check:
    Client_PnL == 0 → Settled
    Client_PnL != 0 → Pending
```

---

**Document Version:** 1.0  
**Last Updated:** 2024-01-07  
**System:** Django Profit-Loss-Share-Settlement System  
**Based on:** PIN-TO-PIN Master Document


