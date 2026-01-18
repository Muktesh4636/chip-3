from rest_framework import viewsets, permissions, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework.authtoken.models import Token
from django.contrib.auth import authenticate
from django.db.models import Sum, Q
from django.utils import timezone
from decimal import Decimal
from datetime import date, timedelta
from .models import Client, Exchange, ClientExchangeAccount, Transaction, ClientExchangeReportConfig
from .serializers import (
    ClientSerializer, ExchangeSerializer, 
    ClientExchangeAccountSerializer, TransactionSerializer
)

@api_view(['GET', 'POST'])
@permission_classes([permissions.IsAuthenticated])
def api_account_report_config(request, account_id):
    try:
        account = ClientExchangeAccount.objects.get(id=account_id, client__user=request.user)
        config, created = ClientExchangeReportConfig.objects.get_or_create(client_exchange=account)
        
        if request.method == 'POST':
            friend_pct = Decimal(str(request.data.get('friend_percentage', config.friend_percentage)))
            my_own_pct = Decimal(str(request.data.get('my_own_percentage', config.my_own_percentage)))
            
            # Update values
            config.friend_percentage = friend_pct
            config.my_own_percentage = my_own_pct
            config.save()
            return Response({'status': 'success'})
            
        return Response({
            'friend_percentage': float(config.friend_percentage),
            'my_own_percentage': float(config.my_own_percentage),
            'my_total_percentage': float(account.profit_share_percentage) # Use profit share as total
        })
    except Exception as e:
        return Response({'error': str(e)}, status=400)

@api_view(['POST'])
@permission_classes([])  # Allow unauthenticated access for login
def api_login(request):
    """API endpoint for mobile app login"""
    username = request.data.get('username')
    password = request.data.get('password')
    
    if not username or not password:
        return Response(
            {'error': 'Username and password required'}, 
            status=status.HTTP_400_BAD_REQUEST
        )
    
    user = authenticate(username=username, password=password)
    if user:
        token, created = Token.objects.get_or_create(user=user)
        return Response({
            'token': token.key,
            'user_id': user.id,
            'username': user.username
        })
    else:
        return Response(
            {'error': 'Invalid credentials'}, 
            status=status.HTTP_401_UNAUTHORIZED
        )

@api_view(['GET'])
@permission_classes([permissions.IsAuthenticated])
def mobile_dashboard_summary(request):
    accounts = ClientExchangeAccount.objects.filter(client__user=request.user)
    
    total_funding = accounts.aggregate(Sum('funding'))['funding__sum'] or 0
    total_balance = accounts.aggregate(Sum('exchange_balance'))['exchange_balance__sum'] or 0
    
    # Calculate PnL and share
    total_pnl = 0
    total_my_share = 0
    for account in accounts:
        total_pnl += account.compute_client_pnl()
        total_my_share += account.compute_my_share()
    
    return Response({
        "total_clients": Client.objects.filter(user=request.user).count(),
        "total_exchanges": Exchange.objects.count(),
        "total_accounts": accounts.count(),
        "total_funding": total_funding,
        "total_balance": total_balance,
        "total_pnl": total_pnl,
        "total_my_share": total_my_share,
        "currency": "INR"
    })

