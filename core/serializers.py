from rest_framework import serializers
from .models import Client, Exchange, ClientExchangeAccount, Transaction

class ClientSerializer(serializers.ModelSerializer):
    class Meta:
        model = Client
        fields = ['id', 'name', 'code', 'referred_by', 'is_company_client']

class ExchangeSerializer(serializers.ModelSerializer):
    class Meta:
        model = Exchange
        fields = ['id', 'name', 'version_name', 'code']

class ClientExchangeAccountSerializer(serializers.ModelSerializer):
    client_name = serializers.CharField(source='client.name', read_only=True)
    exchange_name = serializers.CharField(source='exchange.name', read_only=True)
    pnl = serializers.SerializerMethodField()
    my_share = serializers.SerializerMethodField()
    remaining_amount = serializers.SerializerMethodField()

    def get_pnl(self, obj):
        try:
            return obj.compute_client_pnl()
        except Exception as e:
            return 0

    def get_my_share(self, obj):
        try:
            return obj.compute_my_share()
        except Exception as e:
            return 0

    def get_remaining_amount(self, obj):
        try:
            return obj.get_remaining_settlement_amount()['remaining']
        except Exception as e:
            return 0

    class Meta:
        model = ClientExchangeAccount
        fields = [
            'id', 'client', 'client_name', 'exchange', 'exchange_name',
            'funding', 'exchange_balance', 'pnl', 'my_share', 'remaining_amount',
            'loss_share_percentage', 'profit_share_percentage'
        ]

class TransactionSerializer(serializers.ModelSerializer):
    type_display = serializers.CharField(source='get_type_display', read_only=True)
    client_name = serializers.CharField(source='client_exchange.client.name', read_only=True)
    exchange_name = serializers.CharField(source='client_exchange.exchange.name', read_only=True)

    class Meta:
        model = Transaction
        fields = [
            'id', 'client_exchange', 'client_name', 'exchange_name',
            'date', 'type', 'type_display', 'amount',
            'funding_before', 'funding_after', 'exchange_balance_before', 'exchange_balance_after', 'sequence_no', 'notes'
        ]
