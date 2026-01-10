# Reports System - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [Report Types](#report-types)
3. [Core Concepts](#core-concepts)
4. [Data Sources](#data-sources)
5. [Profit/Loss Calculation Logic](#profitloss-calculation-logic)
6. [Turnover Calculation](#turnover-calculation)
7. [Report Views Detailed](#report-views-detailed)
8. [Time Travel Reporting](#time-travel-reporting)
9. [Client Exchange Report Config](#client-exchange-report-config)
10. [Export Functionality](#export-functionality)
11. [Charts and Visualizations](#charts-and-visualizations)
12. [Filtering and Date Ranges](#filtering-and-date-ranges)
13. [Key Methods Reference](#key-methods-reference)

---

## Overview

The Reports System provides comprehensive analytics and insights into business performance, tracking profits, losses, turnover, and settlement activities across different time periods and dimensions.

### Key Features

- **Multiple Report Types**: Daily, Weekly, Monthly, Custom period, Overview, Client-specific, Exchange-specific
- **Time Travel**: View historical data as of a specific date or date range
- **Profit/Loss Tracking**: Based on RECORD_PAYMENT transactions
- **Turnover Analysis**: Cash movement tracking
- **Visualizations**: Charts and graphs for trends
- **Export Capabilities**: CSV export for external analysis
- **Friend/My Profit Split**: Support for profit sharing between partners

### Core Principle

**SINGLE SOURCE OF TRUTH**: All profit/loss calculations use `RECORD_PAYMENT` transactions only. The transaction `amount` field contains the signed value:
- **Positive amount** = Client paid YOU (your profit from client loss)
- **Negative amount** = YOU paid client (your loss from client profit)

---

## Report Types

### 1. Overview Report (`report_overview`)
**URL**: `/reports/`
**Purpose**: High-level reporting screen with totals, graphs, and trends

**Features**:
- Overall totals (turnover, profit, loss)
- Daily trends (last 30 days)
- Weekly trends (last 4 weeks)
- Monthly trends (last 6 months)
- Transaction type breakdown
- My Profit vs Friend Profit split
- Time travel support
- Client filtering

### 2. Daily Report (`report_daily`)
**URL**: `/reports/daily/`
**Purpose**: Detailed report for a specific date

**Features**:
- Date-specific totals
- Transaction type breakdown
- Client-wise breakdown
- Net profit and profit margin
- Transaction list for the day

### 3. Weekly Report (`report_weekly`)
**URL**: `/reports/weekly/`
**Purpose**: Report for a specific week (Monday to Sunday)

**Features**:
- Week totals
- Daily breakdown within the week
- Transaction type analysis
- Average daily turnover
- Net profit and profit margin

### 4. Monthly Report (`report_monthly`)
**URL**: `/reports/monthly/`
**Purpose**: Report for a specific month

**Features**:
- Month totals
- Weekly breakdown within the month
- Top clients by profit
- Transaction type breakdown
- Average daily turnover
- Net profit and profit margin

### 5. Custom Report (`report_custom`)
**URL**: `/reports/custom/`
**Purpose**: Report for a custom date range

**Features**:
- User-defined start and end dates
- Totals for the period
- Transaction list
- Defaults to last 30 days if not specified

### 6. Client Report (`report_client`)
**URL**: `/reports/client/<pk>/`
**Purpose**: Report for a specific client

**Features**:
- Client-specific totals
- Optional date range filtering
- Transaction list for the client
- All exchanges for the client

### 7. Exchange Report (`report_exchange`)
**URL**: `/reports/exchange/<pk>/`
**Purpose**: Report for a specific exchange

**Features**:
- Exchange-specific totals
- Daily/Weekly/Monthly period selection
- Custom date range support
- Client-wise breakdown
- Transaction type breakdown
- Transaction list

### 8. Time Travel Report (`report_time_travel`)
**URL**: `/reports/time-travel/`
**Purpose**: View historical data as of a specific date or date range

**Features**:
- Single date mode (up to a date)
- Date range mode (between two dates)
- Historical totals
- Pending amounts calculation
- Recent transactions list

---

## Core Concepts

### Transaction Types

The system uses the following transaction types:

1. **RECORD_PAYMENT**: Settlement payments (primary source for profit/loss)
2. **FUNDING**: Money given to clients
3. **TRADE**: Trading activity (audit only)
4. **FEE**: Fee transactions (audit only)
5. **ADJUSTMENT**: Balance adjustments (audit only)

**Note**: Only `RECORD_PAYMENT` transactions are used for profit/loss calculations in reports.

### Sign Convention

**RECORD_PAYMENT Transaction Amount**:
- **Positive (+X)**: Client paid YOU
  - Occurs when Client_PnL < 0 (client in loss)
  - Represents your profit from client's loss
- **Negative (-X)**: YOU paid client
  - Occurs when Client_PnL > 0 (client in profit)
  - Represents your loss (payment to client)

**Formula**:
```
IF Client_PnL < 0 (LOSS):
    Transaction.amount = +SharePayment  (client pays you)
ELSE IF Client_PnL > 0 (PROFIT):
    Transaction.amount = -SharePayment  (you pay client)
```

### Profit vs Loss in Reports

**Your Profit**:
- Sum of positive RECORD_PAYMENT amounts
- Money received from clients (when they had losses)

**Your Loss**:
- Absolute value of negative RECORD_PAYMENT amounts
- Money paid to clients (when they had profits)

**Net Profit**:
```
NetProfit = YourProfit - YourLoss
```

---

## Data Sources

### Primary Data Source: Transaction Model

**Fields Used**:
- `type`: Transaction type (must be 'RECORD_PAYMENT' for profit/loss)
- `amount`: Signed amount (positive = client paid you, negative = you paid client)
- `date`: Transaction date (for filtering)
- `client_exchange`: Link to ClientExchangeAccount
- `notes`: Optional notes

### Filtering

**Base Filter** (applied to all reports):
```python
base_filter = {
    "client_exchange__client__user": request.user
}
```

**Date Filtering**:
- Single date: `date=report_date`
- Date range: `date__gte=start_date, date__lte=end_date`
- Up to date: `date__lte=as_of_date`

**Transaction Type Filtering**:
```python
payment_qs = Transaction.objects.filter(
    type='RECORD_PAYMENT',
    **base_filter,
    **date_filter
)
```

---

## Profit/Loss Calculation Logic

### Your Total Profit

**Formula**:
```python
your_total_profit = Sum(RECORD_PAYMENT.amount)
```

**Explanation**:
- Sum all RECORD_PAYMENT transaction amounts
- Positive amounts = profit (client paid you)
- Negative amounts = loss (you paid client)
- Net result = total profit (can be negative)

**Code**:
```python
payment_qs = Transaction.objects.filter(
    client_exchange__client__user=request.user,
    type='RECORD_PAYMENT'
)
# Apply date filter if specified
if date_filter:
    payment_qs = payment_qs.filter(**date_filter)

your_total_profit = payment_qs.aggregate(
    total=Sum("amount")
)["total"] or Decimal(0)
```

### Income from Clients

**Formula**:
```python
your_total_income_from_clients = Sum(RECORD_PAYMENT.amount WHERE amount > 0)
```

**Explanation**:
- Only positive amounts (client paid you)
- Represents money received from clients

**Code**:
```python
your_total_income_from_clients = payment_qs.filter(
    amount__gt=0
).aggregate(
    total=Sum("amount")
)["total"] or Decimal(0)
```

### Paid to Clients

**Formula**:
```python
your_total_paid_to_clients = ABS(Sum(RECORD_PAYMENT.amount WHERE amount < 0))
```

**Explanation**:
- Only negative amounts (you paid client)
- Absolute value for display (always positive)

**Code**:
```python
your_total_paid_to_clients = abs(
    payment_qs.filter(amount__lt=0).aggregate(
        total=Sum("amount")
    )["total"] or Decimal(0)
)
```

### My Profit and Friend Profit Split

**Purpose**: Split profit between "My Own" and "Friend" portions based on report config.

**Formula**:
```python
my_profit = payment_amount × (my_own_percentage / my_percentage)
friend_profit = payment_amount × (friend_percentage / my_percentage)
```

**Identity**:
```
payment_amount == my_profit + friend_profit
```

**Logic**:
1. For each RECORD_PAYMENT transaction:
   - Get `ClientExchangeAccount.my_percentage` (total percentage)
   - Get `ClientExchangeReportConfig` (if exists)
   - Extract `my_own_percentage` and `friend_percentage`
   - Split payment amount proportionally
2. Aggregate all splits to get totals

**Code**:
```python
my_profit_total = Decimal(0)
friend_profit_total = Decimal(0)

for tx in payment_transactions:
    payment_amount = Decimal(str(tx.amount))  # Can be positive or negative
    account = tx.client_exchange
    my_total_pct = Decimal(str(account.my_percentage))
    
    if my_total_pct == 0:
        continue
    
    report_config = getattr(account, 'report_config', None)
    
    if report_config:
        my_own_pct = Decimal(str(report_config.my_own_percentage))
        friend_pct = Decimal(str(report_config.friend_percentage))
        
        my_profit_part = payment_amount * my_own_pct / my_total_pct
        friend_profit_part = payment_amount * friend_pct / my_total_pct
    else:
        # No report config: all goes to me
        my_profit_part = payment_amount
        friend_profit_part = Decimal(0)
    
    my_profit_total += my_profit_part
    friend_profit_total += friend_profit_part
```

**Example**:
- Payment = +100 (client paid you)
- My Total % = 10
- My Own % = 6, Friend % = 4
- My Profit = 100 × 6 / 10 = 60
- Friend Profit = 100 × 4 / 10 = 40
- Verification: 60 + 40 = 100 ✓

**Negative Payment Example**:
- Payment = -50 (you paid client)
- My Total % = 10
- My Own % = 6, Friend % = 4
- My Profit = -50 × 6 / 10 = -30
- Friend Profit = -50 × 4 / 10 = -20
- Verification: -30 + (-20) = -50 ✓

---

## Turnover Calculation

### Turnover Definition

**CORRECTNESS LOGIC**: Turnover = Cash Turnover (Option A - Recommended)

**Formula**:
```python
total_turnover = Sum(|RECORD_PAYMENT.amount|)
```

**Explanation**:
- Sum of absolute values of all RECORD_PAYMENT transactions
- Represents total cash movement (both directions)
- Only actual cash movements, not trading activity
- **CRITICAL**: Must use absolute values, not signed sum

**Code**:
```python
from django.db.models.functions import Abs

turnover_qs = base_qs.filter(type='RECORD_PAYMENT')
total_turnover = turnover_qs.aggregate(
    total=Sum(Abs("amount"))
)["total"] or 0
```

**Why Absolute Values?**
- Signed sum gives NET cash flow (profit/loss), not turnover
- Example: +100 and -90 → Turnover = 190, not 10
- Turnover measures total activity, not net result

**Important**: Turnover ALWAYS uses absolute values. Net sums are NOT turnover.

---

## Report Views Detailed

### 1. Overview Report (`report_overview`)

**Purpose**: High-level dashboard with comprehensive analytics.

**Key Calculations**:

1. **Total Turnover**:
   ```python
   turnover_qs = base_qs.filter(type='RECORD_PAYMENT')
   total_turnover = turnover_qs.aggregate(total=Sum("amount"))["total"] or 0
   ```

2. **Your Total Profit**:
   ```python
   payment_qs = Transaction.objects.filter(
       client_exchange__client__user=request.user,
       type='RECORD_PAYMENT'
   )
   # Apply filters...
   your_total_profit = payment_qs.aggregate(total=Sum("amount"))["total"] or Decimal(0)
   ```

3. **Income from Clients**:
   ```python
   your_total_income_from_clients = payment_qs.filter(
       amount__gt=0
   ).aggregate(total=Sum("amount"))["total"] or Decimal(0)
   ```

4. **Paid to Clients**:
   ```python
   your_total_paid_to_clients = abs(
       payment_qs.filter(amount__lt=0).aggregate(
           total=Sum("amount")
       )["total"] or Decimal(0)
   )
   ```

5. **My Profit and Friend Profit**:
   - Iterate through payment transactions
   - Split based on report config percentages
   - Aggregate totals

**Date Range Options**:
- **Monthly**: Default, shows selected month
- **Time Travel**: Custom start/end dates or "as of" date
- **Month Selection**: Dropdown to select specific month

**Charts Generated**:
- Daily trends (last 30 days)
- Weekly trends (last 4 weeks)
- Monthly trends (last 6 months)
- Transaction type breakdown
- Top clients (if applicable)

**Filtering**:
- Client filter (optional)
- Date range filter
- Time travel mode

### 2. Daily Report (`report_daily`)

**Purpose**: Detailed analysis for a specific date.

**Key Calculations**:

1. **Date Filter**:
   ```python
   report_date = date.fromisoformat(report_date_str)
   base_filter = {
       "client_exchange__client__user": request.user,
       "date": report_date
   }
   ```

2. **Totals** (Using RECORD_PAYMENT transactions):
   ```python
   qs = Transaction.objects.filter(**base_filter)
   payment_qs = qs.filter(type='RECORD_PAYMENT')
   
   # Turnover = sum of absolute values
   total_turnover = payment_qs.aggregate(
       total=Sum(Abs("amount"))
   )["total"] or 0
   
   # Your Total Profit = signed sum
   your_total_profit = payment_qs.aggregate(
       total=Sum("amount")
   )["total"] or Decimal(0)
   
   # Income from clients = positive amounts
   your_profit = payment_qs.filter(
       amount__gt=0
   ).aggregate(total=Sum("amount"))["total"] or Decimal(0)
   
   # Loss (paid to clients) = absolute value of negative amounts
   your_loss = abs(
       payment_qs.filter(amount__lt=0).aggregate(
           total=Sum("amount")
       )["total"] or Decimal(0)
   )
   ```

**Note**: Daily report has been updated to use RECORD_PAYMENT transactions (same as overview report).

**Charts**:
- Transaction type breakdown (pie chart)
- Client-wise breakdown (bar chart)

**Metrics**:
- Net profit = your_profit - your_loss
- Profit margin = (your_profit / total_turnover) × 100

### 3. Weekly Report (`report_weekly`)

**Purpose**: Analysis for a specific week (Monday to Sunday).

**Key Calculations**:

1. **Week Range**:
   ```python
   today = date.today()
   days_since_monday = today.weekday()
   week_start = today - timedelta(days=days_since_monday)
   week_end = week_start + timedelta(days=6)
   ```

2. **Daily Breakdown**:
   ```python
   for i in range(7):
       current_date = week_start + timedelta(days=i)
       day_qs = qs.filter(date=current_date)
       # Calculate day totals...
   ```

**Charts**:
- Daily breakdown within week (line chart)
- Transaction type breakdown (pie chart)

**Metrics**:
- Net profit
- Profit margin
- Average daily turnover = total_turnover / 7

### 4. Monthly Report (`report_monthly`)

**Purpose**: Analysis for a specific month.

**Key Calculations**:

1. **Month Range**:
   ```python
   month_start = date(year, month, 1)
   if month == 12:
       month_end = date(year, 12, 31)
   else:
       month_end = date(year, month + 1, 1) - timedelta(days=1)
   ```

2. **Weekly Breakdown**:
   ```python
   current_date = month_start
   week_num = 1
   while current_date <= month_end:
       week_end_date = min(current_date + timedelta(days=6), month_end)
       # Calculate week totals...
       current_date = week_end_date + timedelta(days=1)
       week_num += 1
   ```

**Charts**:
- Weekly breakdown within month (bar chart)
- Transaction type breakdown (pie chart)
- Top clients by profit (bar chart)

**Metrics**:
- Net profit
- Profit margin
- Average daily turnover = total_turnover / days_in_month

### 5. Custom Report (`report_custom`)

**Purpose**: User-defined date range analysis.

**Key Calculations**:

1. **Date Range**:
   ```python
   if start_date_str and end_date_str:
       start_date = date.fromisoformat(start_date_str)
       end_date = date.fromisoformat(end_date_str)
   else:
       # Default to last 30 days
       end_date = date.today()
       start_date = end_date - timedelta(days=30)
   ```

2. **Totals**:
   ```python
   qs = Transaction.objects.filter(
       client_exchange__client__user=request.user,
       date__gte=start_date,
       date__lte=end_date
   )
   ```

**Features**:
- Simple totals display
- Transaction list
- No charts (simplified view)

### 6. Client Report (`report_client`)

**Purpose**: Analysis for a specific client across all exchanges.

**Key Calculations**:

1. **Client Filter**:
   ```python
   client = get_object_or_404(Client, pk=client_pk, user=request.user)
   qs = Transaction.objects.filter(
       client_exchange__client=client
   )
   ```

2. **Optional Date Range**:
   ```python
   if start_date_str and end_date_str:
       qs = qs.filter(
           date__gte=start_date,
           date__lte=end_date
       )
   ```

**Features**:
- Client-specific totals
- All transactions for the client
- All exchanges included
- Optional date filtering

### 7. Exchange Report (`report_exchange`)

**Purpose**: Analysis for a specific exchange across all clients.

**Key Calculations**:

1. **Exchange Filter**:
   ```python
   exchange = get_object_or_404(Exchange, pk=exchange_pk)
   qs = Transaction.objects.filter(
       client_exchange__client__user=request.user,
       client_exchange__exchange=exchange,
       date__gte=start_date,
       date__lte=end_date
   )
   ```

2. **Period Selection**:
   - Daily: Today
   - Weekly: Last 7 days
   - Monthly: Last month (same day to same day)
   - Custom: User-defined range

**Charts**:
- Transaction type breakdown
- Client-wise breakdown

**Metrics**:
- Net profit
- Profit margin

### 8. Time Travel Report (`report_time_travel`)

**Purpose**: View historical data as of a specific point in time.

**Modes**:

1. **Single Date Mode** (Legacy):
   ```python
   as_of = date.fromisoformat(as_of_str)
   qs = Transaction.objects.filter(
       **base_filter,
       date__lte=as_of
   )
   ```

2. **Date Range Mode**:
   ```python
   start_date = date.fromisoformat(start_date_str)
   end_date = date.fromisoformat(end_date_str)
   qs = Transaction.objects.filter(
       **base_filter,
       date__gte=start_date,
       date__lte=end_date
   )
   ```

**Features**:
- Historical totals
- Pending amounts calculation (as of date)
- Recent transactions list
- Useful for auditing and historical analysis

---

## Time Travel Reporting

### Purpose

View data as it existed at a specific point in time, useful for:
- Historical audits
- Point-in-time analysis
- Comparing periods
- Understanding trends

### Implementation

**Single Date Mode**:
- Shows all transactions up to and including the selected date
- Totals reflect state as of that date
- Useful for "as of" reporting

**Date Range Mode**:
- Shows transactions between two dates
- Totals reflect activity in that period
- Useful for period comparison

### Date Filter Conversion

**Issue**: Transaction.date is DateTimeField, but filters use date objects.

**Solution**:
```python
if 'date__gte' in date_filter:
    date_gte = date_filter['date__gte']
    if isinstance(date_gte, date):
        filter_dict['date__gte'] = timezone.make_aware(
            datetime.combine(date_gte, datetime.min.time())
        )

if 'date__lte' in date_filter:
    date_lte = date_filter['date__lte']
    if isinstance(date_lte, date):
        filter_dict['date__lte'] = timezone.make_aware(
            datetime.combine(date_lte, datetime.max.time())
        )
```

---

## Client Exchange Report Config

### Purpose

Split profit between "My Own" and "Friend" portions for reporting purposes.

### Model: `ClientExchangeReportConfig`

**Fields**:
- `client_exchange`: OneToOneField to ClientExchangeAccount
- `friend_percentage`: INT (friend's share percentage)
- `my_own_percentage`: INT (your own share percentage)

**Validation**:
```python
friend_percentage + my_own_percentage == client_exchange.my_percentage
```

### Usage in Reports

**When Report Config Exists**:
```python
my_profit = payment_amount × (my_own_percentage / my_percentage)
friend_profit = payment_amount × (friend_percentage / my_percentage)
```

**When Report Config Doesn't Exist**:
```python
my_profit = payment_amount  # All goes to you
friend_profit = 0
```

### Creation

Report config is created when linking client to exchange:
```python
if friend_percentage or my_own_percentage:
    if friend_pct + own_pct == my_percentage_int:
        ClientExchangeReportConfig.objects.create(
            client_exchange=account,
            friend_percentage=friend_pct,
            my_own_percentage=own_pct,
        )
```

---

## Export Functionality

### CSV Export (`export_report_csv`)

**URL**: `/reports/export/`

**Parameters**:
- `type`: Report type filter ("profit", "loss", or "all")
- `start_date`: Start date (optional)
- `end_date`: End date (optional)

**Output Format**:
```csv
Date,Client,Exchange,Type,Amount,Your Share,Client Share,Company Share,Note
2024-01-15,Client A,Exchange X,RECORD_PAYMENT,1000,100,900,0,Payment recorded
```

**Fields**:
- Date: Transaction date
- Client: Client name
- Exchange: Exchange name
- Type: Transaction type
- Amount: Transaction amount (signed)
- Your Share: Your share amount (if applicable)
- Client Share: Client share amount (if applicable)
- Company Share: Company share (always 0, legacy)
- Note: Transaction notes

**Code**:
```python
response = HttpResponse(content_type="text/csv")
response["Content-Disposition"] = f'attachment; filename="report_{date.today()}.csv"'

writer = csv.writer(response)
writer.writerow(["Date", "Client", "Exchange", "Type", "Amount", ...])

for tx in qs:
    writer.writerow([
        tx.date,
        tx.client_exchange.client.name,
        tx.client_exchange.exchange.name,
        tx.get_transaction_type_display(),
        tx.amount,
        # ... other fields
    ])
```

---

## Charts and Visualizations

### Chart Types

1. **Line Charts**: Daily/weekly trends
2. **Bar Charts**: Transaction types, clients, weeks
3. **Pie Charts**: Transaction type breakdown

### Data Preparation

**Daily Trends** (Last 30 days):
```python
daily_data = defaultdict(lambda: {"profit": 0, "loss": 0, "turnover": 0})

daily_payments = base_qs.filter(
    type='RECORD_PAYMENT',
    date__gte=start_date,
    date__lte=end_date
).values("date").annotate(
    profit_sum=Sum("amount")
)

for item in daily_payments:
    tx_date = item['date']
    profit_amount = float(item["profit_sum"] or 0)
    if profit_amount > 0:
        daily_data[tx_date]["profit"] += profit_amount
    elif profit_amount < 0:
        daily_data[tx_date]["loss"] += abs(profit_amount)
```

**Weekly Trends** (Last 4 weeks):
```python
for i in range(4):
    week_start = week_end - timedelta(days=6)
    week_transactions = base_qs.filter(
        date__gte=week_start,
        date__lte=week_end
    )
    week_payments = week_transactions.filter(type='RECORD_PAYMENT')
    # Calculate totals...
    week_end = week_start - timedelta(days=1)
```

**Monthly Trends** (Last 6 months):
```python
for i in range(6):
    month_date = today.replace(day=1)
    # Calculate previous month...
    month_transactions = base_qs.filter(
        date__gte=month_date,
        date__lte=month_end
    )
    # Calculate totals...
```

**Transaction Type Breakdown**:
```python
type_breakdown = base_qs.values("type").annotate(
    count=Count("id"),
    total_amount=Sum("amount")
)

type_map = {
    'FUNDING': ("Funding", "#4b5563"),
    'TRADE': ("Trade", "#6b7280"),
    'FEE': ("Fee", "#9ca3af"),
    'ADJUSTMENT': ("Adjustment", "#6b7280"),
    'RECORD_PAYMENT': ("Record Payment", "#10b981"),
}
```

### JSON Encoding

All chart data is JSON-encoded for JavaScript consumption:
```python
context = {
    "daily_labels": json.dumps(date_labels),
    "daily_profit": json.dumps(profit_data),
    "daily_loss": json.dumps(loss_data),
    # ...
}
```

---

## Filtering and Date Ranges

### Base Filters

**User Filter** (always applied):
```python
base_filter = {"client_exchange__client__user": request.user}
```

**Client Filter** (optional):
```python
if client_id:
    base_filter["client_exchange__client_id"] = client_id
```

**Exchange Filter** (for exchange reports):
```python
base_filter["client_exchange__exchange"] = exchange
```

### Date Range Calculations

**Daily**:
```python
start_date = today
end_date = today
```

**Weekly**:
```python
days_since_monday = today.weekday()
week_start = today - timedelta(days=days_since_monday)
week_end = week_start + timedelta(days=6)
```

**Monthly** (same day to same day):
```python
day_of_month = today.day
if today.month == 1:
    last_month = 12
    last_year = today.year - 1
else:
    last_month = today.month - 1
    last_year = today.year

last_month_days = (date(today.year, today.month, 1) - timedelta(days=1)).day
start_date = date(last_year, last_month, min(day_of_month, last_month_days))
end_date = today
```

**Custom Range**:
```python
start_date = date.fromisoformat(start_date_str)
end_date = date.fromisoformat(end_date_str)
```

### Date Filter Application

```python
if date_filter:
    base_qs = base_qs.filter(**date_filter)
```

**Date Filter Dictionary**:
```python
date_filter = {
    "date__gte": start_date,  # Greater than or equal
    "date__lte": end_date,    # Less than or equal
}
```

**Single Date**:
```python
date_filter = {"date": report_date}
```

**Up to Date** (Time Travel):
```python
date_filter = {"date__lte": as_of_date}
```

---

## Key Methods Reference

### `report_overview(request)`

**Purpose**: Generate overview report with comprehensive analytics.

**Key Steps**:
1. Get report type and date parameters
2. Build base queryset with filters
3. Calculate totals (turnover, profit, loss)
4. Calculate My Profit and Friend Profit splits
5. Generate daily/weekly/monthly trend data
6. Generate chart data
7. Render template

**Returns**: HttpResponse with rendered overview template

**Location**: `core.views.report_overview()`

---

### `report_daily(request)`

**Purpose**: Generate daily report for a specific date.

**Key Steps**:
1. Get report date (defaults to today)
2. Filter transactions for that date
3. Calculate totals
4. Generate chart data
5. Render template

**Returns**: HttpResponse with rendered daily template

**Location**: `core.views.report_daily()`

---

### `report_weekly(request)`

**Purpose**: Generate weekly report for a specific week.

**Key Steps**:
1. Calculate week start (Monday) and end (Sunday)
2. Filter transactions for the week
3. Calculate daily breakdown
4. Generate chart data
5. Render template

**Returns**: HttpResponse with rendered weekly template

**Location**: `core.views.report_weekly()`

---

### `report_monthly(request)`

**Purpose**: Generate monthly report for a specific month.

**Key Steps**:
1. Get month parameter (defaults to current month)
2. Calculate month start and end dates
3. Filter transactions for the month
4. Calculate weekly breakdown
5. Generate chart data
6. Render template

**Returns**: HttpResponse with rendered monthly template

**Location**: `core.views.report_monthly()`

---

### `report_custom(request)`

**Purpose**: Generate custom period report.

**Key Steps**:
1. Get start and end dates (defaults to last 30 days)
2. Filter transactions for the range
3. Calculate totals
4. Render template

**Returns**: HttpResponse with rendered custom template

**Location**: `core.views.report_custom()`

---

### `report_client(request, client_pk)`

**Purpose**: Generate report for a specific client.

**Key Steps**:
1. Get client (404 if not found)
2. Get optional date range
3. Filter transactions for client
4. Calculate totals
5. Render template

**Returns**: HttpResponse with rendered client template

**Location**: `core.views.report_client()`

---

### `report_exchange(request, exchange_pk)`

**Purpose**: Generate report for a specific exchange.

**Key Steps**:
1. Get exchange (404 if not found)
2. Get report type (daily/weekly/monthly) or custom range
3. Calculate date range based on report type
4. Filter transactions for exchange
5. Calculate totals and breakdowns
6. Generate chart data
7. Render template

**Returns**: HttpResponse with rendered exchange template

**Location**: `core.views.report_exchange()`

---

### `report_time_travel(request)`

**Purpose**: Generate time travel report (historical view).

**Key Steps**:
1. Get date parameters (start/end or as_of)
2. Determine mode (range or single date)
3. Filter transactions accordingly
4. Calculate historical totals
5. Calculate pending amounts (as of date)
6. Render template

**Returns**: HttpResponse with rendered time travel template

**Location**: `core.views.report_time_travel()`

---

### `export_report_csv(request)`

**Purpose**: Export report data as CSV.

**Key Steps**:
1. Get report type and date filters
2. Build queryset with filters
3. Create CSV response
4. Write header row
5. Write data rows
6. Return response

**Returns**: HttpResponse with CSV content

**Location**: `core.views.export_report_csv()`

---

## Summary

The Reports System provides comprehensive analytics based on RECORD_PAYMENT transactions:

1. **Single Source of Truth**: All profit/loss calculations use RECORD_PAYMENT transactions
2. **Sign Convention**: Positive = client paid you, Negative = you paid client
3. **Multiple Report Types**: Daily, Weekly, Monthly, Custom, Overview, Client, Exchange
4. **Time Travel**: Historical analysis support
5. **Profit Split**: My Profit vs Friend Profit based on report config
6. **Visualizations**: Charts and graphs for trends
7. **Export**: CSV export functionality

### Key Formulas

- **Your Total Profit**: `Sum(RECORD_PAYMENT.amount)`
- **Income from Clients**: `Sum(RECORD_PAYMENT.amount WHERE amount > 0)`
- **Paid to Clients**: `ABS(Sum(RECORD_PAYMENT.amount WHERE amount < 0))`
- **My Profit**: `payment_amount × (my_own_percentage / my_percentage)`
- **Friend Profit**: `payment_amount × (friend_percentage / my_percentage)`
- **Turnover**: `Sum(ABS(RECORD_PAYMENT.amount))` - ALWAYS uses absolute values

---

**End of Documentation**

