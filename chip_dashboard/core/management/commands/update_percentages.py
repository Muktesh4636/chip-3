"""
Management command to update percentage values from integers to decimals.
Usage: python manage.py update_percentages --client a1 --company 9.5 --own 0.5
"""
from django.core.management.base import BaseCommand
from core.models import ClientExchangeAccount, ClientExchangeReportConfig
from decimal import Decimal


class Command(BaseCommand):
    help = 'Update percentage values for a client exchange account'

    def add_arguments(self, parser):
        parser.add_argument('--client', type=str, required=True, help='Client name')
        parser.add_argument('--company', type=float, required=True, help='Company percentage')
        parser.add_argument('--own', type=float, required=True, help='My Own percentage')

    def handle(self, *args, **options):
        client_name = options['client']
        company_pct = Decimal(str(options['company']))
        own_pct = Decimal(str(options['own']))
        
        # Find the account
        account = ClientExchangeAccount.objects.filter(client__name=client_name).first()
        
        if not account:
            self.stdout.write(self.style.ERROR(f'Account not found for client: {client_name}'))
            return
        
        self.stdout.write(f'Found account: {account}')
        self.stdout.write(f'Current My Total %: {account.my_percentage}')
        
        # Get or create report config
        report_config, created = ClientExchangeReportConfig.objects.get_or_create(
            client_exchange=account,
            defaults={
                'friend_percentage': company_pct,
                'my_own_percentage': own_pct,
            }
        )
        
        if not created:
            report_config.friend_percentage = company_pct
            report_config.my_own_percentage = own_pct
            report_config.save()
            self.stdout.write(self.style.SUCCESS(f'Updated report config'))
        else:
            self.stdout.write(self.style.SUCCESS(f'Created report config'))
        
        self.stdout.write(self.style.SUCCESS(
            f'Successfully updated percentages:\n'
            f'  Company %: {report_config.friend_percentage}\n'
            f'  My Own %: {report_config.my_own_percentage}\n'
            f'  Total: {report_config.friend_percentage + report_config.my_own_percentage}'
        ))




