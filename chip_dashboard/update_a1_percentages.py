#!/usr/bin/env python
"""
Script to update percentages for a1 client account
Run this from the chip_dashboard directory: python update_a1_percentages.py
"""
import os
import sys
import django

# Setup Django
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'broker_portal.settings')
django.setup()

from core.models import ClientExchangeAccount, ClientExchangeReportConfig
from decimal import Decimal

# Find all accounts for client 'a1'
accounts = ClientExchangeAccount.objects.filter(client__name='a1')

if not accounts.exists():
    print("No accounts found for client 'a1'")
    sys.exit(1)

for account in accounts:
    print(f"\nUpdating account: {account} (ID: {account.pk})")
    print(f"Current My Total %: {account.my_percentage}")
    
    # Set the values
    company_pct = Decimal('9.5')
    own_pct = Decimal('0.5')
    
    # Verify they sum to my_percentage
    if company_pct + own_pct != Decimal(str(account.my_percentage)):
        print(f"WARNING: Company % ({company_pct}) + My Own % ({own_pct}) = {company_pct + own_pct}")
        print(f"         But My Total % = {account.my_percentage}")
        print("         Values don't match! Skipping this account.")
        continue
    
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
        print(f"✓ Updated existing report config")
    else:
        print(f"✓ Created new report config")
    
    print(f"  Company %: {report_config.friend_percentage}")
    print(f"  My Own %: {report_config.my_own_percentage}")
    print(f"  Total: {report_config.friend_percentage + report_config.my_own_percentage}")

print("\n✓ All accounts updated successfully!")