@api_view(['GET'])
@permission_classes([permissions.IsAuthenticated])
def api_pending_payments(request):
    """API endpoint for pending payments"""
    accounts = ClientExchangeAccount.objects.filter(client__user=request.user)
    
    pending_list = []
    total_to_receive = 0
    total_to_pay = 0
    
    for acc in accounts:
        client_pnl = acc.compute_client_pnl()
        if client_pnl == 0:
            continue
            
        my_share = acc.compute_my_share()
        
        item = {
            'account_id': acc.id,
            'client_name': acc.client.name,
            'exchange_name': acc.exchange.name,
            'pnl': client_pnl,
            'my_share': my_share,
            'type': 'RECEIVE' if client_pnl < 0 else 'PAY'
        }
        
        if client_pnl < 0:
            total_to_receive += abs(my_share)
        else:
            total_to_pay += abs(my_share)
            
        pending_list.append(item)
        
    return Response({
        'pending_payments': pending_list,
        'total_to_receive': total_to_receive,
        'total_to_pay': total_to_pay,
        'currency': 'INR'
    })

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_add_funding(request, account_id):
    try:
        account = ClientExchangeAccount.objects.get(id=account_id, client__user=request.user)
        amount_raw = str(request.data.get('amount', '0'))
        amount = int(Decimal(amount_raw.replace(',', '')))
        notes = request.data.get('notes', '')
        
        account.funding += amount
        account.exchange_balance += amount
        account.save()
        
        Transaction.objects.create(
            client_exchange=account,
            date=timezone.now(),
            type='FUNDING',
            amount=amount,
            funding_after=account.funding,
            exchange_balance_after=account.exchange_balance,
            notes=notes
        )
        return Response({'status': 'success', 'new_balance': account.exchange_balance})
    except Exception as e:
        print(f"DEBUG API FUNDING ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_update_balance(request, account_id):
    try:
        account = ClientExchangeAccount.objects.get(id=account_id, client__user=request.user)
        amount_raw = str(request.data.get('amount', '0'))
        new_balance = int(Decimal(amount_raw.replace(',', '')))
        notes = request.data.get('notes', '')
        
        account.exchange_balance = new_balance
        account.save()
        
        Transaction.objects.create(
            client_exchange=account,
            date=timezone.now(),
            type='UPDATE_BALANCE',
            amount=new_balance,
            funding_after=account.funding,
            exchange_balance_after=account.exchange_balance,
            notes=notes
        )
        return Response({'status': 'success'})
    except Exception as e:
        print(f"DEBUG API BALANCE ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_record_payment(request, account_id):
    try:
        account = ClientExchangeAccount.objects.get(id=account_id, client__user=request.user)
        amount_raw = str(request.data.get('amount', '0'))
        amount = int(Decimal(amount_raw.replace(',', '')))
        notes = request.data.get('notes', '')
        
        # Payment decreases balance
        account.exchange_balance -= amount
        account.save()
        
        Transaction.objects.create(
            client_exchange=account,
            date=timezone.now(),
            type='RECORD_PAYMENT',
            amount=amount,
            funding_after=account.funding,
            exchange_balance_after=account.exchange_balance,
            notes=notes
        )
        return Response({'status': 'success'})
    except Exception as e:
        print(f"DEBUG API PAYMENT ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['GET'])
@permission_classes([permissions.IsAuthenticated])
def api_reports_summary(request):
    """Real business reports for mobile with period filtering"""
    period = request.query_params.get('period', 'DAILY')
    accounts = ClientExchangeAccount.objects.filter(client__user=request.user)
    today = timezone.now().date()
    
    start_date = today
    if period == 'WEEKLY':
        start_date = today - timedelta(days=7)
    elif period == 'MONTHLY':
        start_date = today - timedelta(days=30)
    
    # Filter transactions for stats if needed, or just return overview
    total_funding = accounts.aggregate(Sum('funding'))['funding__sum'] or 0
    total_balance = accounts.aggregate(Sum('exchange_balance'))['exchange_balance__sum'] or 0
    total_pnl = sum(acc.compute_client_pnl() for acc in accounts)
    total_my_share = sum(acc.compute_my_share() for acc in accounts)
    
    # NEW: Split of My Share (My Own vs Friend/Student)
    my_own_total_share = 0
    friend_total_share = 0
    
    for acc in accounts:
        try:
            config = ClientExchangeReportConfig.objects.get(client_exchange=acc)
            acc_my_share = acc.compute_my_share()
            if acc_my_share > 0:
                # Calculate ratio based on config
                total_config_pct = float(config.my_own_percentage + config.friend_percentage)
                if total_config_pct > 0:
                    my_own_total_share += int((acc_my_share * float(config.my_own_percentage)) / total_config_pct)
                    friend_total_share += int((acc_my_share * float(config.friend_percentage)) / total_config_pct)
                else:
                    # Default if no config
                    my_own_total_share += acc_my_share
        except ClientExchangeReportConfig.DoesNotExist:
            # Default if no config
            my_own_total_share += acc.compute_my_share()

    # Recent Daily Performance (last 7 days)
    daily_stats = []
    for i in range(7):
        day = today - timedelta(days=i)
        day_txns = Transaction.objects.filter(
            client_exchange__client__user=request.user,
            date__date=day
        )
        
        day_pnl = 0
        for tx in day_txns:
            if tx.type == 'TRADE':
                if tx.exchange_balance_before is not None and tx.exchange_balance_after is not None:
                    day_pnl += (tx.exchange_balance_after - tx.exchange_balance_before)
            elif tx.type == 'SETTLEMENT_SHARE' or tx.type == 'RECORD_PAYMENT':
                # These are payments, not trading PnL
                pass
        
        if day_txns.exists():
            daily_stats.append({
                'date': day.strftime('%Y-%m-%d'),
                'pnl': day_pnl,
                'tx_count': day_txns.count()
            })

    return Response({
        'overview': {
            'total_funding': total_funding,
            'total_balance': total_balance,
            'total_pnl': total_pnl,
            'total_my_share': total_my_share,
            'my_own_share': my_own_total_share,
            'friend_share': friend_total_share,
        },
        'daily_performance': daily_stats,
        'period': period,
        'start_date': start_date
    })

@api_view(['GET'])
@permission_classes([permissions.IsAuthenticated])
def api_custom_reports(request):
    """Custom date range reports"""
    from_date_str = request.query_params.get('from_date')
    to_date_str = request.query_params.get('to_date')
    client_id = request.query_params.get('client_id')
    exchange_id = request.query_params.get('exchange_id')

    if not from_date_str or not to_date_str:
        return Response({'error': 'from_date and to_date are required'}, status=400)

    try:
        from_date = timezone.datetime.fromisoformat(from_date_str).date()
        to_date = timezone.datetime.fromisoformat(to_date_str).date()
    except ValueError:
        return Response({'error': 'Invalid date format. Use YYYY-MM-DD'}, status=400)

    # Base queryset
    accounts = ClientExchangeAccount.objects.filter(client__user=request.user)

    # Apply filters
    if client_id:
        accounts = accounts.filter(client_id=client_id)
    if exchange_id:
        accounts = accounts.filter(exchange_id=exchange_id)

    # Get transactions in date range
    transactions = Transaction.objects.filter(
        client_exchange__in=accounts,
        date__date__gte=from_date,
        date__date__lte=to_date
    ).select_related('client_exchange', 'client_exchange__client', 'client_exchange__exchange').order_by('-date')

    # Calculate summary stats
    total_funding = accounts.aggregate(Sum('funding'))['funding__sum'] or 0
    total_balance = accounts.aggregate(Sum('exchange_balance'))['exchange_balance__sum'] or 0
    total_pnl = sum(acc.compute_client_pnl() for acc in accounts)
    total_my_share = sum(acc.compute_my_share() for acc in accounts)

    # Split calculation (same as other reports)
    my_own_total_share = 0
    friend_total_share = 0

    for acc in accounts:
        try:
            config = ClientExchangeReportConfig.objects.get(client_exchange=acc)
            acc_my_share = acc.compute_my_share()
            if acc_my_share > 0:
                total_config_pct = float(config.my_own_percentage + config.friend_percentage)
                if total_config_pct > 0:
                    my_own_total_share += int((acc_my_share * float(config.my_own_percentage)) / total_config_pct)
                    friend_total_share += int((acc_my_share * float(config.friend_percentage)) / total_config_pct)
                else:
                    my_own_total_share += acc_my_share
        except ClientExchangeReportConfig.DoesNotExist:
            my_own_total_share += acc.compute_my_share()

    # Serialize transactions for mobile
    transaction_data = []
    for txn in transactions[:50]:  # Limit to 50 transactions
        transaction_data.append({
            'id': txn.id,
            'type_display': txn.get_type_display(),
            'client_name': txn.client_exchange.client.name,
            'exchange_name': txn.client_exchange.exchange.name,
            'date': txn.date.strftime('%Y-%m-%d %H:%M:%S'),
            'amount': txn.amount,
            'notes': txn.notes or ''
        })

    return Response({
        'overview': {
            'total_funding': total_funding,
            'total_balance': total_balance,
            'total_pnl': total_pnl,
            'total_my_share': total_my_share,
            'my_own_share': my_own_total_share,
            'friend_share': friend_total_share,
        },
        'transactions': transaction_data,
        'from_date': from_date_str,
        'to_date': to_date_str,
        'total_transactions': transactions.count()
    })

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_link_exchange(request):
    try:
        client_id = request.data.get('client_id')
        exchange_id = request.data.get('exchange_id')
        funding_raw = str(request.data.get('funding', '0'))
        initial_funding = int(Decimal(funding_raw.replace(',', '')))
        profit_share_percentage = int(request.data.get('profit_share_percentage', 0))
        loss_share_percentage = int(request.data.get('loss_share_percentage', 0))

        client = Client.objects.get(id=client_id, user=request.user)
        exchange = Exchange.objects.get(id=exchange_id)

        # Check if account already exists
        if ClientExchangeAccount.objects.filter(client=client, exchange=exchange).exists():
            return Response({'error': f'This client is already linked to {exchange.name}'}, status=400)

        account = ClientExchangeAccount.objects.create(
            client=client,
            exchange=exchange,
            funding=initial_funding,
            exchange_balance=initial_funding,
            profit_share_percentage=profit_share_percentage,
            loss_share_percentage=loss_share_percentage
        )
        
        Transaction.objects.create(
            client_exchange=account,
            date=timezone.now(),
            type='FUNDING',
            amount=initial_funding,
            funding_after=initial_funding,
            exchange_balance_after=initial_funding,
            notes='Initial account setup'
        )
        
        return Response({'status': 'success', 'account_id': account.id})
    except Exception as e:
        print(f"DEBUG API LINK ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_create_exchange(request):
    try:
        name = request.data.get('name')
        version = request.data.get('version', '')
        code = request.data.get('code', '')
        
        exchange = Exchange.objects.create(name=name, version_name=version, code=code)
        return Response({'status': 'success', 'id': exchange.id})
    except Exception as e:
        print(f"DEBUG API CREATE EXCHANGE ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_edit_transaction(request, pk):
    try:
        transaction = Transaction.objects.get(id=pk, client_exchange__client__user=request.user)
        amount_raw = str(request.data.get('amount', transaction.amount))
        amount = int(Decimal(amount_raw.replace(',', '')))
        notes = request.data.get('notes', transaction.notes)
        
        # We only allow editing amount and notes for simplicity
        transaction.amount = amount
        transaction.notes = notes
        transaction.save()
        
        return Response({'status': 'success'})
    except Exception as e:
        return Response({'error': str(e)}, status=400)

@api_view(['DELETE'])
@permission_classes([permissions.IsAuthenticated])
def api_delete_transaction(request, pk):
    try:
        from .views import transaction_delete_logic
        transaction = Transaction.objects.get(id=pk, client_exchange__client__user=request.user)
        transaction_delete_logic(transaction)
        return Response({'status': 'success'})
    except Exception as e:
        print(f"DEBUG API DELETE TXN ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['POST'])
@permission_classes([permissions.IsAuthenticated])
def api_update_account_settings(request, account_id):
    try:
        account = ClientExchangeAccount.objects.get(id=account_id, client__user=request.user)
        account.profit_share_percentage = int(Decimal(str(request.data.get('profit_share', account.profit_share_percentage))))
        account.loss_share_percentage = int(Decimal(str(request.data.get('loss_share', account.loss_share_percentage))))
        account.save()
        return Response({'status': 'success'})
    except Exception as e:
        print(f"DEBUG API SETTINGS ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['DELETE'])
@permission_classes([permissions.IsAuthenticated])
def api_delete_client(request, pk):
    try:
        client = Client.objects.get(id=pk, user=request.user)
        client.delete()
        return Response({'status': 'success'})
    except Exception as e:
        print(f"DEBUG API DELETE CLIENT ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

@api_view(['DELETE'])
@permission_classes([permissions.IsAuthenticated])
def api_delete_exchange(request, pk):
    try:
        # Note: Exchanges aren't owned by users in your models, 
        # but for security we'll assume only authenticated users can delete.
        exchange = Exchange.objects.get(id=pk)
        exchange.delete()
        return Response({'status': 'success'})
    except Exception as e:
        print(f"DEBUG API DELETE EXCHANGE ERROR: {str(e)}")
        return Response({'error': str(e)}, status=400)

class ClientViewSet(viewsets.ModelViewSet):
    serializer_class = ClientSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return Client.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

class ExchangeViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Exchange.objects.all()
    serializer_class = ExchangeSerializer
    permission_classes = [permissions.IsAuthenticated]

class ClientExchangeAccountViewSet(viewsets.ModelViewSet):
    serializer_class = ClientExchangeAccountSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return ClientExchangeAccount.objects.filter(client__user=self.request.user)

class TransactionViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = TransactionSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return Transaction.objects.filter(
            client_exchange__client__user=self.request.user
        ).order_by('-created_at', '-id')
