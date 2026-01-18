# Generated manually

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0012_add_decimal_support_to_percentages'),
    ]

    operations = [
        migrations.AddField(
            model_name='exchange',
            name='is_active',
            field=models.BooleanField(default=True, help_text='Whether this exchange is active and can be used'),
        ),
    ]



