# Generated manually

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0013_add_exchange_is_active'),
    ]

    operations = [
        migrations.AddField(
            model_name='exchange',
            name='version',
            field=models.CharField(blank=True, help_text='Optional version identifier for the exchange', max_length=50, null=True),
        ),
    ]



