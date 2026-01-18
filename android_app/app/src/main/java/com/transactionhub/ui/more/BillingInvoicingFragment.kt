package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

data class InvoiceItem(
    val id: Int,
    val invoiceNumber: String,
    val clientName: String,
    val amount: String,
    val status: String,
    val dueDate: String,
    val issueDate: String
)

class InvoiceAdapter(private val invoices: List<InvoiceItem>) :
    RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

    class InvoiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val invoiceNumber: TextView = view.findViewById(R.id.invoiceNumber)
        val clientName: TextView = view.findViewById(R.id.invoiceClientName)
        val amount: TextView = view.findViewById(R.id.invoiceAmount)
        val status: TextView = view.findViewById(R.id.invoiceStatus)
        val dueDate: TextView = view.findViewById(R.id.invoiceDueDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice, parent, false)
        return InvoiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        val invoice = invoices[position]
        holder.invoiceNumber.text = invoice.invoiceNumber
        holder.clientName.text = "Client: ${invoice.clientName}"
        holder.amount.text = "Amount: ${invoice.amount}"
        holder.status.text = "Status: ${invoice.status}"
        holder.dueDate.text = "Due: ${invoice.dueDate}"
    }

    override fun getItemCount() = invoices.size
}

class BillingInvoicingFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var invoiceAdapter: InvoiceAdapter
    private val invoices = mutableListOf<InvoiceItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_billing_invoicing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.billingTitle).text = "Billing & Invoicing"

        setupInvoiceList(view)
        loadBillingData(view)
        setupBillingControls(view)
    }

    private fun setupInvoiceList(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.invoiceRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        invoiceAdapter = InvoiceAdapter(invoices)
        recyclerView.adapter = invoiceAdapter
    }

    private fun loadBillingData(view: View) {
        // Sample invoices - in real app this would come from API
        invoices.addAll(listOf(
            InvoiceItem(1, "INV-2025-001", "ABC Capital", "₹2,45,000", "Paid", "2025-01-15", "2025-01-01"),
            InvoiceItem(2, "INV-2025-002", "XYZ Investments", "₹1,87,500", "Overdue", "2025-01-10", "2025-01-05"),
            InvoiceItem(3, "INV-2025-003", "Global Traders", "₹3,42,000", "Sent", "2025-01-25", "2025-01-10"),
            InvoiceItem(4, "INV-2025-004", "Prime Holdings", "₹98,750", "Paid", "2025-01-20", "2025-01-08"),
            InvoiceItem(5, "INV-2025-005", "Elite Partners", "₹4,56,200", "Pending", "2025-02-01", "2025-01-12"),
            InvoiceItem(6, "INV-2025-006", "Vertex Corp", "₹2,89,300", "Sent", "2025-01-30", "2025-01-14"),
            InvoiceItem(7, "INV-2025-007", "Summit Group", "₹1,67,800", "Paid", "2025-01-18", "2025-01-06"),
            InvoiceItem(8, "INV-2025-008", "Apex Investments", "₹3,78,900", "Overdue", "2025-01-12", "2025-01-03")
        ))

        // Billing Overview
        view.findViewById<TextView>(R.id.billingOverview).text = """
        💰 BILLING OVERVIEW DASHBOARD

        💵 Financial Summary (This Month):
        • Total Invoiced: ₹18,65,450
        • Amount Received: ₹12,34,250
        • Outstanding: ₹6,31,200
        • Overdue Amount: ₹2,76,250

        📊 Collection Performance:
        • Collection Rate: 66.1%
        • Average Payment Time: 18.5 days
        • Overdue Percentage: 14.8%
        • Bad Debt Rate: 1.2%

        📈 Monthly Trends:
        • Invoiced This Month: +23% vs last month
        • Collections This Month: +31% vs last month
        • Outstanding Balance: -8% vs last month
        • Customer Satisfaction: 4.6/5

        🎯 Collection Goals:
        • Target Collection Rate: 75%
        • Days Sales Outstanding: <21 days
        • Bad Debt Target: <2%
        • Customer Retention: >95%
        """.trimIndent()

        // Invoice Status Breakdown
        view.findViewById<TextView>(R.id.invoiceStatus).text = """
        📄 INVOICE STATUS BREAKDOWN

        ✅ Paid Invoices: ${invoices.count { it.status == "Paid" }}
        • Total Value: ₹8,51,750
        • Average Payment Time: 15.2 days
        • On-Time Payment Rate: 78%

        📤 Sent Invoices: ${invoices.count { it.status == "Sent" }}
        • Total Value: ₹6,31,300
        • Average Age: 8.5 days
        • Expected Collection: ₹4,78,200

        ⏳ Pending Invoices: ${invoices.count { it.status == "Pending" }}
        • Total Value: ₹4,56,200
        • Due Soon: ₹4,56,200
        • Follow-up Required: 2 invoices

        🚨 Overdue Invoices: ${invoices.count { it.status == "Overdue" }}
        • Total Value: ₹2,76,250
        • Average Overdue Days: 12.3 days
        • Collection Priority: High

        📊 Aging Analysis:
        • 0-30 days: ₹12,45,300 (66.7%)
        • 31-60 days: ₹4,56,200 (24.4%)
        • 61-90 days: ₹1,63,950 (8.8%)
        • 90+ days: ₹0 (0%)
        """.trimIndent()

        // Payment Methods
        view.findViewById<TextView>(R.id.paymentMethods).text = """
        💳 PAYMENT METHODS & PROCESSING

        💵 Payment Method Distribution:
        • Bank Transfer: 58% (₹7,18,250)
        • Cheque: 23% (₹2,84,000)
        • Online Payment: 12% (₹1,48,000)
        • Cash: 7% (₹86,500)

        ⚡ Payment Processing:
        • Average Processing Time: 2.3 days
        • Auto-Reconciliation Rate: 94%
        • Manual Review Required: 6%
        • Error Rate: 0.3%

        🔄 Recurring Payments:
        • Active Subscriptions: 15 clients
        • Monthly Recurring Revenue: ₹8,45,000
        • Setup Success Rate: 98%
        • Cancellation Rate: 3.2%

        🌐 Digital Payment Integration:
        • Payment Gateway Uptime: 99.9%
        • Transaction Success Rate: 97.8%
        • Chargeback Rate: 0.15%
        • Customer Satisfaction: 4.7/5
        """.trimIndent()

        // Client Billing History
        view.findViewById<TextView>(R.id.clientBilling).text = """
        👥 CLIENT BILLING HISTORY

        🏆 Top Clients by Revenue:
        1. Global Traders - ₹4,56,200 (24.4%)
        2. Apex Investments - ₹3,78,900 (20.3%)
        3. ABC Capital - ₹2,45,000 (13.1%)
        4. Vertex Corp - ₹2,89,300 (15.5%)
        5. Elite Partners - ₹4,56,200 (24.4%)

        💎 High-Value Clients (>₹3,00,000):
        • Global Traders: Always paid on time, excellent relationship
        • Apex Investments: Requires gentle follow-up, usually pays within 7 days
        • Elite Partners: New client, building payment history

        ⚠️ Clients Needing Attention:
        • XYZ Investments: Overdue by 12 days, follow-up initiated
        • ABC Capital: Large account, payment expected today

        📈 Client Payment Trends:
        • Improved Payment Terms: 3 clients upgraded
        • Early Payment Incentives: 8 clients enrolled
        • Payment Plan Requests: 2 clients (both approved)
        • Credit Limit Increases: 4 clients approved

        🎯 Client Retention Metrics:
        • Client Retention Rate: 96.8%
        • Revenue Retention: 98.2%
        • Client Satisfaction: 4.6/5
        • Net Promoter Score: 42
        """.trimIndent()

        // Financial Reporting
        view.findViewById<TextView>(R.id.financialReporting).text = """
        📊 FINANCIAL REPORTING & COMPLIANCE

        💼 Revenue Recognition:
        • Accrued Revenue: ₹18,65,450
        • Deferred Revenue: ₹3,42,000
        • Recognized Revenue: ₹15,23,450
        • Monthly Recurring Revenue: ₹8,45,000

        📋 Tax Compliance:
        • GST Collected: ₹2,34,567
        • TDS Deducted: ₹1,67,890
        • Input Tax Credit: ₹1,89,234
        • Tax Filing Status: Compliant

        ⚖️ Regulatory Reporting:
        • SEBI Compliance: 100% compliant
        • RBI Reporting: All filings submitted
        • Auditor Requirements: Documentation ready
        • Regulatory Audits: Passed all reviews

        📈 Financial KPIs:
        • Days Sales Outstanding: 18.5 days
        • Collection Effectiveness Index: 87.3%
        • Bad Debt to Sales Ratio: 1.2%
        • Customer Acquisition Cost: ₹23,450
        • Customer Lifetime Value: ₹12,34,000

        🎯 Business Intelligence:
        • Profit Margin: 34.2%
        • Operating Expenses: ₹4,56,000/month
        • Client Acquisition Rate: 12 new clients/month
        • Market Share Growth: +8.7% YoY
        • Competitive Position: Industry leader
        """.trimIndent()

        invoiceAdapter.notifyDataSetChanged()
    }

    private fun setupBillingControls(view: View) {
        view.findViewById<Button>(R.id.btnCreateInvoice).setOnClickListener {
            createInvoice()
        }

        view.findViewById<Button>(R.id.btnSendInvoice).setOnClickListener {
            sendInvoice()
        }

        view.findViewById<Button>(R.id.btnPaymentReminder).setOnClickListener {
            sendPaymentReminder()
        }

        view.findViewById<Button>(R.id.btnGenerateReport).setOnClickListener {
            generateBillingReport()
        }

        view.findViewById<Button>(R.id.btnBillingSettings).setOnClickListener {
            billingSettings()
        }
    }

    private fun createInvoice() {
        val invoiceTypes = arrayOf(
            "Service Invoice",
            "Product Invoice",
            "Recurring Subscription",
            "Project Milestone Invoice",
            "Consultation Invoice",
            "Training Invoice",
            "Support Services Invoice",
            "Custom Invoice"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New Invoice")
            .setItems(invoiceTypes) { _, which ->
                val selectedType = invoiceTypes[which]
                Toast.makeText(context, "Creating $selectedType...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    kotlinx.coroutines.delay(1500)
                    Toast.makeText(context, "Invoice created successfully! INV-2025-${String.format("%03d", (invoices.size + 1))}", Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton("Quick Invoice", null)
            .show()
    }

    private fun sendInvoice() {
        val deliveryMethods = arrayOf(
            "Email Invoice",
            "Generate PDF & Download",
            "Mail Physical Copy",
            "Client Portal Upload",
            "Integration with Accounting Software",
            "Bulk Send Multiple Invoices",
            "Schedule Future Delivery"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Send Invoice")
            .setItems(deliveryMethods) { _, which ->
                val selectedMethod = deliveryMethods[which]
                Toast.makeText(context, "$selectedMethod initiated...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun sendPaymentReminder() {
        val reminderTypes = arrayOf(
            "Gentle Reminder (7 days overdue)",
            "Firm Reminder (14 days overdue)",
            "Final Notice (21+ days overdue)",
            "Payment Plan Offer",
            "Settlement Discussion Request",
            "Legal Notice Preparation",
            "Account Suspension Warning"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Send Payment Reminder")
            .setItems(reminderTypes) { _, which ->
                val selectedType = reminderTypes[which]
                Toast.makeText(context, "$selectedType sent to overdue clients...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun generateBillingReport() {
        val reportTypes = arrayOf(
            "Monthly Billing Summary",
            "Client Payment History",
            "Outstanding Invoices Report",
            "Revenue Analysis Report",
            "Collection Performance Report",
            "Tax & Compliance Report",
            "Aging Analysis Report",
            "Custom Date Range Report"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Generate Billing Report")
            .setItems(reportTypes) { _, which ->
                val selectedReport = reportTypes[which]
                Toast.makeText(context, "Generating $selectedReport...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    kotlinx.coroutines.delay(2000)
                    Toast.makeText(context, "$selectedReport generated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun billingSettings() {
        val settingsCategories = arrayOf(
            "Invoice Templates & Branding",
            "Payment Terms & Conditions",
            "Tax Settings & Compliance",
            "Email Templates & Automation",
            "Payment Gateway Configuration",
            "Accounting Software Integration",
            "Client Credit Limits",
            "Reminder Schedule Settings"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Billing Settings")
            .setItems(settingsCategories) { _, which ->
                val selectedCategory = settingsCategories[which]
                Toast.makeText(context, "Opening $selectedCategory...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}