# DASHBOARD - COMPLETE DOCUMENTATION

**Version:** 1.0 (Complete & Comprehensive)  
**Status:** Production Ready  
**Last Updated:** January 2026

---

## TABLE OF CONTENTS

1. [System Overview](#system-overview)
2. [Database Schema](#database-schema)
3. [UI Components](#ui-components)
4. [View Logic](#view-logic)
5. [Metrics & Calculations](#metrics--calculations)
6. [Filters & Search](#filters--search)
7. [Data Flow](#data-flow)
8. [Code Reference](#code-reference)
9. [Edge Cases & Scenarios](#edge-cases--scenarios)
10. [Testing Guide](#testing-guide)

---

## SYSTEM OVERVIEW

The **Dashboard** is the main landing page that provides a live overview of key business metrics, client accounts, and financial summaries. It serves as the central hub for monitoring:

- Total clients, exchanges, and accounts
- Financial metrics (funding, balances, PnL, shares)
- Recent account activity
- Transaction turnover

### Key Features

- ✅ **Real-time metrics** calculated from database
- ✅ **Summary cards** displaying key totals
- ✅ **Recent accounts table** showing latest activity
- ✅ **User-specific data** (only shows data for logged-in user)
- ✅ **Computed values** (PnL, shares calculated on-the-fly)
- ✅ **Responsive design** with card grid layout

### Access Control

- **Authentication Required:** `@login_required` decorator
- **User Filtering:** Only shows clients assigned to logged-in user
- **URL:** `/` (root URL)

---

## DATABASE SCHEMA

### Models Used

The dashboard queries and aggregates data from multiple models:

#### 1. Client Model

**Table:** `core_client`

**Fields Used:**
- `id`: Primary key
- `name`: Client name
- `code`: Client code (optional)
- `user`: Foreign key to User (for filtering)

**Query:**
```python
Client.objects.filter(user=request.user).count()
```

---

#### 2. Exchange Model

**Table:** `core_exchange`

**Fields Used:**
- `id`: Primary key
- `name`: Exchange name

**Query:**
```python
Exchange.objects.count()
```

---

#### 3. ClientExchangeAccount Model

**Table:** `core_clientexchangeaccount`

**Fields Used:**
- `id`: Primary key
- `client`: Foreign key to Client
- `exchange`: Foreign key to Exchange
- `funding`: Total funding (BIGINT)
- `exchange_balance`: Current balance (BIGINT)
- `updated_at`: Last update timestamp

**Methods Used:**
- `compute_client_pnl()`: Calculates Client PnL
- `compute_my_share()`: Calculates admin's share

**Query:**
```python
ClientExchangeAccount.objects.filter(client__user=request.user)
    .select_related("client", "exchange")
```

---

#### 4. Transaction Model

**Table:** `core_transaction`

**Fields Used:**
- `id`: Primary key
- `client_exchange`: Foreign key to ClientExchangeAccount
- `amount`: Transaction amount
- `date`: Transaction date

**Query:**
```python
Transaction.objects.select_related("client_exchange", "client_exchange__client", "client_exchange__exchange")
    .filter(client_exchange__client__user=request.user)
    .aggregate(total=Sum("amount"))
```

---

### Database Relationships

```
User
  └── Client (user FK)
       └── ClientExchangeAccount (client FK)
            ├── Exchange (exchange FK)
            └── Transaction (client_exchange FK)
```

---

## UI COMPONENTS

### Template Structure

**File:** `core/templates/core/dashboard.html`

**Layout:**
1. **Header:** Title and subtitle
2. **Card Grid:** 7 summary cards
3. **Recent Accounts Table:** Last 10 updated accounts

---

### Component 1: Summary Cards

**Container:** `<div class="card-grid">`

**Cards:**

#### Card 1: Total Clients
```html
<div class="card">
    <div class="card-title">Total Clients</div>
    <div class="card-value">{{ total_clients }}</div>
</div>
```

**Data:** Count of clients assigned to logged-in user

---

#### Card 2: Total Exchanges
```html
<div class="card">
    <div class="card-title">Total Exchanges</div>
    <div class="card-value">{{ total_exchanges }}</div>
</div>
```

**Data:** Count of all exchanges in system

---

#### Card 3: Total Accounts
```html
<div class="card">
    <div class="card-title">Total Accounts</div>
    <div class="card-value">{{ total_accounts }}</div>
</div>
```

**Data:** Count of client-exchange accounts for logged-in user

---

#### Card 4: Total Funding
```html
<div class="card">
    <div class="card-title">Total Funding</div>
    <div class="card-value">{{ total_funding|floatformat:0 }}</div>
</div>
```

**Data:** Sum of all funding amounts (formatted as integer)

---

#### Card 5: Total Exchange Balance
```html
<div class="card">
    <div class="card-title">Total Exchange Balance</div>
    <div class="card-value">{{ total_exchange_balance|floatformat:0 }}</div>
</div>
```

**Data:** Sum of all exchange balances (formatted as integer)

---

#### Card 6: Total Client PnL
```html
<div class="card">
    <div class="card-title">Total Client PnL</div>
    <div class="card-value {% if total_client_pnl > 0 %}positive{% elif total_client_pnl < 0 %}negative{% endif %}">
        {{ total_client_pnl|floatformat:0 }}
    </div>
</div>
```

**Data:** Sum of all client PnL values
**Styling:** Green if positive, red if negative

---

#### Card 7: Total My Share
```html
<div class="card">
    <div class="card-title">Total My Share</div>
    <div class="card-value">{{ total_my_share|floatformat:0 }}</div>
</div>
```

**Data:** Sum of all admin shares (formatted as integer)

---

### Component 2: Recent Accounts Table

**Container:** Conditional display (only if `recent_accounts` exists)

**Structure:**
```html
<div style="background: var(--bg-content); border-radius: 8px; padding: 24px; border: 1px solid var(--border); margin-top: 24px;">
    <h3>Recent Accounts</h3>
    <div class="table-wrapper">
        <table>
            <!-- Table content -->
        </table>
    </div>
</div>
```

**Columns:**

1. **Client:** `account.client.name`
2. **Exchange:** `account.exchange.name`
3. **Funding:** `account.funding|floatformat:0`
4. **Balance:** `account.exchange_balance|floatformat:0`
5. **PnL:** Computed with conditional styling
6. **My Share:** Computed with conditional display
7. **Actions:** "View" button linking to account detail

**PnL Display Logic:**
```django
{% if pnl == 0 %}
    <span style="color: var(--muted);">N.A</span>
{% else %}
    <span class="{% if pnl > 0 %}positive{% else %}negative{% endif %}">
        {{ pnl|floatformat:0 }}
    </span>
{% endif %}
```

**My Share Display Logic:**
```django
{% if pnl == 0 %}
    <span style="color: var(--muted);">N.A</span>
{% else %}
    {{ share|floatformat:0 }}
{% endif %}
```

---

## VIEW LOGIC

### Function: dashboard

**Location:** `core/views.py` (lines 204-388)

**Decorator:** `@login_required`

**Method:** GET

**Flow:**

```
1. User accesses root URL (/)
2. Check authentication (@login_required)
3. Extract filters from GET parameters
4. Query transactions (with filters)
5. Calculate transaction totals
6. Query all accounts for user
7. Calculate financial metrics
8. Count entities (clients, exchanges, accounts)
9. Get recent accounts
10. Build context dictionary
11. Render template
```

---

### Step-by-Step Processing

#### Step 1: Initialize Date

```python
today = date.today()
```

**Purpose:** Current date for date-based calculations

---

#### Step 2: Extract Filters

```python
client_id = request.GET.get("client")
exchange_id = request.GET.get("exchange")
search_query = request.GET.get("search", "")
client_type_filter = request.GET.get("client_type") or request.session.get('client_type_filter', 'all')
```

**Filters:**
- `client_id`: Filter by specific client
- `exchange_id`: Filter by specific exchange
- `search_query`: Search by client/exchange name/code
- `client_type_filter`: Filter by client type (from session or GET)

---

#### Step 3: Query Transactions

```python
transactions_qs = Transaction.objects.select_related(
    "client_exchange", 
    "client_exchange__client", 
    "client_exchange__exchange"
).filter(client_exchange__client__user=request.user)
```

**Base Query:** All transactions for logged-in user's clients

**Apply Filters:**

```python
if client_id:
    transactions_qs = transactions_qs.filter(client_exchange__client_id=client_id)

if exchange_id:
    transactions_qs = transactions_qs.filter(client_exchange__exchange_id=exchange_id)

if search_query:
    transactions_qs = transactions_qs.filter(
        Q(client_exchange__client__name__icontains=search_query) |
        Q(client_exchange__client__code__icontains=search_query) |
        Q(client_exchange__exchange__name__icontains=search_query)
    )
```

---

#### Step 4: Calculate Transaction Totals

```python
total_turnover = transactions_qs.aggregate(total=Sum("amount"))["total"] or 0
```

**Result:** Sum of all transaction amounts (or 0 if none)

---

#### Step 5: Query All Accounts

```python
all_accounts = ClientExchangeAccount.objects.filter(
    client__user=request.user
).select_related("client", "exchange")
```

**Query:** All client-exchange accounts for logged-in user

**Optimization:** `select_related()` prevents N+1 queries

---

#### Step 6: Calculate Financial Metrics

```python
total_funding = sum(account.funding for account in all_accounts)
total_exchange_balance = sum(account.exchange_balance for account in all_accounts)
total_client_pnl = sum(account.compute_client_pnl() for account in all_accounts)
total_my_share = sum(account.compute_my_share() for account in all_accounts)
```

**Calculations:**
- **Total Funding:** Sum of all `funding` values
- **Total Exchange Balance:** Sum of all `exchange_balance` values
- **Total Client PnL:** Sum of computed PnL for each account
- **Total My Share:** Sum of computed shares for each account

---

#### Step 7: Count Entities

```python
total_clients = Client.objects.filter(user=request.user).count()
total_exchanges = Exchange.objects.count()
total_accounts = all_accounts.count()
```

**Counts:**
- **Total Clients:** Clients assigned to logged-in user
- **Total Exchanges:** All exchanges (system-wide)
- **Total Accounts:** Client-exchange accounts for user

---

#### Step 8: Get Recent Accounts

```python
recent_accounts = all_accounts.order_by("-updated_at")[:10]
```

**Query:** Last 10 updated accounts (most recent first)

**Limit:** 10 accounts

---

#### Step 9: Build Context

```python
context = {
    "today": today,
    "total_clients": total_clients,
    "total_exchanges": total_exchanges,
    "total_accounts": total_accounts,
    "total_funding": total_funding,
    "total_exchange_balance": total_exchange_balance,
    "total_client_pnl": total_client_pnl,
    "total_my_share": total_my_share,
    "recent_accounts": recent_accounts,
    "total_turnover": total_turnover,
    "your_profit": your_profit,
    "company_profit": company_profit,
    "pending_clients_owe": pending_clients_owe,
    "pending_you_owe_clients": pending_you_owe_clients,
    "active_clients_count": active_clients_count,
    "total_exchanges_count": Exchange.objects.count(),
    "recent_transactions": transactions_qs[:10],
    "all_clients": clients_qs.order_by("name"),
    "all_exchanges": Exchange.objects.all().order_by("name"),
    "selected_client": int(client_id) if client_id else None,
    "selected_exchange": int(exchange_id) if exchange_id else None,
    "search_query": search_query,
    "client_type_filter": client_type_filter,
    "current_balance": current_balance,
    "has_transactions": has_transactions,
}
```

**Context Variables:**
- Metrics: `total_clients`, `total_exchanges`, `total_accounts`, etc.
- Financial: `total_funding`, `total_exchange_balance`, `total_client_pnl`, `total_my_share`
- Lists: `recent_accounts`, `recent_transactions`, `all_clients`, `all_exchanges`
- Filters: `selected_client`, `selected_exchange`, `search_query`, `client_type_filter`

---

#### Step 10: Render Template

```python
return render(request, "core/dashboard.html", context)
```

**Template:** `core/templates/core/dashboard.html`

---

## METRICS & CALCULATIONS

### Metric 1: Total Clients

**Formula:**
```python
total_clients = Client.objects.filter(user=request.user).count()
```

**Calculation:** Count of clients assigned to logged-in user

**Database Query:**
```sql
SELECT COUNT(*) FROM core_client WHERE user_id = {user_id}
```

**Example:**
- User has 5 clients → `total_clients = 5`

---

### Metric 2: Total Exchanges

**Formula:**
```python
total_exchanges = Exchange.objects.count()
```

**Calculation:** Count of all exchanges (system-wide, not user-specific)

**Database Query:**
```sql
SELECT COUNT(*) FROM core_exchange
```

**Example:**
- System has 3 exchanges → `total_exchanges = 3`

---

### Metric 3: Total Accounts

**Formula:**
```python
total_accounts = all_accounts.count()
```

**Where:**
```python
all_accounts = ClientExchangeAccount.objects.filter(client__user=request.user)
```

**Calculation:** Count of client-exchange accounts for logged-in user

**Database Query:**
```sql
SELECT COUNT(*) FROM core_clientexchangeaccount 
WHERE client_id IN (SELECT id FROM core_client WHERE user_id = {user_id})
```

**Example:**
- User has 10 client-exchange accounts → `total_accounts = 10`

---

### Metric 4: Total Funding

**Formula:**
```python
total_funding = sum(account.funding for account in all_accounts)
```

**Calculation:** Sum of all `funding` values for user's accounts

**Database Query:**
```sql
SELECT SUM(funding) FROM core_clientexchangeaccount 
WHERE client_id IN (SELECT id FROM core_client WHERE user_id = {user_id})
```

**Example:**
- Account 1: funding = 1000
- Account 2: funding = 2000
- Account 3: funding = 500
- **Total:** `total_funding = 3500`

---

### Metric 5: Total Exchange Balance

**Formula:**
```python
total_exchange_balance = sum(account.exchange_balance for account in all_accounts)
```

**Calculation:** Sum of all `exchange_balance` values for user's accounts

**Database Query:**
```sql
SELECT SUM(exchange_balance) FROM core_clientexchangeaccount 
WHERE client_id IN (SELECT id FROM core_client WHERE user_id = {user_id})
```

**Example:**
- Account 1: balance = 1200
- Account 2: balance = 1800
- Account 3: balance = 400
- **Total:** `total_exchange_balance = 3400`

---

### Metric 6: Total Client PnL

**Formula:**
```python
total_client_pnl = sum(account.compute_client_pnl() for account in all_accounts)
```

**Where:**
```python
def compute_client_pnl(self):
    return self.exchange_balance - self.funding
```

**Calculation:** Sum of computed PnL for each account

**Per-Account Formula:**
```
Client_PnL = ExchangeBalance − Funding
```

**Example:**
- Account 1: balance=1200, funding=1000 → PnL = +200
- Account 2: balance=1800, funding=2000 → PnL = -200
- Account 3: balance=400, funding=500 → PnL = -100
- **Total:** `total_client_pnl = -100`

**Display:** Green if positive, red if negative

---

### Metric 7: Total My Share

**Formula:**
```python
total_my_share = sum(account.compute_my_share() for account in all_accounts)
```

**Where:**
```python
def compute_my_share(self):
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

**Calculation:** Sum of computed shares for each account

**Per-Account Formula:**
```
ExactShare = |Client_PnL| × (Share% / 100)
FinalShare = floor(ExactShare)
```

**Example:**
- Account 1: PnL=-200, Loss%=10% → Share = floor(20) = 20
- Account 2: PnL=+100, Profit%=20% → Share = floor(20) = 20
- Account 3: PnL=-50, Loss%=10% → Share = floor(5) = 5
- **Total:** `total_my_share = 45`

---

### Metric 8: Total Turnover

**Formula:**
```python
total_turnover = transactions_qs.aggregate(total=Sum("amount"))["total"] or 0
```

**Calculation:** Sum of all transaction amounts (filtered by user/client/exchange/search)

**Database Query:**
```sql
SELECT SUM(amount) FROM core_transaction 
WHERE client_exchange_id IN (
    SELECT id FROM core_clientexchangeaccount 
    WHERE client_id IN (SELECT id FROM core_client WHERE user_id = {user_id})
)
```

**Example:**
- Transaction 1: amount = 500
- Transaction 2: amount = 300
- Transaction 3: amount = 200
- **Total:** `total_turnover = 1000`

---

## FILTERS & SEARCH

### Filter 1: Client Filter

**Parameter:** `client`

**Usage:**
```
/?client=1
```

**Implementation:**
```python
client_id = request.GET.get("client")
if client_id:
    transactions_qs = transactions_qs.filter(client_exchange__client_id=client_id)
```

**Effect:** Filters transactions and accounts to specific client

---

### Filter 2: Exchange Filter

**Parameter:** `exchange`

**Usage:**
```
/?exchange=2
```

**Implementation:**
```python
exchange_id = request.GET.get("exchange")
if exchange_id:
    transactions_qs = transactions_qs.filter(client_exchange__exchange_id=exchange_id)
```

**Effect:** Filters transactions and accounts to specific exchange

---

### Filter 3: Search Query

**Parameter:** `search`

**Usage:**
```
/?search=john
```

**Implementation:**
```python
search_query = request.GET.get("search", "")
if search_query:
    transactions_qs = transactions_qs.filter(
        Q(client_exchange__client__name__icontains=search_query) |
        Q(client_exchange__client__code__icontains=search_query) |
        Q(client_exchange__exchange__name__icontains=search_query)
    )
```

**Search Fields:**
- Client name (case-insensitive)
- Client code (case-insensitive)
- Exchange name (case-insensitive)

**Effect:** Filters transactions matching search term

---

### Filter 4: Client Type Filter

**Parameter:** `client_type`

**Usage:**
```
/?client_type=my
```

**Implementation:**
```python
client_type_filter = request.GET.get("client_type") or request.session.get('client_type_filter', 'all')
if client_type_filter == '':
    client_type_filter = 'all'
```

**Values:**
- `all`: All clients
- `my`: My clients only
- `company`: Company clients only

**Storage:** Stored in session for persistence

---

## DATA FLOW

### Complete Flow Diagram

```
┌─────────────────┐
│  User Accesses  │
│  Root URL (/)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  @login_required│
│  Check Auth     │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌──────┐  ┌──────────┐
│Logged│  │Not Logged│
│  In  │  │          │
└───┬──┘  └────┬─────┘
    │          │
    │          ▼
    │    ┌──────────┐
    │    │Redirect to│
    │    │Login Page│
    │    └──────────┘
    │
    ▼
┌─────────────────┐
│ Extract Filters │
│ from GET params │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Query           │
│ Transactions    │
│ (with filters)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Calculate       │
│ Total Turnover  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Query All       │
│ Accounts        │
│ (for user)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Calculate       │
│ Financial       │
│ Metrics         │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Count Entities  │
│ (clients, etc.) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Get Recent      │
│ Accounts        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Build Context   │
│ Dictionary      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Render Template │
│ dashboard.html   │
└─────────────────┘
```

---

## CODE REFERENCE

### View: dashboard

**File:** `core/views.py`

**Lines:** 204-388

**Full Code:**
```python
@login_required
def dashboard(request):
    """Minimal dashboard view summarizing key metrics with filters."""
    
    today = date.today()
    
    # Filters
    client_id = request.GET.get("client")
    exchange_id = request.GET.get("exchange")
    search_query = request.GET.get("search", "")
    client_type_filter = request.GET.get("client_type") or request.session.get('client_type_filter', 'all')
    if client_type_filter == '':
        client_type_filter = 'all'
    
    # Base queryset
    transactions_qs = Transaction.objects.select_related(
        "client_exchange", 
        "client_exchange__client", 
        "client_exchange__exchange"
    ).filter(client_exchange__client__user=request.user)
    
    # Apply filters
    if client_id:
        transactions_qs = transactions_qs.filter(client_exchange__client_id=client_id)
    
    if exchange_id:
        transactions_qs = transactions_qs.filter(client_exchange__exchange_id=exchange_id)
    
    if search_query:
        transactions_qs = transactions_qs.filter(
            Q(client_exchange__client__name__icontains=search_query) |
            Q(client_exchange__client__code__icontains=search_query) |
            Q(client_exchange__exchange__name__icontains=search_query)
        )
    
    total_turnover = transactions_qs.aggregate(total=Sum("amount"))["total"] or 0
    
    # Get all accounts for the current user
    all_accounts = ClientExchangeAccount.objects.filter(
        client__user=request.user
    ).select_related("client", "exchange")
    
    # Calculate totals from accounts
    total_funding = sum(account.funding for account in all_accounts)
    total_exchange_balance = sum(account.exchange_balance for account in all_accounts)
    total_client_pnl = sum(account.compute_client_pnl() for account in all_accounts)
    total_my_share = sum(account.compute_my_share() for account in all_accounts)
    
    # Count totals
    total_clients = Client.objects.filter(user=request.user).count()
    total_exchanges = Exchange.objects.count()
    total_accounts = all_accounts.count()
    
    # Get recent accounts (last 10 updated)
    recent_accounts = all_accounts.order_by("-updated_at")[:10]
    
    context = {
        "today": today,
        "total_clients": total_clients,
        "total_exchanges": total_exchanges,
        "total_accounts": total_accounts,
        "total_funding": total_funding,
        "total_exchange_balance": total_exchange_balance,
        "total_client_pnl": total_client_pnl,
        "total_my_share": total_my_share,
        "recent_accounts": recent_accounts,
        "total_turnover": total_turnover,
        # ... other context variables
    }
    return render(request, "core/dashboard.html", context)
```

---

### Template: dashboard.html

**File:** `core/templates/core/dashboard.html`

**Structure:**
```html
{% extends "core/base.html" %}

{% block title %}Dashboard · Transaction Hub{% endblock %}
{% block page_title %}Dashboard{% endblock %}
{% block page_subtitle %}Live overview of turnover, profit, and pending payments{% endblock %}

{% block content %}
<!-- Card Grid -->
<div class="card-grid">
    <!-- 7 summary cards -->
</div>

<!-- Recent Accounts Table -->
{% if recent_accounts %}
<div>
    <h3>Recent Accounts</h3>
    <table>
        <!-- Table rows -->
    </table>
</div>
{% endif %}
{% endblock %}
```

---

### URL Configuration

**File:** `core/urls.py`

**Pattern:**
```python
urlpatterns = [
    path('', views.dashboard, name='dashboard'),
    # ... other patterns
]
```

**URL:** `/` (root URL)

---

## EDGE CASES & SCENARIOS

### Edge Case 1: No Accounts

**Scenario:**
- User has no client-exchange accounts

**Behavior:**
- `all_accounts` is empty queryset
- All sums = 0
- `total_accounts = 0`
- `recent_accounts` is empty (table not displayed)

**Display:**
- Cards show 0 values
- Recent accounts section hidden

---

### Edge Case 2: No Transactions

**Scenario:**
- User has accounts but no transactions

**Behavior:**
- `total_turnover = 0`
- Accounts still displayed
- PnL and shares calculated from account balances

---

### Edge Case 3: Negative PnL

**Scenario:**
- Total client PnL is negative (losses exceed profits)

**Behavior:**
- `total_client_pnl` is negative
- Card displays with `negative` CSS class (red color)
- Shares calculated using loss percentage

**Display:**
```html
<div class="card-value negative">{{ total_client_pnl|floatformat:0 }}</div>
```

---

### Edge Case 4: Zero PnL Account

**Scenario:**
- Account has `exchange_balance == funding` (PnL = 0)

**Behavior:**
- `compute_client_pnl()` returns 0
- `compute_my_share()` returns 0
- Table shows "N.A" for PnL and Share

**Display:**
```django
{% if pnl == 0 %}
    <span style="color: var(--muted);">N.A</span>
{% endif %}
```

---

### Edge Case 5: Very Large Numbers

**Scenario:**
- Total funding or balance exceeds display limits

**Behavior:**
- `floatformat:0` formats as integer (no decimals)
- Large numbers displayed as-is
- No scientific notation

**Example:**
- `total_funding = 1234567890` → Display: "1234567890"

---

### Edge Case 6: Multiple Users

**Scenario:**
- System has multiple users with different clients

**Behavior:**
- Each user sees only their own clients
- `filter(client__user=request.user)` ensures isolation
- Totals calculated only for user's accounts

---

### Edge Case 7: Filtered View

**Scenario:**
- User applies client or exchange filter

**Behavior:**
- Transactions filtered accordingly
- Accounts still show all (not filtered in current implementation)
- Totals calculated from all accounts (not filtered)

**Note:** Current implementation filters transactions but not account totals

---

### Edge Case 8: Search with No Results

**Scenario:**
- User searches for non-existent client/exchange

**Behavior:**
- `transactions_qs` is empty
- `total_turnover = 0`
- Accounts still displayed (search doesn't filter accounts)

---

### Edge Case 9: Recent Accounts Limit

**Scenario:**
- User has more than 10 accounts

**Behavior:**
- Only last 10 updated accounts shown
- `[:10]` slice limits results
- Ordered by `-updated_at` (most recent first)

---

### Edge Case 10: Unauthenticated Access

**Scenario:**
- User tries to access dashboard without login

**Behavior:**
- `@login_required` redirects to login page
- After login, redirects back to dashboard

**Protection:**
```python
@login_required
def dashboard(request):
    # View code
```

---

## TESTING GUIDE

### Test Scenario 1: Basic Dashboard Load

**Steps:**
1. Login as authenticated user
2. Navigate to `/`
3. View dashboard

**Expected:**
- ✅ All 7 cards displayed
- ✅ Values shown (0 if no data)
- ✅ Recent accounts table shown (if accounts exist)
- ✅ No errors

**Verify:**
- `total_clients` matches user's client count
- `total_accounts` matches user's account count
- All calculations correct

---

### Test Scenario 2: Financial Metrics Calculation

**Steps:**
1. Create test accounts:
   - Account 1: funding=1000, balance=1200
   - Account 2: funding=2000, balance=1800
   - Account 3: funding=500, balance=400
2. Access dashboard

**Expected:**
- ✅ `total_funding = 3500`
- ✅ `total_exchange_balance = 3400`
- ✅ `total_client_pnl = -100` (3400 - 3500)
- ✅ `total_my_share` calculated correctly

---

### Test Scenario 3: Recent Accounts Display

**Steps:**
1. Create 15 accounts
2. Update 5 accounts (to change `updated_at`)
3. Access dashboard

**Expected:**
- ✅ Only 10 accounts shown
- ✅ Most recently updated accounts first
- ✅ Table columns correct
- ✅ PnL and Share calculated per account

---

### Test Scenario 4: Zero PnL Account Display

**Steps:**
1. Create account with funding=1000, balance=1000
2. Access dashboard

**Expected:**
- ✅ Account shown in recent accounts
- ✅ PnL shows "N.A"
- ✅ Share shows "N.A"
- ✅ No errors

---

### Test Scenario 5: Negative PnL Styling

**Steps:**
1. Create account with funding=1000, balance=800
2. Access dashboard

**Expected:**
- ✅ Total Client PnL card shows negative value
- ✅ Card has `negative` CSS class (red color)
- ✅ Account PnL shows negative value (red)

---

### Test Scenario 6: Client Filter

**Steps:**
1. Create multiple clients with accounts
2. Access `/client=1`
3. View dashboard

**Expected:**
- ✅ Transactions filtered to client 1
- ✅ `total_turnover` reflects filtered transactions
- ✅ Accounts still show all (current behavior)

---

### Test Scenario 7: Search Functionality

**Steps:**
1. Create client named "John Doe"
2. Access `/?search=john`
3. View dashboard

**Expected:**
- ✅ Transactions filtered to matching client/exchange
- ✅ `total_turnover` reflects filtered transactions
- ✅ Search term preserved in context

---

### Test Scenario 8: Empty State

**Steps:**
1. Create user with no clients/accounts
2. Access dashboard

**Expected:**
- ✅ All cards show 0
- ✅ Recent accounts section hidden
- ✅ No errors
- ✅ Page loads successfully

---

### Test Scenario 9: Large Dataset

**Steps:**
1. Create 100+ accounts
2. Access dashboard

**Expected:**
- ✅ Dashboard loads (may be slower)
- ✅ Only 10 recent accounts shown
- ✅ Totals calculated correctly
- ✅ No performance issues

---

### Test Scenario 10: Unauthenticated Access

**Steps:**
1. Logout (if logged in)
2. Navigate to `/`

**Expected:**
- ❌ Access denied
- ✅ Redirect to login page
- ✅ After login, redirect back to dashboard

---

## SUMMARY

### Key Components

1. **Database Models:**
   - `Client`: Client entities
   - `Exchange`: Exchange platforms
   - `ClientExchangeAccount`: Client-exchange relationships
   - `Transaction`: Transaction records

2. **View:** `dashboard`
   - Calculates metrics from database
   - Applies filters and search
   - Builds context for template

3. **Template:** `dashboard.html`
   - Card grid with 7 summary cards
   - Recent accounts table

### Metrics Displayed

1. **Total Clients:** Count of user's clients
2. **Total Exchanges:** Count of all exchanges
3. **Total Accounts:** Count of user's accounts
4. **Total Funding:** Sum of all funding
5. **Total Exchange Balance:** Sum of all balances
6. **Total Client PnL:** Sum of computed PnL
7. **Total My Share:** Sum of computed shares

### Calculations

- **PnL:** `ExchangeBalance − Funding`
- **Share:** `floor(|PnL| × Share%)`
- **Totals:** Sum of account values

### Filters

- Client filter (`?client=1`)
- Exchange filter (`?exchange=2`)
- Search (`?search=term`)
- Client type (`?client_type=my`)

### User Isolation

- Only shows data for logged-in user
- `filter(client__user=request.user)` ensures privacy
- Each user sees their own metrics only

---

**END OF DOCUMENTATION**

