# Generated manually

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0012_add_decimal_support_to_percentages'),
    ]

    operations = [
        migrations.AddField(
            model_name='exchange',
            name='version_name',
            field=models.CharField(blank=True, help_text='Version or variant name of the exchange', max_length=100, null=True),
        ),
    ]


