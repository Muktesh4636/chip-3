package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.transactionhub.R

class NotificationsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.notificationsTitle).text = "Notifications & Alerts"

        setupNotificationSettings(view)
    }

    private fun setupNotificationSettings(view: View) {
        val switchPaymentReminders = view.findViewById<Switch>(R.id.switchPaymentReminders)
        val switchDailyReports = view.findViewById<Switch>(R.id.switchDailyReports)
        val switchWeeklyReports = view.findViewById<Switch>(R.id.switchWeeklyReports)
        val switchSecurityAlerts = view.findViewById<Switch>(R.id.switchSecurityAlerts)
        val switchTransactionAlerts = view.findViewById<Switch>(R.id.switchTransactionAlerts)

        // Load saved preferences (for now, just set defaults)
        switchPaymentReminders.isChecked = true
        switchSecurityAlerts.isChecked = true

        // Setup listeners
        switchPaymentReminders.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, "Payment reminders: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchDailyReports.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, "Daily reports: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchWeeklyReports.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, "Weekly reports: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchSecurityAlerts.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, "Security alerts: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchTransactionAlerts.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(context, "Transaction alerts: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnTestNotification).setOnClickListener {
            Toast.makeText(context, "Test notification sent! 📱", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnNotificationSettings).setOnClickListener {
            Toast.makeText(context, "Notification settings not available via mobile app. Please use device settings.", Toast.LENGTH_LONG).show()
        }
    }
}