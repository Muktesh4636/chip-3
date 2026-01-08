# Views.py Fix Required

## Issue

The `core/views.py` file has been overwritten with old code that references non-existent `Transaction.TYPE_*` constants.

## Problem

The Transaction model only has:
- `TRANSACTION_TYPES` (list of tuples)
- No class attributes like `TYPE_PROFIT`, `TYPE_LOSS`, `TYPE_SETTLEMENT`, etc.

## Current Transaction Types

```python
TRANSACTION_TYPES = [
    ('FUNDING', 'Funding'),
    ('TRADE', 'Trade'),
    ('FEE', 'Fee'),
    ('ADJUSTMENT', 'Adjustment'),
    ('RECORD_PAYMENT', 'Record Payment'),
]
```

## Required Fixes

### 1. Replace all `Transaction.TYPE_*` references with string values:

- `Transaction.TYPE_PROFIT` → `'PROFIT'` (but this type doesn't exist - should be removed)
- `Transaction.TYPE_LOSS` → `'LOSS'` (but this type doesn't exist - should be removed)
- `Transaction.TYPE_SETTLEMENT` → `'SETTLEMENT'` (but this type doesn't exist - should be removed)
- `Transaction.TYPE_FUNDING` → `'FUNDING'`
- `Transaction.TYPE_BALANCE_RECORD` → Remove (doesn't exist)

### 2. Dashboard View

The dashboard should compute values from accounts, not transactions:

```python
@login_required
def dashboard(request):
    """Dashboard view showing system overview."""
    accounts = ClientExchangeAccount.objects.select_related('client', 'exchange').all()
    
    # Compute totals
    total_funding = sum(acc.funding for acc in accounts)
    total_exchange_balance = sum(acc.exchange_balance for acc in accounts)
    total_client_pnl = sum(acc.compute_client_pnl() for acc in accounts)
    total_my_share = sum(acc.compute_my_share() for acc in accounts)
    
    # Counts
    total_clients = Client.objects.count()
    total_exchanges = Exchange.objects.count()
    total_accounts = accounts.count()
    
    # Recent accounts with PnL
    recent_accounts = accounts.order_by('-updated_at')[:10]
    
    context = {
        'total_clients': total_clients,
        'total_exchanges': total_exchanges,
        'total_accounts': total_accounts,
        'total_funding': total_funding,
        'total_exchange_balance': total_exchange_balance,
        'total_client_pnl': total_client_pnl,
        'total_my_share': total_my_share,
        'recent_accounts': recent_accounts,
    }
    return render(request, "core/dashboard.html", context)
```

### 3. Remove Old Code

The views.py file contains many old functions and views that don't match the PIN-TO-PIN implementation:
- Remove transaction-based profit/loss calculations
- Remove settlement tracking code
- Remove pending amount calculations from transactions
- Keep only: client management, exchange management, account management, funding, balance updates, record payment, pending summary

## Immediate Fix

To fix the AttributeError, replace line 259:

**Before:**
```python
transactions_qs.filter(transaction_type=Transaction.TYPE_PROFIT)
```

**After:**
```python
# Profit computed from accounts, not transactions
# Remove this line or replace with account-based calculation
```

## Recommendation

The views.py file needs a complete rewrite to match the PIN-TO-PIN implementation. The current file has 4114 lines of old code that doesn't match the new system design.

