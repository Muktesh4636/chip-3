# REPORTS SYSTEM - COMPLETE DOCUMENTATION

**Version:** 1.0  
**Status:** Current Implementation  
**Last Updated:** January 2026

---

## TABLE OF CONTENTS

1. [System Overview](#system-overview)
2. [Data Sources](#data-sources)
3. [Report Sections](#report-sections)
4. [Formulas and Calculations](#formulas-and-calculations)
5. [Filtering Logic](#filtering-logic)
6. [Time Travel Mode](#time-travel-mode)
7. [Code Implementation](#code-implementation)
8. [Current Limitations](#current-limitations)
9. [Future Enhancements](#future-enhancements)

---

## SYSTEM OVERVIEW

The Reports System provides analytics and insights into transaction activity and business performance.

### Key Principles

1. **Transaction-Based Reporting**: Reports are built from `Transaction` model records (audit trail)
2. **User-Scoped**: All reports filter by `request.user` (only shows user's own data)
3. **Time-Filtered**: Supports month selection, date ranges, and time-travel mode
4. **Client-Filterable**: Can filter reports by specific client
5. **Real-Time**: Calculations performed on-demand from database

### Report Types

- **Overview Report**: Main dashboard with multiple charts and statistics
- **Daily Report**: Day-by-day breakdown
- **Weekly Report**: Week-by-week trends
- **Monthly Report**: Month-by-month trends
- **Custom Report**: User-defined date range
- **Client Report**: Per-client analysis
- **Exchange Report**: Per-exchange analysis
- **Time Travel Report**: Historical point-in-time analysis

---

## DATA SOURCES

### Primary Source: Transaction Model

**Model:** `Transaction`  
**Location:** `core/models.py`

**Fields Used:**
- `id`: Transaction ID
- `client_exchange`: ForeignKey to ClientExchangeAccount
- `date`: Transaction date
- `type`: Transaction type (choices: FUNDING, RECORD_PAYMENT, TRADE, FEE, ADJUSTMENT)
- `amount`: Transaction amount (BIGINT)
- `exchange_balance_after`: Exchange balance after transaction (for audit)
- `notes`: Optional notes
- `created_at`: Timestamp when record was created

**Transaction Types:**
- `FUNDING`: Money given to client
- `RECORD_PAYMENT`: Settlement payment recorded
- `TRADE`: Trading activity (legacy/placeholder)
- `FEE`: Fee transaction
- `ADJUSTMENT`: Balance adjustment

### Secondary Source: ClientExchangeAccount Model

**Note:** Currently NOT used in reports, but should be for profit/loss calculations.

**Fields Available:**
- `funding`: Total funding given to client
- `exchange_balance`: Current balance on exchange
- `compute_client_pnl()`: Calculates Client_PnL = exchange_balance - funding
- `compute_my_share()`: Calculates admin share using masked share settlement system

---

## REPORT SECTIONS

### Section 1: Summary Statistics Cards

**Location:** Top of reports page  
**Display:** Grid of stat boxes

#### 1.1 Total Turnover

**Formula:**
```
TotalTurnover = SUM(Transaction.amount) WHERE filtered_by_user_and_dates
```

**Calculation:**
```python
total_turnover = base_qs.aggregate(total=Sum("amount"))["total"] or 0
```

**What It Shows:**
- Sum of all transaction amounts in the selected time period
- Includes all transaction types (FUNDING, RECORD_PAYMENT, TRADE, FEE, ADJUSTMENT)
- Filtered by user and date range

**Display:**
- Large number with currency formatting
- Label: "Total Turnover"

#### 1.2 Your Total Profit

**Formula (CASH-FLOW BASED):**
```
My Profit = Total Money Received from clients − Total Money Paid to clients

YourTotalProfit = Σ(RECORD_PAYMENT.amount)
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.client_exchange.client.user = current_user
  AND date filters applied
```

**Sign Convention:**
- **Client pays me** (Loss case): `amount` is **POSITIVE** → Profit for me
- **I pay client** (Profit case): `amount` is **NEGATIVE** → Expense for me

**Implementation:**
```python
payment_transactions = base_qs.filter(type='RECORD_PAYMENT')
your_total_profit = payment_transactions.aggregate(total=Sum("amount"))["total"] or Decimal(0)

# Breakdown for display
your_total_income_from_clients = payment_transactions.filter(amount__gt=0).aggregate(
    total=Sum("amount")
)["total"] or Decimal(0)

your_total_paid_to_clients = abs(payment_transactions.filter(amount__lt=0).aggregate(
    total=Sum("amount")
)["total"] or Decimal(0))
```

**Example:**
```
Transactions:
  Client A pays me: +4,000
  Client B pays me: +6,000
  I pay Client C:   -3,000
  I pay Client D:   -5,000

Calculation:
  Money received = 4,000 + 6,000 = 10,000
  Money paid     = 3,000 + 5,000 = 8,000
  My Profit      = 10,000 - 8,000 = 2,000
```

**Display:**
- Shows actual cash-flow based profit
- Positive = you earned money
- Negative = you paid more than received
- Label: "Your Total Profit"
- Shows breakdown: "Money received (₹X) - Money paid (₹Y)"

#### 1.3 Company Profit

**Formula:**
```python
company_profit = Decimal(0)  # Always 0 - all clients are "my clients"
```

**Why Zero:**
- System no longer distinguishes company clients
- All clients are personal clients
- Company share concept removed

**Display:**
- Always shows 0
- Label: "Company Profit"

---

### Section 2: Daily Trends Chart

**Location:** First chart section  
**Display:** Line chart showing last 30 days

#### 2.1 Date Range Calculation

**Formula:**
```
IF time_travel_mode AND start_date_str AND end_date_str:
    start_date = parse(start_date_str)
    end_date = parse(end_date_str)
    IF (end_date - start_date).days > 30:
        end_date = start_date + 30 days  # Limit to 30 days
ELSE:
    start_date = today - 30 days
    end_date = today
```

**Implementation:**
```python
if time_travel_mode and start_date_str and end_date_str:
    start_date = date.fromisoformat(start_date_str)
    end_date = date.fromisoformat(end_date_str)
    days_diff = (end_date - start_date).days
    if days_diff > 30:
        end_date = start_date + timedelta(days=30)
else:
    start_date = today - timedelta(days=30)
    end_date = today
```

#### 2.2 Daily Data Aggregation

**Formula:**
```
FOR EACH date FROM start_date TO end_date:
    DailyTurnover[date] = SUM(Transaction.amount) 
                          WHERE Transaction.date = date 
                          AND filtered_by_user_and_client
    
    DailyProfit[date] = SUM(RECORD_PAYMENT.amount)
                        WHERE Transaction.type = 'RECORD_PAYMENT'
                          AND Transaction.date = date
                          AND filtered_by_user_and_client
    
    DailyLoss[date] = ABS(SUM(RECORD_PAYMENT.amount))
                      WHERE Transaction.type = 'RECORD_PAYMENT'
                        AND Transaction.amount < 0
                        AND Transaction.date = date
                        AND filtered_by_user_and_client
```

**Implementation:**
```python
daily_data = defaultdict(lambda: {"profit": 0, "loss": 0, "turnover": 0})

# Daily turnover (all transaction types)
daily_transactions = base_qs.filter(
    date__gte=start_date,
    date__lte=end_date
).values("date").annotate(
    turnover_sum=Sum("amount")
)

for item in daily_transactions:
    tx_date = item["date"]
    daily_data[tx_date]["turnover"] += float(item["turnover_sum"] or 0)

# Daily profit/loss from RECORD_PAYMENT transactions
daily_payments = base_qs.filter(
    type='RECORD_PAYMENT',
    date__gte=start_date,
    date__lte=end_date
).values("date").annotate(
    profit_sum=Sum("amount")  # Can be positive or negative
)

for item in daily_payments:
    tx_date = item["date"]
    profit_amount = float(item["profit_sum"] or 0)
    daily_data[tx_date]["profit"] += profit_amount
    # Loss is the negative part (when I pay clients)
    if profit_amount < 0:
        daily_data[tx_date]["loss"] += abs(profit_amount)
```

#### 2.3 Chart Data Preparation

**Formula:**
```
FOR EACH date FROM start_date TO end_date (max 30 days):
    date_labels.append(date.strftime("%Y-%m-%d"))
    profit_data.append(0)  # Not calculated from transactions
    loss_data.append(0)     # Not calculated from transactions
    turnover_data.append(DailyTurnover[date] or 0)
```

**Implementation:**
```python
date_labels = []
profit_data = []
loss_data = []
turnover_data = []

current_date = start_date
days_count = 0
while current_date <= end_date and days_count < 30:
    day_data = daily_data[current_date]
    date_labels.append(current_date.strftime("%Y-%m-%d"))
    profit_data.append(float(day_data.get("profit", 0)))  # Always 0
    loss_data.append(float(day_data.get("loss", 0)))      # Always 0
    turnover_data.append(float(day_data.get("turnover", 0)))
    current_date += timedelta(days=1)
    days_count += 1
```

**Display:**
- Line chart with 3 series: Profit (always 0), Loss (always 0), Turnover (actual data)
- X-axis: Dates (YYYY-MM-DD format)
- Y-axis: Amount
- Chart type: Line chart (Chart.js)

---

### Section 3: Transaction Type Breakdown

**Location:** Pie/Donut chart section  
**Display:** Breakdown by transaction type

#### 3.1 Type Aggregation

**Formula:**
```
FOR EACH Transaction.type:
    TypeCount[type] = COUNT(Transaction.id) WHERE type = type AND filtered
    TypeTotalAmount[type] = SUM(Transaction.amount) WHERE type = type AND filtered
```

**Implementation:**
```python
type_breakdown = base_qs.values("type").annotate(
    count=Count("id"),
    total_amount=Sum("amount")
)
```

#### 3.2 Type Mapping

**Transaction Types:**
- `FUNDING`: Label="Funding", Color="#4b5563"
- `RECORD_PAYMENT`: Label="Record Payment", Color="#6b7280"
- `TRADE`: Label="Trade", Color="#9ca3af"
- `FEE`: Label="Fee", Color="#6b7280"
- `ADJUSTMENT`: Label="Adjustment", Color="#9ca3af"

**Implementation:**
```python
type_map = {
    'FUNDING': ("Funding", "#4b5563"),
    'RECORD_PAYMENT': ("Record Payment", "#6b7280"),
    'TRADE': ("Trade", "#9ca3af"),
    'FEE': ("Fee", "#6b7280"),
    'ADJUSTMENT': ("Adjustment", "#9ca3af"),
}

for item in type_breakdown:
    tx_type = item["type"]
    if tx_type in type_map:
        label, color = type_map[tx_type]
        type_labels.append(label)
        type_counts.append(item["count"])
        type_amounts.append(float(item["total_amount"] or 0))
        type_colors.append(color)
```

**Display:**
- Pie or donut chart
- Each slice represents a transaction type
- Shows count and total amount per type
- Colors match type_map

---

### Section 4: Monthly Trends Chart

**Location:** Monthly chart section  
**Display:** Bar/line chart for last 6 months

#### 4.1 Month Calculation

**Formula:**
```
FOR i IN 0 TO 5 (last 6 months):
    month_date = today.replace(day=1)
    FOR j IN 0 TO i-1:
        month_date = month_date - 1 month
    
    IF month_date.month == 12:
        month_end = date(month_date.year, 12, 31)
    ELSE:
        month_end = date(month_date.year, month_date.month + 1, 1) - 1 day
```

**Implementation:**
```python
for i in range(6):
    month_date = today.replace(day=1)
    for _ in range(i):
        if month_date.month == 1:
            month_date = month_date.replace(year=month_date.year - 1, month=12)
        else:
            month_date = month_date.replace(month=month_date.month - 1)
    
    if month_date.month == 12:
        month_end = date(month_date.year, 12, 31)
    else:
        month_end = date(month_date.year, month_date.month + 1, 1) - timedelta(days=1)
```

#### 4.2 Monthly Aggregation

**Formula:**
```
FOR EACH month IN last_6_months:
    MonthTurnover[month] = SUM(Transaction.amount) 
                           WHERE Transaction.date >= month_start 
                           AND Transaction.date <= month_end
                           AND filtered_by_user_and_client
    
    MonthProfit[month] = SUM(RECORD_PAYMENT.amount)
                         WHERE Transaction.type = 'RECORD_PAYMENT'
                           AND Transaction.date >= month_start
                           AND Transaction.date <= month_end
                           AND filtered_by_user_and_client
    
    MonthLoss[month] = ABS(SUM(RECORD_PAYMENT.amount))
                       WHERE Transaction.type = 'RECORD_PAYMENT'
                         AND Transaction.amount < 0
                         AND Transaction.date >= month_start
                         AND Transaction.date <= month_end
                         AND filtered_by_user_and_client
```

**Implementation:**
```python
month_transactions = base_qs.filter(
    date__gte=month_date,
    date__lte=month_end
)

# Monthly turnover (all transaction types)
month_turnover_val = month_transactions.aggregate(total=Sum("amount"))["total"] or 0

# Monthly profit/loss from RECORD_PAYMENT transactions
month_payments = month_transactions.filter(type='RECORD_PAYMENT')
month_profit_val = month_payments.aggregate(total=Sum("amount"))["total"] or 0

# Monthly loss (negative payments = I paid clients)
month_loss_val = abs(month_payments.filter(amount__lt=0).aggregate(
    total=Sum("amount")
)["total"] or 0)

monthly_profit.insert(0, float(month_profit_val))
monthly_loss.insert(0, float(month_loss_val))
monthly_turnover.insert(0, float(month_turnover_val))
```

**Display:**
- Bar or line chart
- X-axis: Month labels (e.g., "Jan 2026", "Feb 2026")
- Y-axis: Amount
- 3 series: Profit (always 0), Loss (always 0), Turnover (actual data)
- Months ordered oldest to newest (left to right)

---

### Section 5: Weekly Trends Chart

**Location:** Weekly chart section  
**Display:** Bar/line chart for last 4 weeks

#### 5.1 Week Calculation

**Formula:**
```
FOR i IN 0 TO 3 (last 4 weeks):
    week_end = today - (i * 7 days)
    week_start = week_end - 6 days
```

**Implementation:**
```python
for i in range(4):
    week_end = today - timedelta(days=i*7)
    week_start = week_end - timedelta(days=6)
    weekly_labels.insert(0, f"Week {4-i} ({week_start.strftime('%b %d')} - {week_end.strftime('%b %d')})")
```

#### 5.2 Weekly Aggregation

**Formula:**
```
FOR EACH week IN last_4_weeks:
    WeekTurnover[week] = SUM(Transaction.amount) 
                         WHERE Transaction.date >= week_start 
                         AND Transaction.date <= week_end
                         AND filtered_by_user_and_client
    
    WeekProfit[week] = SUM(RECORD_PAYMENT.amount)
                       WHERE Transaction.type = 'RECORD_PAYMENT'
                         AND Transaction.date >= week_start
                         AND Transaction.date <= week_end
                         AND filtered_by_user_and_client
    
    WeekLoss[week] = ABS(SUM(RECORD_PAYMENT.amount))
                     WHERE Transaction.type = 'RECORD_PAYMENT'
                       AND Transaction.amount < 0
                       AND Transaction.date >= week_start
                       AND Transaction.date <= week_end
                       AND filtered_by_user_and_client
```

**Implementation:**
```python
week_transactions = base_qs.filter(
    date__gte=week_start,
    date__lte=week_end
)

# Weekly turnover (all transaction types)
week_turnover_val = week_transactions.aggregate(total=Sum("amount"))["total"] or 0

# Weekly profit/loss from RECORD_PAYMENT transactions
week_payments = week_transactions.filter(type='RECORD_PAYMENT')
week_profit_val = week_payments.aggregate(total=Sum("amount"))["total"] or 0

# Weekly loss (negative payments = I paid clients)
week_loss_val = abs(week_payments.filter(amount__lt=0).aggregate(
    total=Sum("amount")
)["total"] or 0)

weekly_profit.insert(0, float(week_profit_val))
weekly_loss.insert(0, float(week_loss_val))
weekly_turnover.insert(0, float(week_turnover_val))
```

**Display:**
- Bar or line chart
- X-axis: Week labels (e.g., "Week 1 (Jan 01 - Jan 07)")
- Y-axis: Amount
- 3 series: Profit (always 0), Loss (always 0), Turnover (actual data)
- Weeks ordered oldest to newest (left to right)

---

### Section 6: Top Clients Chart

**Location:** Top clients section  
**Display:** Bar chart showing top clients by turnover

#### 6.1 Client Aggregation

**Formula:**
```
TopClients = SELECT 
    Transaction.client_exchange.client.name,
    SUM(Transaction.amount) AS total_turnover
FROM Transaction
WHERE Transaction.date >= start_date
  AND filtered_by_user_and_client
GROUP BY Transaction.client_exchange.client.name
ORDER BY total_turnover DESC
LIMIT 10
```

**Implementation:**
```python
top_clients = base_qs.filter(
    date__gte=start_date
).values(
    "client_exchange__client__name"
).annotate(
    total_turnover=Sum("amount")
).order_by("-total_turnover")[:10]

client_labels = [item["client_exchange__client__name"] for item in top_clients]
client_profits = [float(item["total_turnover"] or 0) for item in top_clients]
```

**Note:** Variable named `client_profits` but actually contains turnover data.

**Display:**
- Bar chart
- X-axis: Client names
- Y-axis: Turnover amount
- Shows top 10 clients by turnover in last 30 days (or filtered period)
- Bars ordered highest to lowest

---

### Section 7: Recent Transactions Table

**Location:** Bottom of reports page  
**Display:** Table showing recent transactions

#### 7.1 Transaction Selection

**Formula:**
```
RecentTransactions = SELECT 
    Transaction.*,
    Transaction.client_exchange.client.name,
    Transaction.client_exchange.exchange.name
FROM Transaction
WHERE filtered_by_user_and_client_and_dates
ORDER BY Transaction.date DESC, Transaction.created_at DESC
LIMIT 50
```

**Implementation:**
```python
time_travel_transactions = base_qs.select_related(
    "client_exchange", 
    "client_exchange__client", 
    "client_exchange__exchange"
).order_by("-date", "-created_at")[:50]
```

**Display:**
- Table with columns:
  - Date
  - Client
  - Exchange
  - Type
  - Amount
  - Notes
- Sorted by date (newest first), then by created_at (newest first)
- Limited to 50 most recent transactions

---

## FORMULAS AND CALCULATIONS

### Formula 1: Total Turnover

```
TotalTurnover = Σ(Transaction.amount)
WHERE Transaction.client_exchange.client.user = current_user
  AND Transaction.date >= start_date (if filtered)
  AND Transaction.date <= end_date (if filtered)
  AND Transaction.client_exchange.client_id = client_id (if filtered)
```

**Django ORM:**
```python
base_qs = Transaction.objects.filter(
    client_exchange__client__user=request.user
)

if client_id:
    base_qs = base_qs.filter(client_exchange__client_id=client_id)

if date_filter:
    base_qs = base_qs.filter(**date_filter)

total_turnover = base_qs.aggregate(total=Sum("amount"))["total"] or 0
```

---

### Formula 1A: My Profit (CASH-FLOW BASED)

```
MyProfit = Σ(RECORD_PAYMENT.amount)
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.client_exchange.client.user = current_user
  AND Transaction.date >= start_date (if filtered)
  AND Transaction.date <= end_date (if filtered)
  AND Transaction.client_exchange.client_id = client_id (if filtered)
```

**Sign Convention:**
- Positive amount = Client paid me (profit)
- Negative amount = I paid client (expense)

**Django ORM:**
```python
payment_transactions = base_qs.filter(type='RECORD_PAYMENT')
your_total_profit = payment_transactions.aggregate(total=Sum("amount"))["total"] or Decimal(0)

# Breakdown
your_total_income_from_clients = payment_transactions.filter(amount__gt=0).aggregate(
    total=Sum("amount")
)["total"] or Decimal(0)

your_total_paid_to_clients = abs(payment_transactions.filter(amount__lt=0).aggregate(
    total=Sum("amount")
)["total"] or Decimal(0))
```

**Example:**
```
Client A pays me: +4,000
Client B pays me: +6,000
I pay Client C:   -3,000
I pay Client D:   -5,000

My Profit = 4,000 + 6,000 - 3,000 - 5,000 = 2,000
```

---

### Formula 2: Daily Turnover

```
DailyTurnover[date] = Σ(Transaction.amount)
WHERE Transaction.date = date
  AND Transaction.client_exchange.client.user = current_user
  AND Transaction.client_exchange.client_id = client_id (if filtered)
```

**Django ORM:**
```python
daily_transactions = base_qs.filter(
    date__gte=start_date,
    date__lte=end_date
).values("date").annotate(
    turnover_sum=Sum("amount")
)
```

---

### Formula 2A: Daily Profit/Loss

```
DailyProfit[date] = Σ(RECORD_PAYMENT.amount)
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.date = date
  AND filtered_by_user_and_client

DailyLoss[date] = ABS(Σ(RECORD_PAYMENT.amount))
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.amount < 0
  AND Transaction.date = date
  AND filtered_by_user_and_client
```

**Django ORM:**
```python
daily_payments = base_qs.filter(
    type='RECORD_PAYMENT',
    date__gte=start_date,
    date__lte=end_date
).values("date").annotate(
    profit_sum=Sum("amount")  # Can be positive or negative
)

for item in daily_payments:
    tx_date = item["date"]
    profit_amount = float(item["profit_sum"] or 0)
    daily_data[tx_date]["profit"] += profit_amount
    if profit_amount < 0:
        daily_data[tx_date]["loss"] += abs(profit_amount)
```

---

### Formula 3: Transaction Type Breakdown

```
TypeCount[type] = COUNT(Transaction.id)
WHERE Transaction.type = type
  AND filtered_by_user_and_dates_and_client

TypeTotalAmount[type] = Σ(Transaction.amount)
WHERE Transaction.type = type
  AND filtered_by_user_and_dates_and_client
```

**Django ORM:**
```python
type_breakdown = base_qs.values("type").annotate(
    count=Count("id"),
    total_amount=Sum("amount")
)
```

---

### Formula 4: Monthly Turnover

```
MonthlyTurnover[month] = Σ(Transaction.amount)
WHERE Transaction.date >= month_start
  AND Transaction.date <= month_end
  AND filtered_by_user_and_client
```

**Django ORM:**
```python
month_transactions = base_qs.filter(
    date__gte=month_date,
    date__lte=month_end
)

month_turnover_val = month_transactions.aggregate(total=Sum("amount"))["total"] or 0
```

---

### Formula 4A: Monthly Profit/Loss

```
MonthlyProfit[month] = Σ(RECORD_PAYMENT.amount)
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.date >= month_start
  AND Transaction.date <= month_end
  AND filtered_by_user_and_client

MonthlyLoss[month] = ABS(Σ(RECORD_PAYMENT.amount))
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.amount < 0
  AND Transaction.date >= month_start
  AND Transaction.date <= month_end
  AND filtered_by_user_and_client
```

**Django ORM:**
```python
month_payments = month_transactions.filter(type='RECORD_PAYMENT')
month_profit_val = month_payments.aggregate(total=Sum("amount"))["total"] or 0
month_loss_val = abs(month_payments.filter(amount__lt=0).aggregate(
    total=Sum("amount")
)["total"] or 0)
```

---

### Formula 5: Weekly Turnover

```
WeeklyTurnover[week] = Σ(Transaction.amount)
WHERE Transaction.date >= week_start
  AND Transaction.date <= week_end
  AND filtered_by_user_and_client
```

**Django ORM:**
```python
week_transactions = base_qs.filter(
    date__gte=week_start,
    date__lte=week_end
)

week_turnover_val = week_transactions.aggregate(total=Sum("amount"))["total"] or 0
```

---

### Formula 5A: Weekly Profit/Loss

```
WeeklyProfit[week] = Σ(RECORD_PAYMENT.amount)
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.date >= week_start
  AND Transaction.date <= week_end
  AND filtered_by_user_and_client

WeeklyLoss[week] = ABS(Σ(RECORD_PAYMENT.amount))
WHERE Transaction.type = 'RECORD_PAYMENT'
  AND Transaction.amount < 0
  AND Transaction.date >= week_start
  AND Transaction.date <= week_end
  AND filtered_by_user_and_client
```

**Django ORM:**
```python
week_payments = week_transactions.filter(type='RECORD_PAYMENT')
week_profit_val = week_payments.aggregate(total=Sum("amount"))["total"] or 0
week_loss_val = abs(week_payments.filter(amount__lt=0).aggregate(
    total=Sum("amount")
)["total"] or 0)
```

---

### Formula 6: Top Clients by Turnover

```
TopClients = SELECT 
    Transaction.client_exchange.client.name AS client_name,
    Σ(Transaction.amount) AS total_turnover
FROM Transaction
WHERE Transaction.date >= start_date
  AND filtered_by_user_and_client
GROUP BY Transaction.client_exchange.client.name
ORDER BY total_turnover DESC
LIMIT 10
```

**Django ORM:**
```python
top_clients = base_qs.filter(
    date__gte=start_date
).values(
    "client_exchange__client__name"
).annotate(
    total_turnover=Sum("amount")
).order_by("-total_turnover")[:10]
```

---

## FILTERING LOGIC

### User Filtering

**Always Applied:**
```python
user_filter = {"client_exchange__client__user": request.user}
base_qs = Transaction.objects.filter(**user_filter)
```

**Purpose:** Ensure users only see their own data

---

### Client Filtering

**Optional Filter:**
```python
client_id = request.GET.get("client")
if client_id:
    base_qs = base_qs.filter(client_exchange__client_id=client_id)
```

**Purpose:** Filter reports to show only transactions for a specific client

**UI:** Dropdown selector in reports page

---

### Date Filtering

**Three Modes:**

#### Mode 1: Month Selection (Default)

**Formula:**
```
month_str = request.GET.get("month", today.strftime("%Y-%m"))
year, month = parse(month_str)

selected_month_start = date(year, month, 1)
IF month == 12:
    selected_month_end = date(year, 12, 31)
ELSE:
    selected_month_end = date(year, month + 1, 1) - 1 day

date_filter = {
    "date__gte": selected_month_start,
    "date__lte": selected_month_end
}
```

**Implementation:**
```python
month_str = request.GET.get("month", today.strftime("%Y-%m"))
try:
    year, month = map(int, month_str.split("-"))
    selected_month_start = date(year, month, 1)
    if month == 12:
        selected_month_end = date(year, 12, 31)
    else:
        selected_month_end = date(year, month + 1, 1) - timedelta(days=1)
except (ValueError, IndexError):
    # Fallback to current month
    selected_month_start = date(today.year, today.month, 1)
    if today.month == 12:
        selected_month_end = date(today.year, 12, 31)
    else:
        selected_month_end = date(today.year, today.month + 1, 1) - timedelta(days=1)

date_filter = {
    "date__gte": selected_month_start,
    "date__lte": selected_month_end
}
```

#### Mode 2: Date Range (Time Travel)

**Formula:**
```
IF start_date_str AND end_date_str:
    start_date_filter = parse(start_date_str)
    end_date_filter = parse(end_date_str)
    date_filter = {
        "date__gte": start_date_filter,
        "date__lte": end_date_filter
    }
```

**Implementation:**
```python
start_date_str = request.GET.get("start_date")
end_date_str = request.GET.get("end_date")

if start_date_str and end_date_str:
    start_date_filter = date.fromisoformat(start_date_str)
    end_date_filter = date.fromisoformat(end_date_str)
    date_filter = {"date__gte": start_date_filter, "date__lte": end_date_filter}
```

#### Mode 3: As-Of Date (Time Travel)

**Formula:**
```
IF as_of_str:
    as_of_filter = parse(as_of_str)
    date_filter = {"date__lte": as_of_filter}
    time_travel_mode = True
```

**Implementation:**
```python
as_of_str = request.GET.get("date")
if as_of_str:
    time_travel_mode = True
    as_of_filter = date.fromisoformat(as_of_str)
    date_filter = {"date__lte": as_of_filter}
```

**Priority:** Date range > As-of date > Month selection

---

## TIME TRAVEL MODE

### Purpose

Allows viewing reports as of a specific date or date range in the past.

### Implementation

**Detection:**
```python
time_travel_mode = False

if start_date_str and end_date_str:
    time_travel_mode = True
elif as_of_str:
    time_travel_mode = True
```

**Date Filtering:**
- **Date Range:** Shows transactions between start_date and end_date
- **As-Of Date:** Shows transactions up to and including as_of_date

**Effect on Charts:**
- Daily trends: Limited to 30 days max if date range > 30 days
- Monthly trends: Still shows last 6 months from today (not filtered by time travel)
- Weekly trends: Still shows last 4 weeks from today (not filtered by time travel)
- Top clients: Uses start_date from time travel if available

---

## CODE IMPLEMENTATION

### View Function: `report_overview`

**Location:** `core/views.py` (line ~1488)

**Flow:**
1. Get filters (client, month, date range)
2. Build base queryset (filtered by user)
3. Apply client filter if specified
4. Apply date filter (month, date range, or as-of)
5. Calculate totals (turnover)
6. Calculate daily trends (last 30 days)
7. Calculate transaction type breakdown
8. Calculate monthly trends (last 6 months)
9. Calculate weekly trends (last 4 weeks)
10. Calculate top clients (last 30 days)
11. Get recent transactions (last 50)
12. Build context dictionary
13. Render template

**Key Variables:**
- `base_qs`: Base queryset (Transaction.objects filtered by user)
- `total_turnover`: Sum of all transaction amounts
- `daily_data`: Dictionary mapping dates to turnover
- `type_breakdown`: Aggregated transaction types
- `monthly_labels`, `monthly_turnover`: Last 6 months data
- `weekly_labels`, `weekly_turnover`: Last 4 weeks data
- `top_clients`: Top 10 clients by turnover
- `time_travel_transactions`: Recent 50 transactions

---

### Template: `reports/overview.html`

**Location:** `core/templates/core/reports/overview.html`

**Sections:**
1. **Filter Section:** Client dropdown, month selector
2. **Summary Cards:** Total Turnover, Your Total Profit, Company Profit
3. **Daily Trends Chart:** Chart.js line chart
4. **Transaction Type Breakdown:** Chart.js pie/donut chart
5. **Monthly Trends Chart:** Chart.js bar/line chart
6. **Weekly Trends Chart:** Chart.js bar/line chart
7. **Top Clients Chart:** Chart.js bar chart
8. **Recent Transactions Table:** HTML table

**JavaScript:**
- Chart.js initialization for all charts
- Client filter change handler
- Month selector change handler
- Scroll position preservation

---

## CURRENT LIMITATIONS

### 1. Profit/Loss Calculation (FIXED)

**Status:** ✅ IMPLEMENTED

**Implementation:**
- Profit/loss now calculated from RECORD_PAYMENT transactions
- Cash-flow based calculation (money received - money paid)
- Sign convention: Positive = client paid me, Negative = I paid client

**Current Code:**
```python
payment_transactions = base_qs.filter(type='RECORD_PAYMENT')
your_total_profit = payment_transactions.aggregate(total=Sum("amount"))["total"] or Decimal(0)
```

**What Changed:**
- RECORD_PAYMENT transactions now store negative amounts when I pay clients
- Reports calculate profit by summing RECORD_PAYMENT amounts
- Daily/weekly/monthly charts show actual profit/loss trends

---

### 2. Company Profit Always Zero

**Issue:**
- Company profit always shows 0
- System no longer distinguishes company vs personal clients

**Current Code:**
```python
company_profit = Decimal(0)  # All clients are now my clients
```

**Why:**
- Business logic changed
- All clients are now "my clients"
- Company share concept removed

---

### 3. Monthly/Weekly Trends Not Filtered by Time Travel

**Issue:**
- Monthly trends always show last 6 months from today
- Weekly trends always show last 4 weeks from today
- Not filtered by time travel date range

**Current Code:**
```python
# Monthly trends always use today
for i in range(6):
    month_date = today.replace(day=1)  # Uses today, not time travel date
    # ...
```

**Expected Behavior:**
- If time travel mode, should show months/weeks relative to selected date
- Currently shows relative to today regardless of time travel

---

### 4. Top Clients Uses Fixed 30-Day Window

**Issue:**
- Top clients always uses last 30 days from today
- Not filtered by selected month or time travel date range

**Current Code:**
```python
top_clients = base_qs.filter(
    date__gte=start_date  # start_date is from daily trends (30 days ago)
)
```

**Expected Behavior:**
- Should use selected month date range if month selected
- Should use time travel date range if time travel mode
- Currently always uses last 30 days

---

## RECORD_PAYMENT SIGN CONVENTION

### Implementation Details

**When Creating RECORD_PAYMENT Transactions:**

**Location 1:** `record_payment` view (line ~3541)
```python
# SIGN CONVENTION FOR MY PROFIT CALCULATION:
# - If client_pnl < 0 (LOSS): Client pays me → amount is POSITIVE (profit for me)
# - If client_pnl > 0 (PROFIT): I pay client → amount is NEGATIVE (expense for me)
transaction_amount = paid_amount if client_pnl < 0 else -paid_amount

Transaction.objects.create(
    client_exchange=account,
    date=timezone.now(),
    type='RECORD_PAYMENT',
    amount=transaction_amount,  # Positive if client pays me, negative if I pay client
    exchange_balance_after=account.exchange_balance,
    notes=notes or f"Payment recorded: {paid_amount}. {action_desc}"
)
```

**Location 2:** `pending_summary` view (line ~612)
```python
# SIGN CONVENTION FOR MY PROFIT CALCULATION:
# - payment_type == 'client_pays': Client pays me → amount is POSITIVE (profit)
# - payment_type == 'you_pay': I pay client → amount is NEGATIVE (expense)
transaction_amount = amount if payment_type == 'client_pays' else -amount

Transaction.objects.create(
    client_exchange=client_exchange,
    type='RECORD_PAYMENT',
    amount=transaction_amount,  # Positive if client pays me, negative if I pay client
    date=tx_date,
    notes=note or f"Settlement: ₹{amount} ({payment_type})"
)
```

### Sign Convention Summary

| Scenario | Client PnL | Payment Direction | Transaction Amount | My Profit Impact |
|----------|------------|-------------------|-------------------|------------------|
| Loss | < 0 | Client → Admin | **+amount** | ✅ Positive (income) |
| Profit | > 0 | Admin → Client | **-amount** | ❌ Negative (expense) |

---

## FUTURE ENHANCEMENTS

### Enhancement 1: Profit/Loss Calculation (COMPLETED ✅)

**Status:** ✅ IMPLEMENTED

**Implementation:** Cash-flow based calculation from RECORD_PAYMENT transactions
- Positive amounts = client paid me (profit)
- Negative amounts = I paid client (expense)
- My Profit = Σ(RECORD_PAYMENT.amount)

---

### Enhancement 2: Time-Based Profit/Loss

**Goal:** Show profit/loss trends over time

**Implementation:**
- Create daily snapshots of ClientExchangeAccount state
- Store PnL and share calculations per day
- Aggregate snapshots for historical reports

**Alternative:**
- Calculate profit/loss from transactions (if transactions tracked PnL changes)
- Currently transactions don't track PnL, only amounts

---

### Enhancement 3: Filter Monthly/Weekly by Time Travel

**Goal:** Make monthly/weekly trends respect time travel mode

**Implementation:**
```python
# Use time travel date instead of today
reference_date = today
if time_travel_mode and end_date_str:
    reference_date = date.fromisoformat(end_date_str)
elif time_travel_mode and as_of_str:
    reference_date = date.fromisoformat(as_of_str)

# Calculate months/weeks relative to reference_date
for i in range(6):
    month_date = reference_date.replace(day=1)
    for _ in range(i):
        # Go back i months from reference_date
        # ...
```

---

### Enhancement 4: Filter Top Clients by Selected Period

**Goal:** Make top clients respect selected month/date range

**Implementation:**
```python
# Use selected month date range if available
if date_filter:
    top_clients = base_qs.filter(**date_filter).values(...)
else:
    # Fallback to last 30 days
    top_clients = base_qs.filter(
        date__gte=start_date
    ).values(...)
```

---

## SUMMARY

### What Data is Shown

1. **Total Turnover:** Sum of all transaction amounts
2. **Your Total Profit:** ✅ Cash-flow based: `Σ(RECORD_PAYMENT.amount)` (money received - money paid)
3. **Company Profit:** Always 0 (concept removed)
4. **Daily Trends:** Last 30 days of turnover and profit/loss from RECORD_PAYMENT
5. **Transaction Type Breakdown:** Count and total by type
6. **Monthly Trends:** Last 6 months of turnover and profit/loss from RECORD_PAYMENT
7. **Weekly Trends:** Last 4 weeks of turnover and profit/loss from RECORD_PAYMENT
8. **Top Clients:** Top 10 clients by turnover in last 30 days
9. **Recent Transactions:** Last 50 transactions table

### Key Formulas

1. **Total Turnover:** `Σ(Transaction.amount) WHERE filtered`
2. **My Profit:** `Σ(RECORD_PAYMENT.amount) WHERE type='RECORD_PAYMENT' AND filtered`
3. **Daily Turnover:** `Σ(Transaction.amount) GROUP BY date`
4. **Daily Profit:** `Σ(RECORD_PAYMENT.amount) WHERE type='RECORD_PAYMENT' GROUP BY date`
5. **Daily Loss:** `ABS(Σ(RECORD_PAYMENT.amount)) WHERE type='RECORD_PAYMENT' AND amount<0 GROUP BY date`
6. **Type Breakdown:** `COUNT(id), SUM(amount) GROUP BY type`
7. **Monthly Turnover:** `Σ(Transaction.amount) WHERE date IN month`
8. **Monthly Profit:** `Σ(RECORD_PAYMENT.amount) WHERE type='RECORD_PAYMENT' AND date IN month`
9. **Weekly Turnover:** `Σ(Transaction.amount) WHERE date IN week`
10. **Weekly Profit:** `Σ(RECORD_PAYMENT.amount) WHERE type='RECORD_PAYMENT' AND date IN week`
11. **Top Clients:** `SUM(amount) GROUP BY client ORDER BY SUM DESC LIMIT 10`

### Filtering

- **User:** Always filtered by `request.user`
- **Client:** Optional filter by `client_id`
- **Date:** Month selection (default), date range, or as-of date

### Current State

- ✅ Transaction-based reporting works
- ✅ Turnover calculations accurate
- ✅ **Profit/loss calculation implemented (cash-flow based)**
- ✅ **Daily/weekly/monthly profit/loss charts show actual data**
- ✅ Date filtering works
- ✅ Client filtering works
- ✅ Sign convention: Positive = client paid me, Negative = I paid client
- ❌ Monthly/weekly trends not filtered by time travel
- ❌ Top clients uses fixed 30-day window

---

**END OF DOCUMENTATION**

