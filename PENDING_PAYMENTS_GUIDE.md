# Pending Payments System - Complete Guide

**Version:** 2.0  
**Last Updated:** January 2025

---

## 📋 Table of Contents

1. [Introduction](#introduction)
2. [How It Works](#how-it-works)
3. [Key Concepts](#key-concepts)
4. [Formulas](#formulas)
5. [Step-by-Step Process](#step-by-step-process)
6. [Examples](#examples)
7. [Common Scenarios](#common-scenarios)
8. [Troubleshooting](#troubleshooting)

---

## Introduction

The **Pending Payments System** tracks settlement amounts between you and your clients based on their trading results. It automatically calculates how much money needs to be exchanged based on profit/loss and your share percentage.

### What It Does

- ✅ Calculates settlement amounts based on trading outcomes
- ✅ Tracks remaining amounts across multiple payments
- ✅ Prevents errors with automatic validations
- ✅ Separates different trading cycles
- ✅ Shows clear "who owes whom" information

### Two Main Sections

1. **Clients Owe You** - When clients are in loss
2. **You Owe Clients** - When clients are in profit

---

## How It Works

### Basic Flow

```
1. Client trades → Creates profit or loss
2. System calculates your share → Based on percentage
3. System locks the share → Prevents changes
4. You record payments → Reduces remaining amount
5. System tracks progress → Shows what's left
```

### Key Principle

**Share is decided by trading, NOT by payments**

- Share amount is locked when trading creates profit/loss
- Share never shrinks after payments
- Payments reduce the remaining amount, not the share

---

## Key Concepts

### 1. Client PnL (Profit/Loss)

**What it is:** The difference between exchange balance and funding

**Formula:**
```
Client_PnL = Exchange Balance - Funding
```

**What it means:**
- **Positive (+X)**: Client made profit → You owe client
- **Negative (-X)**: Client made loss → Client owes you
- **Zero (0)**: Trading flat → No settlement needed

**Example:**
- Funding: ₹100
- Exchange Balance: ₹50
- Client_PnL = 50 - 100 = **-₹50** (Loss)

### 2. Final Share (Your Share Amount)

**What it is:** Your portion of the profit/loss

**Formula:**
```
FinalShare = floor(|Client_PnL| × SharePercentage / 100)
```

**Where SharePercentage depends on:**
- **Loss case**: Uses `loss_share_percentage` (or `my_percentage`)
- **Profit case**: Uses `profit_share_percentage` (or `my_percentage`)
- **Zero PnL**: Returns 0 (no share)

**Example:**
- Client_PnL = -₹90 (Loss)
- Loss Share Percentage = 10%
- FinalShare = floor(90 × 10 / 100) = **₹9**

### 3. Remaining Amount

**What it is:** How much settlement is still pending

**Formula:**
```
RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)
```

**Display Sign:**
- **Loss case**: Shows as **POSITIVE** (client owes you)
- **Profit case**: Shows as **NEGATIVE** (you owe client)

**Example:**
- Locked Share: ₹9
- Already Paid: ₹5
- RemainingRaw = max(0, 9 - 5) = **₹4**
- Display: **+₹4** (if loss) or **-₹4** (if profit)

### 4. Masked Capital

**What it is:** The amount deducted from balance when payment is recorded

**Formula:**
```
MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
```

**Purpose:** Maps share payment back to PnL linearly

**Example:**
- Share Payment: ₹3
- Locked Initial PnL: ₹90
- Locked Initial Share: ₹9
- MaskedCapital = (3 × 90) / 9 = **₹30**

**Impact:**
- **Loss case**: Reduces Funding by MaskedCapital
- **Profit case**: Reduces Exchange Balance by MaskedCapital

---

## Formulas

### Master Formula List

#### 1. Client PnL
```
Client_PnL = Exchange Balance - Funding
```

#### 2. Final Share
```
IF Client_PnL == 0:
    FinalShare = 0
ELSE:
    SharePercentage = get_share_percentage(Client_PnL)
    FinalShare = floor(|Client_PnL| × SharePercentage / 100)
```

#### 3. Remaining Amount (Raw)
```
RemainingRaw = max(0, LockedInitialFinalShare - TotalSettled)
```

#### 4. Display Remaining (Signed)
```
IF Client_PnL < 0 (Loss):
    DisplayRemaining = +RemainingRaw  (client owes you)
ELSE IF Client_PnL > 0 (Profit):
    DisplayRemaining = -RemainingRaw  (you owe client)
ELSE:
    DisplayRemaining = 0
```

#### 5. Masked Capital
```
MaskedCapital = (SharePayment × |LockedInitialPnL|) / LockedInitialFinalShare
```

#### 6. Balance Update
```
IF Client_PnL < 0 (Loss):
    Funding = Funding - MaskedCapital
ELSE IF Client_PnL > 0 (Profit):
    Exchange Balance = Exchange Balance - MaskedCapital
```

#### 7. Transaction Sign
```
# Calculate BEFORE balance update
Client_PnL_before = compute_client_pnl()

IF Client_PnL_before > 0:
    Transaction.amount = -SharePayment  (you paid client)
ELSE:
    Transaction.amount = +SharePayment  (client paid you)
```

---

## Step-by-Step Process

### Process 1: Initial Setup

**When:** Client starts trading

1. **Set Initial Values**
   - Funding: ₹100
   - Exchange Balance: ₹50

2. **Calculate Client_PnL**
   ```
   Client_PnL = 50 - 100 = -₹50 (Loss)
   ```

3. **Calculate Final Share**
   ```
   FinalShare = floor(50 × 10 / 100) = ₹5
   ```

4. **Lock Share**
   - System automatically locks share
   - Stores: locked_initial_final_share = ₹5
   - Sets cycle_start_date

5. **Calculate Remaining**
   ```
   RemainingRaw = max(0, 5 - 0) = ₹5
   DisplayRemaining = +₹5 (client owes you)
   ```

### Process 2: Recording a Payment

**When:** Client pays you (or you pay client)

1. **User Clicks "Record Payment"**
   - System loads account
   - Shows current remaining amount

2. **User Enters Payment Amount**
   - Example: ₹3

3. **System Validates**
   - ✅ Checks PnL is not zero
   - ✅ Checks share is not zero
   - ✅ Checks payment ≤ remaining
   - ✅ Checks balance won't go negative

4. **System Locks Account**
   - Prevents concurrent changes
   - Uses database row locking

5. **System Calculates Client_PnL Before**
   ```
   Client_PnL_before = -₹50 (Loss)
   ```

6. **System Decides Transaction Sign**
   ```
   Transaction.amount = +₹3 (client paid you)
   ```

7. **System Calculates Masked Capital**
   ```
   MaskedCapital = (3 × 50) / 5 = ₹30
   ```

8. **System Updates Balance**
   ```
   Funding = 100 - 30 = ₹70
   Exchange Balance = ₹50 (unchanged)
   ```

9. **System Creates Records**
   - Settlement record: amount = ₹3
   - Transaction record: amount = +₹3

10. **System Updates Remaining**
    ```
    RemainingRaw = max(0, 5 - 3) = ₹2
    DisplayRemaining = +₹2 (client owes you)
    ```

### Process 3: Multiple Payments

**Scenario:** Client pays in installments

**Payment 1: ₹2**
- Remaining: ₹5 → ₹3
- Balance: Funding ₹100 → ₹80

**Payment 2: ₹2**
- Remaining: ₹3 → ₹1
- Balance: Funding ₹80 → ₹60

**Payment 3: ₹1**
- Remaining: ₹1 → ₹0 (Settled!)
- Balance: Funding ₹60 → ₹50

---

## Examples

### Example 1: Loss Case - Complete Settlement

**Initial State:**
- Funding: ₹100
- Exchange Balance: ₹10
- Loss Share Percentage: 10%

**Step 1: Calculate Client_PnL**
```
Client_PnL = 10 - 100 = -₹90 (Loss)
```

**Step 2: Calculate Final Share**
```
FinalShare = floor(90 × 10 / 100) = ₹9
```

**Step 3: Lock Share**
```
locked_initial_final_share = ₹9
```

**Step 4: Show Remaining**
```
RemainingRaw = ₹9
DisplayRemaining = +₹9 (client owes you)
```

**Step 5: Record Payment of ₹9**
```
SharePayment = ₹9
MaskedCapital = (9 × 90) / 9 = ₹90

Funding = 100 - 90 = ₹10
New Client_PnL = 10 - 10 = ₹0 (Settled!)

RemainingRaw = max(0, 9 - 9) = ₹0
DisplayRemaining = ₹0 (Settled)
```

### Example 2: Profit Case - Partial Payments

**Initial State:**
- Funding: ₹100
- Exchange Balance: ₹290
- Profit Share Percentage: 20%

**Step 1: Calculate Client_PnL**
```
Client_PnL = 290 - 100 = +₹190 (Profit)
```

**Step 2: Calculate Final Share**
```
FinalShare = floor(190 × 20 / 100) = ₹38
```

**Step 3: Lock Share**
```
locked_initial_final_share = ₹38
```

**Step 4: Show Remaining**
```
RemainingRaw = ₹38
DisplayRemaining = -₹38 (you owe client)
```

**Step 5: Record Payment 1 of ₹15**
```
SharePayment = ₹15
MaskedCapital = (15 × 190) / 38 = ₹75

Exchange Balance = 290 - 75 = ₹215
New Client_PnL = 215 - 100 = +₹115

RemainingRaw = max(0, 38 - 15) = ₹23
DisplayRemaining = -₹23 (you owe client)
```

**Step 6: Record Payment 2 of ₹23**
```
SharePayment = ₹23
MaskedCapital = (23 × 190) / 38 = ₹115

Exchange Balance = 215 - 115 = ₹100
New Client_PnL = 100 - 100 = ₹0 (Settled!)

RemainingRaw = max(0, 38 - 38) = ₹0
DisplayRemaining = ₹0 (Settled)
```

### Example 3: Zero PnL (Trading Flat)

**State:**
- Funding: ₹100
- Exchange Balance: ₹100
- Client_PnL = 100 - 100 = ₹0

**Result:**
- FinalShare = 0
- Remaining = 0
- Display: **N.A** (No settlement needed)
- Settlement blocked

---

## Common Scenarios

### Scenario 1: Client Pays in Installments

**Question:** Can a client pay in multiple installments?

**Answer:** Yes! The system tracks remaining amount across all payments.

**Example:**
- Total Share: ₹10
- Payment 1: ₹4 → Remaining: ₹6
- Payment 2: ₹3 → Remaining: ₹3
- Payment 3: ₹3 → Remaining: ₹0 (Settled)

### Scenario 2: What Happens After Settlement?

**Question:** What happens when remaining reaches zero?

**Answer:**
- Remaining shows ₹0
- Status shows "Settled"
- No more payments can be recorded
- If trading continues and creates new PnL, new cycle starts

### Scenario 3: PnL Changes During Settlement

**Question:** What if PnL changes while settling?

**Answer:**
- Old cycle continues until fully settled
- New PnL creates new cycle with new locked share
- Old and new cycles are tracked separately

**Example:**
- Cycle 1 (Loss): Share ₹10, Paid ₹5, Remaining ₹5
- Trading changes: New Profit cycle starts
- Cycle 2 (Profit): New Share ₹8, Paid ₹0, Remaining ₹8
- Both cycles tracked independently

### Scenario 4: Overpayment

**Question:** What if client pays more than remaining?

**Answer:**
- System prevents overpayment
- Shows error: "Paid amount cannot exceed remaining"
- Payment is rejected

### Scenario 5: Zero Share

**Question:** Why does some client show "N.A"?

**Answer:** Shows "N.A" when:
- Client_PnL = 0 (trading flat)
- FinalShare = 0 (share percentage too small or PnL too small)

**Action:** No settlement needed or possible

---

## Troubleshooting

### Issue 1: Remaining Amount Shows Wrong Sign

**Symptom:** Profit case shows positive remaining

**Solution:** Check that display sign is applied correctly:
```python
IF Client_PnL > 0:
    DisplayRemaining = -RemainingRaw  # Negative
ELSE:
    DisplayRemaining = +RemainingRaw  # Positive
```

### Issue 2: Share Keeps Changing

**Symptom:** Share amount changes after payment

**Solution:** Ensure share is locked:
- Check `locked_initial_final_share` is set
- Verify `lock_initial_share_if_needed()` is called
- Share should never change after locking

### Issue 3: Old Payments Affecting New Cycle

**Symptom:** Settlements from old cycle counted in new cycle

**Solution:** Ensure cycle filtering:
```python
settlements = Settlement.objects.filter(
    client_exchange=account,
    date__gte=cycle_start_date
)
```

### Issue 4: Transaction Sign Wrong

**Symptom:** Profit case shows positive transaction

**Solution:** Calculate sign BEFORE balance update:
```python
# CORRECT ORDER
client_pnl_before = compute_client_pnl()
transaction_amount = -paid if client_pnl_before > 0 else +paid
apply_masked_capital()  # THEN update
```

### Issue 5: Balance Goes Negative

**Symptom:** Payment makes balance negative

**Solution:** Validate before updating:
```python
IF Client_PnL < 0:
    IF Funding - MaskedCapital < 0:
        Raise ValidationError
ELSE:
    IF ExchangeBalance - MaskedCapital < 0:
        Raise ValidationError
```

---

## Quick Reference

### Key Formulas

| Concept | Formula |
|---------|---------|
| Client PnL | `Exchange Balance - Funding` |
| Final Share | `floor(\|Client_PnL\| × Share% / 100)` |
| Remaining | `max(0, LockedShare - TotalSettled)` |
| Masked Capital | `(Payment × \|LockedPnL\|) / LockedShare` |

### Sign Conventions

| Case | Client_PnL | DisplayRemaining | Transaction |
|------|------------|------------------|-------------|
| Loss | Negative | Positive (+) | Positive (+) |
| Profit | Positive | Negative (-) | Negative (-) |

### Display Rules

- **Loss Case**: Remaining shows as **POSITIVE** (client owes you)
- **Profit Case**: Remaining shows as **NEGATIVE** (you owe client)
- **Zero PnL**: Shows **N.A** (no settlement)
- **Zero Share**: Shows **N.A** (no settlement)

---

## Summary

The Pending Payments System:

✅ **Tracks settlements** accurately across multiple payments  
✅ **Locks shares** to prevent changes  
✅ **Separates cycles** to maintain accuracy  
✅ **Shows clear signs** for who owes whom  
✅ **Validates payments** to prevent errors  
✅ **Handles edge cases** gracefully  

**Remember:**
- Share is decided by trading, not payments
- Transaction sign is determined BEFORE balance update
- Remaining is stored positive, signed at display time
- Cycles are separated to prevent mixing

---

**End of Guide**

