# Transaction Hub - Complete Project Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Database Models](#database-models)
4. [Core Features](#core-features)
5. [Formulas and Calculations](#formulas-and-calculations)
6. [Business Logic](#business-logic)
7. [Transaction Types](#transaction-types)
8. [Settlement System](#settlement-system)
9. [Pending Payments System](#pending-payments-system)
10. [Reports System](#reports-system)
11. [Dashboard](#dashboard)
12. [Authentication System](#authentication-system)
13. [API/URL Structure](#apiurl-structure)
14. [Implementation Details](#implementation-details)
15. [Data Flow](#data-flow)
16. [Error Handling](#error-handling)
17. [Security Features](#security-features)

---

## Project Overview

**Transaction Hub** is a comprehensive Django-based financial management system designed for tracking client trading activities, profit/loss calculations, and settlement management across multiple exchanges.

### Key Features
- **Client Management**: Manage clients and their trading accounts
- **Exchange Management**: Configure and manage multiple trading exchanges
- **Transaction Tracking**: Complete audit trail of all financial transactions
- **Profit/Loss Calculation**: Real-time PnL calculation using PIN-TO-PIN methodology
- **Settlement System**: Masked Share Settlement System with locked share amounts
- **Pending Payments**: Track outstanding payments between admin and clients
- **Reports & Analytics**: Daily, weekly, monthly, and custom date range reports
- **CSV Export**: Export reports and pending payments to CSV format

### Technology Stack
- **Backend**: Django 4.2.27
- **Database**: PostgreSQL
- **Python**: 3.12
- **Server**: Gunicorn
- **Authentication**: Custom user model with email OTP verification

---

## System Architecture

### Design Principles

1. **PIN-TO-PIN System**: Only two money values stored (`funding` and `exchange_balance`). All other values are derived.
2. **Masked Share Settlement**: Share amounts are locked at first calculation to prevent shrinking after payments.
3. **Audit Trail**: All transactions are recorded with before/after balance snapshots.
4. **Cycle-Based Tracking**: Separate settlement cycles for profit and loss periods.

### Core Components

```
┌─────────────────┐
│   CustomUser    │
└────────┬────────┘
         │
         │ owns
         ▼
┌─────────────────┐
│     Client      │
└────────┬────────┘
         │
         │ has many
         ▼
┌─────────────────────────┐
│ ClientExchangeAccount   │◄─────┐
└────────┬────────────────┘      │
         │                       │
         │ linked to            │
         ▼                       │
┌─────────────────┐             │
│    Exchange     │             │
└─────────────────┘             │
                                 │
         ┌───────────────────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────────┐
│  Transaction    │      │   Settlement      │
└─────────────────┘      └──────────────────┘
         │
         │ references
         ▼
┌─────────────────────────┐
│ ClientExchangeReportConfig│
└─────────────────────────┘
```

---

## Database Models

### 1. CustomUser

**Purpose**: Custom user model with flexible username requirements.

**Fields**:
- `username` (CharField, max_length=30, unique=True): Username (4-30 characters, any characters allowed)
- Standard Django user fields (email, password, etc.)

**Validation**:
- Username must be 4-30 characters
- No character restrictions (unlike default Django)

**Location**: `core/models.py`

---

### 2. Client

**Purpose**: Represents a trading client.

**Fields**:
- `name` (CharField, max_length=200): Client name
- `code` (CharField, max_length=50, blank=True, null=True, unique=True): Unique client code (optional)
- `referred_by` (CharField, max_length=200, blank=True, null=True): Referrer information
- `is_company_client` (BooleanField, default=False): Company client flag
- `user` (ForeignKey to CustomUser): Owner of this client
- `created_at`, `updated_at` (DateTimeField): Timestamps

**Business Rules**:
- Client code must be UNIQUE if provided (non-NULL)
- Client code can be EMPTY/NULL
- Multiple clients can have the same name
- Two clients must NEVER have the same non-NULL client code

**Validation**:
```python
# Empty strings are converted to None
if code is None:
    # Multiple NULL codes allowed
else:
    # Code must be unique
```

---

### 3. Exchange

**Purpose**: Represents a trading exchange/platform.

**Fields**:
- `name` (CharField, max_length=200): Exchange name (case-insensitive unique)
- `code` (CharField, max_length=50, blank=True, null=True, unique=True): Exchange code
- `version` (CharField, max_length=50, blank=True, null=True): Optional version identifier
- `is_active` (BooleanField, default=True): Whether exchange is active
- `created_at`, `updated_at` (DateTimeField): Timestamps

**Business Rules**:
- Exchange name must be unique (case-insensitive)
- Exchange can be disabled (is_active=False)
- Version is optional identifier

**Constraints**:
- Unique constraint on `name` (case-insensitive)

---

### 4. ClientExchangeAccount

**Purpose**: CORE SYSTEM TABLE - Links client to exchange, stores ONLY real money values.

**Fields**:

#### Core Money Fields (ONLY TWO VALUES STORED)
- `funding` (BigIntegerField, default=0): Total real money given to client
- `exchange_balance` (BigIntegerField, default=0): Current balance on exchange

#### Percentage Fields
- `my_percentage` (DecimalField, max_digits=5, decimal_places=2, default=0): Default admin share percentage (0-100, decimals allowed)
- `loss_share_percentage` (IntegerField, default=0): Admin share percentage for losses (0-100)
- `profit_share_percentage` (IntegerField, default=0): Admin share percentage for profits (0-100)

#### Locked Share Fields (Cycle Management)
- `locked_initial_final_share` (BigIntegerField, null=True): Initial FinalShare locked at start of PnL cycle
- `locked_share_percentage` (IntegerField, null=True): Share percentage locked at start of PnL cycle
- `locked_initial_pnl` (BigIntegerField, null=True): Initial PnL when share was locked
- `cycle_start_date` (DateTimeField, null=True): Timestamp when current PnL cycle started
- `locked_initial_funding` (BigIntegerField, null=True): Funding amount when share was locked

**Relationships**:
- `client` (ForeignKey to Client)
- `exchange` (ForeignKey to Exchange)

**Constraints**:
- Unique together: `['client', 'exchange']`

**Key Methods**:
- `compute_client_pnl()`: Calculate Client_PnL = exchange_balance - funding
- `get_share_percentage(client_pnl)`: Get appropriate share percentage based on PnL direction
- `compute_my_share()`: Calculate final share using floor rounding
- `lock_initial_share_if_needed()`: Lock share at first calculation
- `get_remaining_settlement_amount()`: Calculate remaining settlement using locked share
- `close_cycle()`: Reset cycle-related fields

**Business Rules**:
- Only `funding` and `exchange_balance` are stored
- All other values (profit, loss, shares) are DERIVED, never stored
- Loss share percentage is IMMUTABLE once data exists
- Share is locked at first calculation and NEVER shrinks after payments

---

### 5. ClientExchangeReportConfig

**Purpose**: Report-only configuration for splitting profit between friend/company and admin.

**Fields**:
- `client_exchange` (OneToOneField to ClientExchangeAccount): Linked account
- `friend_percentage` (DecimalField, max_digits=5, decimal_places=2, default=0): Friend/Company percentage (report only)
- `my_own_percentage` (DecimalField, max_digits=5, decimal_places=2, default=0): Admin's own percentage (report only)

**Validation**:
```python
friend_percentage + my_own_percentage = client_exchange.my_percentage
```

**Key Methods**:
- `compute_friend_share()`: Calculate friend share from Client_PnL
- `compute_my_own_share()`: Calculate admin's own share from Client_PnL

**Business Rules**:
- Used ONLY for reports, NOT for system logic
- Supports decimal values (e.g., 9.5%, 0.5%)
- Must sum to `my_percentage`

---

### 6. Transaction

**Purpose**: TRANSACTIONS TABLE - AUDIT ONLY. Stores transaction history for audit purposes.

**Fields**:
- `client_exchange` (ForeignKey to ClientExchangeAccount)
- `date` (DateTimeField): Transaction date
- `type` (CharField, max_length=20): Transaction type (see Transaction Types section)
- `amount` (BigIntegerField): Amount (signed, for reporting only)
- `funding_before` (BigIntegerField, null=True): Funding before transaction (audit)
- `funding_after` (BigIntegerField, null=True): Funding after transaction (audit)
- `exchange_balance_before` (BigIntegerField, null=True): Exchange balance before transaction (audit)
- `exchange_balance_after` (BigIntegerField, null=True): Exchange balance after transaction (audit)
- `sequence_no` (IntegerField, default=0): Sequence number for ordering (auto-increment per account)
- `notes` (TextField, blank=True, null=True): Optional notes

**Transaction Types**:
- `FUNDING_MANUAL`: User adds capital
- `FUNDING_AUTO`: Optional re-funding after settlement
- `TRADE`: Exchange trading activity
- `SETTLEMENT_SHARE`: Share payment (profit/loss)
- `FEE`: Fee transaction
- `ADJUSTMENT`: Adjustment transaction
- `RECORD_PAYMENT`: Record Payment (legacy, used for reports)

**Business Rules**:
- NEVER used to recompute balances
- Balance mutations stored as before/after values for audit trail
- Each transaction represents exactly one financial intent
- Sequence number auto-increments per account

---

### 7. Settlement

**Purpose**: MASKED SHARE SETTLEMENT SYSTEM - Settlement Tracking.

**Fields**:
- `client_exchange` (ForeignKey to ClientExchangeAccount)
- `amount` (BigIntegerField, validators=[MinValueValidator(1)]): Settlement amount (integer, > 0)
- `date` (DateTimeField): Date when payment was made
- `notes` (TextField, blank=True, null=True): Optional notes
- `created_at`, `updated_at` (DateTimeField): Timestamps

**Business Rules**:
- Tracks individual settlement payments to prevent over-settlement
- Each settlement records a partial or full payment of admin's share
- Amount must be > 0
- Used to calculate remaining settlement amount

---

### 8. EmailOTP

**Purpose**: Stores OTP codes for email verification during signup.

**Fields**:
- `email` (EmailField, unique=True): Email address
- `username` (CharField, max_length=150): Username
- `otp_code` (CharField, max_length=6): 6-digit OTP code
- `is_verified` (BooleanField, default=False): Verification status
- `expires_at` (DateTimeField): OTP expiration time
- `created_at`, `updated_at` (DateTimeField): Timestamps

**Business Rules**:
- OTP expires after a set time period
- One OTP per email
- Used for email verification during signup

---

## Core Features

### 1. Client Management

**Views**:
- `client_list`: List all clients
- `client_create`: Create new client
- `client_detail`: View client details with all exchange accounts
- `client_delete`: Delete client

**Features**:
- Create clients with optional unique codes
- View all exchange accounts for a client
- Track client trading activity
- Delete clients (cascades to related data)

**URLs**:
- `/clients/` - List clients
- `/clients/create/` - Create client
- `/clients/<id>/` - Client detail
- `/clients/<id>/delete/` - Delete client

---

### 2. Exchange Management

**Views**:
- `exchange_list`: List all exchanges
- `exchange_create`: Create new exchange
- `exchange_detail`: View exchange details with all client accounts
- `exchange_delete`: Delete exchange
- `exchange_toggle_status`: Enable/disable exchange
- `link_client_to_exchange`: Link client to exchange (create ClientExchangeAccount)
- `exchange_account_detail`: View client-exchange account details
- `client_exchange_edit`: Edit client-exchange account (percentages, etc.)

**Features**:
- Create exchanges with optional version identifiers
- Enable/disable exchanges
- Link clients to exchanges
- Configure share percentages (loss and profit)
- View aggregated financial data per exchange
- Delete exchanges (cascades to related data)

**URLs**:
- `/exchanges/` - List exchanges
- `/exchanges/create/` - Create exchange
- `/exchanges/<id>/` - Exchange detail
- `/exchanges/<id>/delete/` - Delete exchange
- `/exchanges/<id>/toggle-status/` - Toggle exchange status
- `/exchanges/link/` - Link client to exchange
- `/exchanges/account/<id>/` - Account detail
- `/exchanges/account/<id>/edit/` - Edit account

---

### 3. Transaction Management

**Views**:
- `transaction_list`: List all transactions (with filters)
- `transaction_detail`: View transaction details
- `transaction_edit`: Edit transaction (audit trail only)

**Features**:
- View complete transaction history
- Filter by client, exchange, date range, type
- View before/after balance snapshots
- Edit transaction details (audit purposes only)

**URLs**:
- `/transactions/` - List transactions
- `/transactions/<id>/` - Transaction detail
- `/transactions/<id>/edit/` - Edit transaction

---

### 4. Funding & Balance Management

**Views**:
- `add_funding`: Add funding to client-exchange account
- `update_exchange_balance`: Update exchange balance (for trades, fees, etc.)
- `record_payment`: Record settlement payment

**Features**:
- Add manual funding (increases `funding`)
- Update exchange balance (changes `exchange_balance`)
- Record settlement payments (creates Settlement and Transaction records)
- Automatic cycle reset on funding changes

**URLs**:
- `/exchanges/account/<id>/funding/` - Add funding
- `/exchanges/account/<id>/update-balance/` - Update balance
- `/exchanges/account/<id>/record-payment/` - Record payment

---

### 5. Pending Payments

**Views**:
- `pending_summary`: View pending payments summary
- `export_pending_csv`: Export pending payments to CSV

**Features**:
- Two sections: "Clients Owe You" (losses) and "You Owe Clients" (profits)
- Shows remaining settlement amounts
- N.A. display for zero PnL or zero share cases
- Search and filter functionality
- CSV export with Period column

**URLs**:
- `/pending/` - Pending payments summary
- `/pending/export/` - Export CSV

---

### 6. Reports & Analytics

**Views**:
- `report_overview`: Reports overview page
- `report_daily`: Daily report for specific date
- `report_weekly`: Weekly report
- `report_monthly`: Monthly report
- `report_custom`: Custom date range report
- `report_client`: Client-specific report
- `report_exchange`: Exchange-specific report
- `report_time_travel`: Time-travel report (historical view)
- `export_report_csv`: Export report to CSV

**Features**:
- Turnover calculation (from TRADE transactions)
- Profit/loss calculation (from RECORD_PAYMENT transactions)
- Profit split between Company % and My Own %
- Charts and visualizations
- CSV export with Period and Report columns

**URLs**:
- `/reports/` - Reports overview
- `/reports/daily/` - Daily report
- `/reports/weekly/` - Weekly report
- `/reports/monthly/` - Monthly report
- `/reports/custom/` - Custom report
- `/reports/client/<id>/` - Client report
- `/reports/exchange/<id>/` - Exchange report
- `/reports/time-travel/` - Time-travel report
- `/reports/export/` - Export CSV

---

### 7. Dashboard

**Views**:
- `dashboard`: Main dashboard with key metrics

**Features**:
- Total turnover (from TRADE transactions)
- Total profit/loss
- Client and exchange filters
- Search functionality
- Quick access to key features

**URLs**:
- `/` - Dashboard

---

## Formulas and Calculations

### 1. Client PnL (Profit/Loss)

**Formula**:
```
Client_PnL = Exchange_Balance - Funding
```

**Interpretation**:
- `Client_PnL < 0`: Client is in LOSS → Client owes admin
- `Client_PnL > 0`: Client is in PROFIT → Admin owes client
- `Client_PnL = 0`: Neutral (no profit/loss)

**Implementation**:
```python
def compute_client_pnl(self):
    return self.exchange_balance - self.funding
```

**Returns**: BigInteger (can be negative for loss)

---

### 2. Share Percentage Selection

**Formula**:
```python
if client_pnl < 0:  # LOSS
    share_pct = loss_share_percentage if loss_share_percentage > 0 else my_percentage
elif client_pnl > 0:  # PROFIT
    share_pct = profit_share_percentage if profit_share_percentage > 0 else my_percentage
else:  # ZERO
    share_pct = 0
```

**Rules**:
- Loss case: Uses `loss_share_percentage` if set and > 0, else falls back to `my_percentage`
- Profit case: Uses `profit_share_percentage` if set and > 0, else falls back to `my_percentage`
- Zero case: Returns 0 (no share on zero PnL)

**Implementation**:
```python
def get_share_percentage(self, client_pnl=None):
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

---

### 3. Final Share (My Share) Calculation

**Formula**:
```
if Client_PnL == 0:
    Final_Share = 0
else:
    Share_Percentage = get_share_percentage(Client_PnL)
    Exact_Share = |Client_PnL| × (Share_Percentage / 100.0)
    Final_Share = floor(Exact_Share)  # Round DOWN
```

**Important Notes**:
- Uses **floor rounding** (round down) for final share
- Always returns a positive value (uses absolute value of PnL)
- Returns 0 if `Client_PnL = 0`

**Implementation**:
```python
def compute_my_share(self):
    import math
    client_pnl = self.compute_client_pnl()
    
    if client_pnl == 0:
        return 0
    
    share_pct = self.get_share_percentage(client_pnl)
    exact_share = abs(client_pnl) * (share_pct / 100.0)
    final_share = math.floor(exact_share)
    
    return int(final_share)
```

**Example**:
- Client_PnL = -999,508 (Loss)
- Share_Percentage = 15%
- Exact_Share = 999,508 × 0.15 = 149,926.2
- Final_Share = floor(149,926.2) = **149,926**

---

### 4. Remaining Settlement Amount

**Formula**:
```
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

**Important**:
- Uses **locked share**, not current calculated share
- Only counts settlements from **current cycle**
- Returns raw value (always ≥ 0)
- Sign is applied at display time based on PnL direction

**Implementation**:
```python
def get_remaining_settlement_amount(self):
    self.lock_initial_share_if_needed()
    
    if self.cycle_start_date:
        total_settled = self.settlements.filter(
            date__gte=self.cycle_start_date
        ).aggregate(total=models.Sum('amount'))['total'] or 0
    else:
        total_settled = self.settlements.aggregate(
            total=models.Sum('amount')
        )['total'] or 0
    
    if self.locked_initial_final_share is not None:
        initial_final_share = self.locked_initial_final_share
    else:
        # Lock current share if > 0
        current_share = self.compute_my_share()
        if current_share > 0:
            # Lock it now
            # ... locking logic ...
            initial_final_share = current_share
        else:
            return {'remaining': 0, 'overpaid': 0, 'initial_final_share': 0, 'total_settled': total_settled}
    
    remaining = max(0, initial_final_share - total_settled)
    overpaid = max(0, total_settled - initial_final_share)
    
    return {
        'remaining': remaining,
        'overpaid': overpaid,
        'initial_final_share': initial_final_share,
        'total_settled': total_settled
    }
```

---

### 5. Display Remaining Amount

**Formula**:
```python
if Client_PnL > 0:  # PROFIT - You owe client
    Display_Remaining = -Remaining_Raw  # Negative
else:  # LOSS - Client owes you
    Display_Remaining = +Remaining_Raw  # Positive
```

**Implementation**:
```python
def calculate_display_remaining(client_pnl, remaining_amount):
    if client_pnl > 0:
        return -remaining_amount  # You owe client (negative)
    else:
        return remaining_amount  # Client owes you (positive)
```

**UI Display**:
- Always shows absolute value (positive) in UI
- Sign interpretation handled by section (Clients Owe You vs You Owe Clients)

---

### 6. Turnover Calculation

**Formula**:
```
Turnover = Σ |Exchange_Balance_After - Exchange_Balance_Before|
```

**Where**:
- Only `TRADE` type transactions are included
- `Exchange_Balance_After` = Balance after the trade
- `Exchange_Balance_Before` = Balance before the trade
- Absolute value is used (always positive)

**Implementation**:
```python
trade_qs = transactions.filter(type='TRADE').exclude(
    exchange_balance_before__isnull=True
).exclude(
    exchange_balance_after__isnull=True
)

total_turnover = sum(
    abs(tx.exchange_balance_after - tx.exchange_balance_before)
    for tx in trade_qs
) or 0
```

**Important**: Turnover measures trading activity, NOT funding or settlements.

---

### 7. Profit/Loss Calculation (Reports)

**Formula**:
```
Your_Total_Profit = Σ(RECORD_PAYMENT.amount)
```

**Where**:
- Only `RECORD_PAYMENT` transactions are used
- Positive amounts = Client paid you (your profit)
- Negative amounts = You paid client (your loss)
- Sum of all payment amounts = Your total profit/loss

**Sign Convention**:
| Payment Direction | Meaning | Amount Sign | Effect on You |
|-------------------|---------|-------------|---------------|
| Client → You | Client loss settlement | **+X** | ✅ Your PROFIT |
| You → Client | Client profit settlement | **-X** | ❌ Your LOSS |

---

### 8. Profit Split Calculation (Company % vs My Own %)

**Formula**:
```
If Report_Config exists:
    My_Profit = Payment_Amount × (My_Own_% / My_Total_%)
    Company_Profit = Payment_Amount × (Company_% / My_Total_%)
    
If No Report_Config:
    My_Profit = Payment_Amount (100% goes to you)
    Company_Profit = 0
```

**Validation**:
```
Company_% + My_Own_% = My_Total_%
```

**Implementation**:
```python
for tx in payment_transactions:
    payment_amount = Decimal(str(tx.amount))
    account = tx.client_exchange
    my_total_pct = Decimal(str(account.my_percentage))
    
    if my_total_pct == 0:
        continue
    
    report_config = getattr(account, 'report_config', None)
    
    if report_config:
        my_own_pct = Decimal(str(report_config.my_own_percentage))
        friend_pct = Decimal(str(report_config.friend_percentage))
        
        # Weight by absolute payment amount
        weight = abs(payment_amount)
        total_weighted_my_own += weight * (my_own_pct / my_total_pct)
        total_weighted_friend += weight * (friend_pct / my_total_pct)
        total_weighted_amount += weight

# Split total profit using weighted average percentages
if total_weighted_amount > 0:
    weighted_my_own_ratio = total_weighted_my_own / total_weighted_amount
    weighted_friend_ratio = total_weighted_friend / total_weighted_amount
    
    my_profit_total = your_total_profit * weighted_my_own_ratio
    friend_profit_total = your_total_profit * weighted_friend_ratio
else:
    # No report configs found, all goes to me
    my_profit_total = your_total_profit
    friend_profit_total = Decimal(0)
```

**Example**:
- Configuration: My Total % = 10%, Company % = 9.5%, My Own % = 0.5%
- Payment: +₹10,000 (Client paid you)
- My Profit = ₹10,000 × (0.5 / 10) = ₹500
- Company Profit = ₹10,000 × (9.5 / 10) = ₹9,500
- Verification: ₹500 + ₹9,500 = ₹10,000 ✓

---

### 9. Masked Capital Calculation

**Formula**:
```
Masked_Capital = (Share_Payment × |Locked_Initial_PnL|) / Locked_Initial_Final_Share
```

**Purpose**: Maps share payment back to PnL linearly, not exponentially.

**Implementation**:
```python
def compute_masked_capital(self, share_payment):
    settlement_info = self.get_remaining_settlement_amount()
    initial_final_share = settlement_info['initial_final_share']
    locked_initial_pnl = self.locked_initial_pnl
    
    if initial_final_share == 0 or locked_initial_pnl is None:
        return 0
    
    return int((share_payment * abs(locked_initial_pnl)) / initial_final_share)
```

---

## Business Logic

### 1. Share Locking System

**Purpose**: Prevent share from shrinking after payments. Share is decided by trading outcome, not by settlement.

**When Share is Locked**:
1. **First Calculation**: When `locked_initial_final_share` is None and `compute_my_share() > 0`
2. **PnL Cycle Change**: When PnL sign flips (LOSS → PROFIT or PROFIT → LOSS)
3. **Funding Change**: When funding amount changes (new exposure = new cycle)
4. **PnL Magnitude Reduction**: When PnL magnitude reduces significantly (trading reduced exposure)

**Lock Process**:
```python
def lock_initial_share_if_needed(self):
    client_pnl = self.compute_client_pnl()
    
    # Check for cycle reset conditions
    if funding_changed or pnl_magnitude_reduced:
        reset_cycle()
    
    # Check for PnL sign flip
    if locked_initial_pnl exists and sign_flipped:
        lock_new_share()
    
    # First time lock
    if locked_initial_final_share is None:
        final_share = self.compute_my_share()
        if final_share > 0:
            self.locked_initial_final_share = final_share
            self.locked_share_percentage = self.get_share_percentage(client_pnl)
            self.locked_initial_pnl = client_pnl
            self.cycle_start_date = timezone.now()
            self.locked_initial_funding = self.funding
```

**Key Points**:
- Share is locked at first calculation
- Share NEVER shrinks after payments
- Lock resets only when cycle changes

---

### 2. Cycle Management

**PnL Cycle**: A period where PnL maintains the same sign (all profit or all loss).

**Cycle Reset Conditions**:
1. **Sign Flip**: PnL changes from profit to loss or vice versa
2. **Funding Change**: New funding added (new exposure)
3. **PnL Magnitude Reduction**: Significant reduction in PnL magnitude (trading reduced exposure)
4. **Zero PnL with Full Settlement**: PnL becomes zero and all settlements are complete

**Cycle Tracking**:
- `cycle_start_date`: Timestamp when current cycle started
- Settlements are filtered by `cycle_start_date` to only count current cycle payments
- Old cycle settlements don't mix with new cycle shares

**Example**:
- Cycle started: 2026-01-01
- Settlement 1: 2025-12-15 (old cycle) → **NOT counted**
- Settlement 2: 2026-01-10 (current cycle) → **Counted**
- Settlement 3: 2026-01-20 (current cycle) → **Counted**

---

### 3. N.A. Display Logic

**When to Show N.A**:
1. **Zero PnL**: `Client_PnL = 0` (neutral case)
2. **Zero Final Share**: `Final_Share = 0` (even if PnL ≠ 0)

**Display Rules**:
- **PROFIT(+)/LOSS(-)**: Show "N.A" if `Client_PnL = 0` or `show_na = True`
- **MY SHARE**: Show "N.A" if `Final_Share = 0` or `show_na = True`
- **MY%**: Show actual percentage even when PnL = 0 (use `my_percentage` directly, not `get_share_percentage()`)

**Example**:
- Client_PnL = 0
- Funding = 0
- Exchange Balance = 0
- MY% = 12.00 (still shows 12.00%, not 0%)
- PROFIT(+)/LOSS(-) = "N.A"
- MY SHARE = "N.A"

---

### 4. Section Classification (Pending Payments)

**Clients Owe You Section**:
- Condition: `Client_PnL < 0` (Loss case)
- Interpretation: Client owes admin money
- Display Remaining: Positive value
- Includes: Zero PnL cases (shown as N.A)

**You Owe Clients Section**:
- Condition: `Client_PnL > 0` (Profit case)
- Interpretation: Admin owes client money
- Display Remaining: Negative value (shown as positive in UI)
- Includes: Zero Final Share cases (shown as N.A)

---

## Transaction Types

### 1. FUNDING_MANUAL

**Purpose**: User adds capital to client-exchange account.

**Effect**:
- Increases `funding` field
- Creates Transaction record with type `FUNDING_MANUAL`
- May reset PnL cycle (new exposure = new cycle)

**Implementation**:
```python
# Add funding
account.funding += amount
account.save()

# Create transaction
Transaction.objects.create(
    client_exchange=account,
    date=timezone.now(),
    type='FUNDING_MANUAL',
    amount=amount,
    funding_before=old_funding,
    funding_after=account.funding,
    exchange_balance_before=account.exchange_balance,
    exchange_balance_after=account.exchange_balance
)
```

---

### 2. FUNDING_AUTO

**Purpose**: Optional re-funding after settlement.

**Effect**:
- Increases `funding` field
- Creates Transaction record with type `FUNDING_AUTO`
- Resets PnL cycle (new exposure = new cycle)

**Note**: Currently not actively used, but available for future auto re-funding features.

---

### 3. TRADE

**Purpose**: Exchange trading activity.

**Effect**:
- Updates `exchange_balance` field
- Creates Transaction record with type `TRADE`
- Records before/after exchange balance for turnover calculation
- Does NOT affect funding

**Implementation**:
```python
# Update exchange balance
old_balance = account.exchange_balance
account.exchange_balance = new_balance
account.save()

# Create transaction
Transaction.objects.create(
    client_exchange=account,
    date=timezone.now(),
    type='TRADE',
    amount=0,  # Not used for trades
    funding_before=account.funding,
    funding_after=account.funding,
    exchange_balance_before=old_balance,
    exchange_balance_after=new_balance
)
```

**Turnover Calculation**:
- Turnover = |exchange_balance_after - exchange_balance_before|
- Only TRADE transactions contribute to turnover

---

### 4. SETTLEMENT_SHARE

**Purpose**: Share payment (profit/loss settlement).

**Effect**:
- Creates Settlement record
- Creates Transaction record with type `SETTLEMENT_SHARE`
- Updates balances based on PnL direction:
  - If `Client_PnL < 0` (LOSS): `funding = funding - MaskedCapital`
  - If `Client_PnL > 0` (PROFIT): `exchange_balance = exchange_balance - MaskedCapital`
- Uses locked share for validation

**Implementation**:
```python
# Lock share if needed
account.lock_initial_share_if_needed()

# Get remaining settlement amount
settlement_info = account.get_remaining_settlement_amount()
remaining_amount = settlement_info['remaining']

# Validate payment amount
if share_payment > remaining_amount:
    raise ValidationError("Payment exceeds remaining amount")

# Calculate masked capital
masked_capital = account.compute_masked_capital(share_payment)

# Update balances based on PnL direction
client_pnl = account.compute_client_pnl()
if client_pnl < 0:
    # LOSS: Reduce funding
    account.funding = max(0, account.funding - masked_capital)
else:
    # PROFIT: Reduce exchange balance
    account.exchange_balance = max(0, account.exchange_balance - masked_capital)

account.save()

# Create settlement
Settlement.objects.create(
    client_exchange=account,
    amount=share_payment,
    date=payment_date,
    notes=notes
)

# Create transaction
Transaction.objects.create(
    client_exchange=account,
    date=payment_date,
    type='SETTLEMENT_SHARE',
    amount=share_payment,  # Signed based on PnL direction
    funding_before=old_funding,
    funding_after=account.funding,
    exchange_balance_before=old_balance,
    exchange_balance_after=account.exchange_balance
)
```

---

### 5. RECORD_PAYMENT

**Purpose**: Record Payment (legacy type, used for reports).

**Effect**:
- Creates Transaction record with type `RECORD_PAYMENT`
- Used for profit/loss calculation in reports
- Does NOT update balances (balances are updated separately)
- Sign convention:
  - `+X` = Client paid you (Client loss) → Your PROFIT
  - `-X` = You paid client (Client profit) → Your LOSS

**Note**: This is a legacy transaction type. New settlements should use `SETTLEMENT_SHARE`.

---

### 6. FEE

**Purpose**: Fee transaction.

**Effect**:
- Updates `exchange_balance` (reduces balance)
- Creates Transaction record with type `FEE`
- Does NOT affect funding

---

### 7. ADJUSTMENT

**Purpose**: Adjustment transaction.

**Effect**:
- Updates balances as needed
- Creates Transaction record with type `ADJUSTMENT`
- Used for manual corrections

---

## Settlement System

### Overview

The **Masked Share Settlement System** ensures that:
1. Share amounts are locked at first calculation
2. Share never shrinks after payments
3. Settlements are tracked per cycle
4. Over-settlement is prevented

### Settlement Process

**Step 1: Lock Share**
```python
account.lock_initial_share_if_needed()
```

**Step 2: Get Remaining Amount**
```python
settlement_info = account.get_remaining_settlement_amount()
remaining_amount = settlement_info['remaining']
initial_final_share = settlement_info['initial_final_share']
```

**Step 3: Validate Payment**
```python
if share_payment > remaining_amount:
    raise ValidationError("Payment exceeds remaining amount")
```

**Step 4: Calculate Masked Capital**
```python
masked_capital = account.compute_masked_capital(share_payment)
```

**Step 5: Update Balances**
```python
client_pnl = account.compute_client_pnl()
if client_pnl < 0:
    # LOSS: Reduce funding
    account.funding = max(0, account.funding - masked_capital)
else:
    # PROFIT: Reduce exchange balance
    account.exchange_balance = max(0, account.exchange_balance - masked_capital)
```

**Step 6: Create Records**
```python
# Create settlement
Settlement.objects.create(
    client_exchange=account,
    amount=share_payment,
    date=payment_date,
    notes=notes
)

# Create transaction
Transaction.objects.create(
    client_exchange=account,
    date=payment_date,
    type='SETTLEMENT_SHARE',
    amount=share_payment,
    # ... balance snapshots ...
)
```

### Settlement Validation

**Rules**:
1. Payment amount must be > 0
2. Payment cannot exceed remaining amount
3. Final share must be > 0 (cannot settle if share is 0)
4. Balances cannot go negative

**Error Messages**:
- "Final share is 0. Cannot record payment."
- "Payment amount exceeds remaining amount."
- "Invalid payment amount."

---

## Pending Payments System

### Overview

The Pending Payments system tracks outstanding financial obligations between admin and clients based on trading performance.

### Data Structure

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

### Calculation Process

**For Each Client Exchange**:
1. Calculate `Client_PnL` using `compute_client_pnl()`
2. Determine section (loss/profit/neutral)
3. Lock share if needed using `lock_initial_share_if_needed()`
4. Get remaining settlement using `get_remaining_settlement_amount()`
5. Calculate display remaining using `calculate_display_remaining()`
6. Add to appropriate list (clients_owe_list or you_owe_list)

**Totals Calculation**:
```python
total_clients_owe = sum(item["amount_owed"] for item in clients_owe_list)
total_my_share_clients_owe = sum(item["remaining_amount"] for item in clients_owe_list)
total_you_owe = sum(item["amount_owed"] for item in you_owe_list)
total_my_share_you_owe = sum(item["remaining_amount"] for item in you_owe_list)
```

### CSV Export

**Format**:
```csv
Period,U_CODE,Master,OPENING POINTS,AVL.POINTS(CLOSING POINTS),PROFIT(+)/LOSS(-),MY SHARE,MY%
2026-01-12,VIJ77&EXC,VIJEXCHV1,10000000,9000492,-999508,149926,15.00
,VIJ77&EXC,VIJETHA77 V2,0,0,N.A,N.A,12.00
```

**Columns**:
1. **Period**: Date (only in first row, empty in subsequent rows)
2. **U_CODE**: Client code
3. **Master**: Exchange name
4. **OPENING POINTS**: Funding amount
5. **AVL.POINTS(CLOSING POINTS)**: Exchange balance
6. **PROFIT(+)/LOSS(-)**: Client PnL (or "N.A")
7. **MY SHARE**: Final share amount (or "N.A")
8. **MY%**: Share percentage

---

## Reports System

### Report Types

1. **Daily Report**: Report for a specific date
2. **Weekly Report**: Report for a week
3. **Monthly Report**: Report for a month
4. **Custom Report**: Report for custom date range
5. **Client Report**: Report for specific client
6. **Exchange Report**: Report for specific exchange
7. **Time-Travel Report**: Historical view at a specific point in time

### Report Calculations

**Turnover**:
```python
trade_qs = transactions.filter(type='TRADE').exclude(
    exchange_balance_before__isnull=True
).exclude(
    exchange_balance_after__isnull=True
)

total_turnover = sum(
    abs(tx.exchange_balance_after - tx.exchange_balance_before)
    for tx in trade_qs
) or 0
```

**Profit/Loss**:
```python
payment_qs = transactions.filter(type='RECORD_PAYMENT')
your_total_profit = payment_qs.aggregate(total=Sum("amount"))["total"] or Decimal(0)
your_profit = payment_qs.filter(amount__gt=0).aggregate(total=Sum("amount"))["total"] or Decimal(0)
your_loss = abs(payment_qs.filter(amount__lt=0).aggregate(total=Sum("amount"))["total"] or Decimal(0))
```

**Profit Split**:
```python
# Calculate weighted average percentages
for tx in payment_transactions:
    payment_amount = Decimal(str(tx.amount))
    account = tx.client_exchange
    my_total_pct = Decimal(str(account.my_percentage))
    
    if my_total_pct == 0:
        continue
    
    report_config = getattr(account, 'report_config', None)
    
    if report_config:
        my_own_pct = Decimal(str(report_config.my_own_percentage))
        friend_pct = Decimal(str(report_config.friend_percentage))
        
        weight = abs(payment_amount)
        total_weighted_my_own += weight * (my_own_pct / my_total_pct)
        total_weighted_friend += weight * (friend_pct / my_total_pct)
        total_weighted_amount += weight

# Split total profit
if total_weighted_amount > 0:
    weighted_my_own_ratio = total_weighted_my_own / total_weighted_amount
    weighted_friend_ratio = total_weighted_friend / total_weighted_amount
    
    my_profit_total = your_total_profit * weighted_my_own_ratio
    friend_profit_total = your_total_profit * weighted_friend_ratio
else:
    my_profit_total = your_total_profit
    friend_profit_total = Decimal(0)
```

### CSV Export

**Format**:
```csv
Period,Report,Exchange,Type,Amount,Exchange Balance After,Note
2026-01-01 to 2026-01-31,2026-01-15,Exchange 1,Funding,10000,10000,Initial funding
,2026-01-16,Exchange 1,Trade,5000,15000,Trade transaction
```

**Columns**:
1. **Period**: Date range (only in first row, empty in subsequent rows)
2. **Report**: Transaction date
3. **Exchange**: Exchange name
4. **Type**: Transaction type
5. **Amount**: Transaction amount
6. **Exchange Balance After**: Balance after transaction
7. **Note**: Transaction notes

---

## Dashboard

### Metrics Displayed

1. **Total Turnover**: Sum of absolute exchange balance movements for TRADE transactions
2. **Total Profit/Loss**: Sum of RECORD_PAYMENT amounts
3. **My Profit**: Split from total profit using Company % and My Own %
4. **Company Profit**: Split from total profit using Company % and My Own %

### Filters

- **Client Filter**: Filter by specific client
- **Exchange Filter**: Filter by specific exchange
- **Search**: Search by client name, code, or exchange name
- **Client Type Filter**: Filter by client type (stored in session)

---

## Authentication System

### Signup Process

1. **User Registration**:
   - Username (4-30 characters, any characters allowed)
   - Email address
   - Password

2. **Email OTP Verification**:
   - OTP code sent to email
   - User enters OTP to verify
   - OTP expires after set time period
   - Resend OTP option available

3. **Login**:
   - Username/email and password
   - Session-based authentication
   - Session expires at browser close

### Security Features

- **Rate Limiting**: Login rate limiting (5 requests per 5 minutes)
- **CSRF Protection**: Django CSRF protection enabled
- **Password Validation**: Django password validators
- **Session Security**: Secure session cookies
- **Security Headers**: Custom security headers middleware

---

## API/URL Structure

### Authentication URLs
- `/login/` - Login page
- `/logout/` - Logout
- `/signup/` - Signup page
- `/verify-otp/` - Verify OTP
- `/resend-otp/` - Resend OTP

### Client URLs
- `/clients/` - List clients
- `/clients/create/` - Create client
- `/clients/<id>/` - Client detail
- `/clients/<id>/delete/` - Delete client

### Exchange URLs
- `/exchanges/` - List exchanges
- `/exchanges/create/` - Create exchange
- `/exchanges/<id>/` - Exchange detail
- `/exchanges/<id>/delete/` - Delete exchange
- `/exchanges/<id>/toggle-status/` - Toggle exchange status
- `/exchanges/link/` - Link client to exchange
- `/exchanges/account/<id>/` - Account detail
- `/exchanges/account/<id>/edit/` - Edit account

### Transaction URLs
- `/exchanges/account/<id>/funding/` - Add funding
- `/exchanges/account/<id>/update-balance/` - Update balance
- `/exchanges/account/<id>/record-payment/` - Record payment
- `/transactions/` - List transactions
- `/transactions/<id>/` - Transaction detail
- `/transactions/<id>/edit/` - Edit transaction

### Pending Payments URLs
- `/pending/` - Pending payments summary
- `/pending/export/` - Export CSV

### Reports URLs
- `/reports/` - Reports overview
- `/reports/daily/` - Daily report
- `/reports/weekly/` - Weekly report
- `/reports/monthly/` - Monthly report
- `/reports/custom/` - Custom report
- `/reports/client/<id>/` - Client report
- `/reports/exchange/<id>/` - Exchange report
- `/reports/time-travel/` - Time-travel report
- `/reports/export/` - Export CSV

### Dashboard URL
- `/` - Dashboard

---

## Implementation Details

### Decimal Precision

**Storage**:
- All percentages stored as `Decimal` with 2 decimal places
- Example: 9.5% stored as `9.50`

**Calculation**:
- All calculations use `Decimal` type for precision
- No rounding errors
- Exact decimal arithmetic

**Example**:
```python
from decimal import Decimal

my_own_pct = Decimal(str(report_config.my_own_percentage))  # e.g., 0.50
friend_pct = Decimal(str(report_config.friend_percentage))  # e.g., 9.50
my_total_pct = Decimal(str(account.my_percentage))         # e.g., 10.00

my_profit = payment_amount * (my_own_pct / my_total_pct)
```

### Floor Rounding

**Purpose**: Ensure admin never gets more than calculated share.

**Implementation**:
```python
import math

exact_share = abs(client_pnl) * (share_pct / 100.0)
final_share = math.floor(exact_share)  # Round DOWN
```

**Example**:
- Exact_Share = 149,926.9
- Final_Share = floor(149,926.9) = 149,926 (not 149,927)

### Database Transactions

**Usage**: Critical operations use database transactions to ensure atomicity.

**Example**:
```python
from django.db import transaction as db_transaction

with db_transaction.atomic():
    # Lock share
    account.lock_initial_share_if_needed()
    
    # Validate payment
    if share_payment > remaining_amount:
        raise ValidationError("Payment exceeds remaining amount")
    
    # Update balances
    account.funding -= masked_capital
    account.save()
    
    # Create settlement
    Settlement.objects.create(...)
    
    # Create transaction
    Transaction.objects.create(...)
```

### Error Handling

**Validation Errors**:
- Form validation errors displayed to user
- Database constraint violations caught and displayed
- Custom error messages for business logic violations

**Example**:
```python
try:
    account.save()
except ValidationError as e:
    messages.error(request, str(e))
except Exception as e:
    messages.error(request, f"Error: {str(e)}")
```

---

## Data Flow

### Adding Funding

```
User Input → add_funding view
    ↓
Validate amount > 0
    ↓
Update account.funding
    ↓
Create Transaction (FUNDING_MANUAL)
    ↓
Redirect to account detail
```

### Recording Payment

```
User Input → record_payment view
    ↓
Lock share if needed
    ↓
Get remaining settlement amount
    ↓
Validate payment <= remaining
    ↓
Calculate masked capital
    ↓
Update balances (based on PnL direction)
    ↓
Create Settlement record
    ↓
Create Transaction (SETTLEMENT_SHARE)
    ↓
Redirect to account detail
```

### Calculating Pending Payments

```
Get all client exchanges
    ↓
For each exchange:
    Calculate Client_PnL
    ↓
    Determine section (loss/profit/neutral)
    ↓
    Lock share if needed
    ↓
    Get remaining settlement amount
    ↓
    Calculate display remaining
    ↓
    Add to appropriate list
    ↓
Sort lists by amount
    ↓
Calculate totals
    ↓
Render template
```

### Generating Report

```
Get date range
    ↓
Filter transactions by date range
    ↓
Calculate turnover (from TRADE transactions)
    ↓
Calculate profit/loss (from RECORD_PAYMENT transactions)
    ↓
Split profit (using Company % and My Own %)
    ↓
Aggregate by client/exchange
    ↓
Generate charts
    ↓
Render template
```

---

## Error Handling

### Validation Errors

**Client Code Uniqueness**:
```python
if existing.exists():
    raise ValidationError(
        f"Client code '{self.code}' is already in use by client '{existing_client.name}'."
    )
```

**Loss Share Percentage Immutability**:
```python
if old_instance.loss_share_percentage != self.loss_share_percentage:
    if has_transactions or has_settlements:
        raise ValidationError(
            "Loss share percentage cannot be changed after data exists."
        )
```

**Report Config Validation**:
```python
if abs(friend_plus_own - my_total) >= epsilon:
    raise ValidationError(
        f"Friend % ({self.friend_percentage}) + My Own % ({self.my_own_percentage}) "
        f"must equal My Total % ({my_total})"
    )
```

### Business Logic Errors

**Settlement Validation**:
```python
if final_share == 0:
    raise ValidationError("Final share is 0. Cannot record payment.")

if share_payment > remaining_amount:
    raise ValidationError("Payment amount exceeds remaining amount.")
```

**Balance Validation**:
```python
if new_balance < 0:
    raise ValidationError("Balance cannot be negative.")
```

---

## Security Features

### Authentication Security

- **Password Hashing**: Django's PBKDF2 password hashing
- **Session Security**: Secure session cookies
- **CSRF Protection**: Django CSRF middleware
- **Login Rate Limiting**: 5 requests per 5 minutes

### Data Security

- **User Isolation**: Users can only access their own data
- **SQL Injection Prevention**: Django ORM prevents SQL injection
- **XSS Protection**: Django template auto-escaping
- **Input Validation**: Form validation and model validation

### Security Headers

Custom middleware adds security headers:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Referrer-Policy: strict-origin-when-cross-origin`

---

## Summary

### Key Formulas

1. **Client_PnL = Exchange_Balance - Funding**
2. **Final_Share = floor(|Client_PnL| × Share_Percentage / 100)**
3. **Remaining_Raw = max(0, Locked_Initial_Final_Share - Total_Settled)**
4. **Display_Remaining = sign(Client_PnL) × Remaining_Raw**
5. **Turnover = Σ |Exchange_Balance_After - Exchange_Balance_Before| (TRADE only)**
6. **Your_Total_Profit = Σ(RECORD_PAYMENT.amount)**
7. **My_Profit = Payment × (My_Own_% / My_Total_%)**
8. **Company_Profit = Payment × (Company_% / My_Total_%)**

### Key Principles

1. **PIN-TO-PIN**: Only two money values stored (`funding` and `exchange_balance`)
2. **Masked Share Settlement**: Share locked at first calculation, never shrinks
3. **Cycle-Based Tracking**: Separate cycles for profit and loss periods
4. **Audit Trail**: All transactions recorded with before/after snapshots
5. **Decimal Precision**: All calculations use Decimal type
6. **Floor Rounding**: Final share always rounded down

---

**Document Version**: 1.0  
**Last Updated**: January 12, 2026  
**System**: Transaction Hub - Money Flow Control  
**Framework**: Django 4.2.27  
**Database**: PostgreSQL


