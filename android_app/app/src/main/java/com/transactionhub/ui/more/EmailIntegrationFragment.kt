package com.transactionhub.ui.more

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class EmailIntegrationFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_email_integration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.emailIntegrationTitle).text = "Email Integration & Sharing"

        setupEmailButtons(view)
    }

    private fun setupEmailButtons(view: View) {
        view.findViewById<Button>(R.id.btnEmailDailyReport).setOnClickListener {
            sendDailyReportEmail()
        }

        view.findViewById<Button>(R.id.btnEmailWeeklyReport).setOnClickListener {
            sendWeeklyReportEmail()
        }

        view.findViewById<Button>(R.id.btnEmailCustomReport).setOnClickListener {
            sendCustomReportEmail()
        }

        view.findViewById<Button>(R.id.btnEmailTransactionSummary).setOnClickListener {
            sendTransactionSummaryEmail()
        }

        view.findViewById<Button>(R.id.btnEmailClientReport).setOnClickListener {
            sendClientReportEmail()
        }

        view.findViewById<Button>(R.id.btnEmailAccountStatement).setOnClickListener {
            sendAccountStatementEmail()
        }

        view.findViewById<Button>(R.id.btnEmailDataExport).setOnClickListener {
            sendDataExportEmail()
        }

        view.findViewById<Button>(R.id.btnShareApp).setOnClickListener {
            shareApp()
        }
    }

    private fun sendDailyReportEmail() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportsSummary(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val data = response.body()
                    val subject = "Daily Trading Report - ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}"
                    val body = generateDailyReportBody(data)
                    composeEmail(subject, body)
                } else {
                    Toast.makeText(context, "Failed to generate daily report", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendWeeklyReportEmail() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportsSummary(ApiClient.getAuthToken(token), "weekly")
                if (response.isSuccessful) {
                    val data = response.body()
                    val subject = "Weekly Trading Report - ${java.text.SimpleDateFormat("yyyy-'W'ww", java.util.Locale.getDefault()).format(java.util.Date())}"
                    val body = generateWeeklyReportBody(data)
                    composeEmail(subject, body)
                } else {
                    Toast.makeText(context, "Failed to generate weekly report", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendCustomReportEmail() {
        // Show date picker dialog for custom date range
        Toast.makeText(context, "Custom report email - select date range first", Toast.LENGTH_SHORT).show()
        // TODO: Implement date range picker
    }

    private fun sendTransactionSummaryEmail() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val transactions = response.body() ?: emptyList()
                    val subject = "Transaction Summary Report"
                    val body = generateTransactionSummaryBody(transactions)
                    composeEmail(subject, body)
                } else {
                    Toast.makeText(context, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendClientReportEmail() {
        Toast.makeText(context, "Select a client to send their report", Toast.LENGTH_SHORT).show()
        // TODO: Show client selection dialog
    }

    private fun sendAccountStatementEmail() {
        Toast.makeText(context, "Select an account to send statement", Toast.LENGTH_SHORT).show()
        // TODO: Show account selection dialog
    }

    private fun sendDataExportEmail() {
        Toast.makeText(context, "Attach exported data files to email", Toast.LENGTH_SHORT).show()
        // TODO: Attach CSV files to email
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this amazing Trading Management App! Download it to manage your trading accounts professionally. 📊💼📱")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share App"))
    }

    private fun generateDailyReportBody(data: Map<String, Any>?): String {
        return """
            📊 DAILY TRADING REPORT

            Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}

            📈 SUMMARY
            • Total Capital: ₹${String.format("%,d", data?.get("total_capital") as? Int ?: 0)}
            • Global PnL: ₹${String.format("%,d", data?.get("global_pnl") as? Int ?: 0)}
            • Total My Share: ₹${String.format("%,d", data?.get("total_my_share") as? Int ?: 0)}

            📋 PERFORMANCE METRICS
            • Total Clients: ${data?.get("total_clients") ?: "N/A"}
            • Total Exchanges: ${data?.get("total_exchanges") ?: "N/A"}
            • Total Accounts: ${data?.get("total_accounts") ?: "N/A"}

            This report was generated from TransactionHub Mobile App.
            For detailed analysis, please log in to the web dashboard.
        """.trimIndent()
    }

    private fun generateWeeklyReportBody(data: Map<String, Any>?): String {
        return """
            📊 WEEKLY TRADING REPORT

            Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}

            📈 WEEKLY SUMMARY
            • Total Capital: ₹${String.format("%,d", data?.get("total_capital") as? Int ?: 0)}
            • Weekly PnL: ₹${String.format("%,d", data?.get("global_pnl") as? Int ?: 0)}
            • Weekly Share: ₹${String.format("%,d", data?.get("total_my_share") as? Int ?: 0)}

            📋 PERFORMANCE METRICS
            • Total Clients: ${data?.get("total_clients") ?: "N/A"}
            • Total Exchanges: ${data?.get("total_exchanges") ?: "N/A"}
            • Total Accounts: ${data?.get("total_accounts") ?: "N/A"}

            This report was generated from TransactionHub Mobile App.
            For detailed weekly analysis, please log in to the web dashboard.
        """.trimIndent()
    }

    private fun generateTransactionSummaryBody(transactions: List<com.transactionhub.data.models.Transaction>): String {
        val recentTransactions = transactions.take(10)
        val totalAmount = transactions.sumOf { it.amount }

        return """
            📊 TRANSACTION SUMMARY REPORT

            Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}

            📈 SUMMARY
            • Total Transactions: ${transactions.size}
            • Total Amount: ₹${String.format("%,d", totalAmount)}
            • Average Transaction: ₹${String.format("%,d", if (transactions.isNotEmpty()) totalAmount / transactions.size else 0)}

            📋 RECENT TRANSACTIONS
            ${recentTransactions.joinToString("\n") { txn ->
                "• ${txn.date}: ${txn.type_display} - ₹${String.format("%,d", txn.amount)} (${txn.client_name} | ${txn.exchange_name})"
            }}

            This report was generated from TransactionHub Mobile App.
            For complete transaction history, please log in to the web dashboard.
        """.trimIndent()
    }

    private fun composeEmail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found. Please install an email app.", Toast.LENGTH_LONG).show()
        }
    }
}