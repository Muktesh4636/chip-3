package com.transactionhub.ui.more

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

class CRMFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_crm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.crmTitle).text = "Customer Relationship Management"

        loadCRMData(view)
        setupCRMControls(view)
    }

    private fun loadCRMData(view: View) {
        // Sales Pipeline
        view.findViewById<TextView>(R.id.salesPipeline).text = """
        🎯 SALES PIPELINE SUMMARY

        🔥 New Leads: 15 leads (+23% this week)
        ⚡ Qualified: 8 leads
        📋 Proposal: 5 leads
        🤝 Negotiation: 3 leads
        ✅ Closed Won: 12 this month
        ❌ Closed Lost: 2 this month

        💰 Pipeline Value: ₹45,67,000
        📈 Conversion Rate: 18.5%
        ⏱️ Avg. Sales Cycle: 12 days
        """.trimIndent()

        // Client Activity
        view.findViewById<TextView>(R.id.clientActivity).text = """
        👥 RECENT CLIENT ACTIVITY

        📅 Today:
        • Call with Rahul Sharma - Portfolio Review
        • Email from Apex Capital - New account inquiry
        • Meeting with Vertex Corp - Quarterly strategy

        🗓️ Yesterday:
        • Onboarded 2 new clients (Global Traders, Elite Partners)
        • Sent performance report to 15 clients
        • Resolved 3 support tickets

        🏆 Top Clients (Engagement):
        1. ABC Capital (High)
        2. Apex Investments (Medium)
        3. Global Traders (High)
        """.trimIndent()

        // Lead Sources
        view.findViewById<TextView>(R.id.leadSources).text = """
        🌐 LEAD SOURCE ANALYSIS

        🤝 Referrals: 45% (₹20,50,000)
        🏢 LinkedIn: 25% (₹11,40,000)
        🌐 Website: 15% (₹6,85,000)
        📧 Email Campaigns: 10% (₹4,56,000)
        📞 Cold Outreach: 5% (₹2,28,000)

        🎯 Top Performing Source: Referrals (92% conversion)
        ⚠️ Lowest Performing: Cold Outreach (12% conversion)
        """.trimIndent()

        // Customer Satisfaction
        view.findViewById<TextView>(R.id.customerSatisfaction).text = """
        ⭐ CUSTOMER SATISFACTION (CSAT)

        📊 Overall Score: 4.8 / 5.0
        😊 Happy Clients: 94%
        😐 Neutral: 4%
        😞 Unhappy: 2%

        💬 Recent Feedback:
        • "Excellent support team!" - Rahul S.
        • "Love the mobile app interface." - Maria K.
        • "Reports are very detailed." - David L.

        📈 Net Promoter Score (NPS): 72 (Excellent)
        """.trimIndent()
    }

    private fun setupCRMControls(view: View) {
        view.findViewById<Button>(R.id.btnAddLead).setOnClickListener {
            addNewLead()
        }

        view.findViewById<Button>(R.id.btnViewPipeline).setOnClickListener {
            viewPipeline()
        }

        view.findViewById<Button>(R.id.btnClientInteraction).setOnClickListener {
            recordInteraction()
        }

        view.findViewById<Button>(R.id.btnCRMReports).setOnClickListener {
            showCRMReports()
        }

        view.findViewById<Button>(R.id.btnCRMSettings).setOnClickListener {
            crmSettings()
        }
    }

    private fun addNewLead() {
        val leadForm = arrayOf(
            "Individual Lead",
            "Institutional Lead",
            "Partner Lead",
            "Referral Lead",
            "Corporate Lead"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add New Lead")
            .setItems(leadForm) { _, which ->
                val selected = leadForm[which]
                Toast.makeText(context, "Opening $selected form...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Quick Add", null)
            .show()
    }

    private fun viewPipeline() {
        val pipelineStages = arrayOf(
            "Discovery (15)",
            "Qualification (8)",
            "Proposal (5)",
            "Negotiation (3)",
            "Closing (2)",
            "Closed Won (12)",
            "Closed Lost (2)"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Sales Pipeline")
            .setItems(pipelineStages) { _, which ->
                val stage = pipelineStages[which]
                Toast.makeText(context, "Viewing leads in $stage stage...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun recordInteraction() {
        val interactionTypes = arrayOf(
            "📞 Phone Call",
            "📧 Email Sent",
            "🤝 Meeting Held",
            "💬 Chat/Message",
            "📝 Note Added",
            "📊 Report Delivered",
            "🆘 Support Ticket"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Record Client Interaction")
            .setItems(interactionTypes) { _, which ->
                val selected = interactionTypes[which]
                Toast.makeText(context, "Recording $selected...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showCRMReports() {
        val report = """
        CRM PERFORMANCE REPORT

        📈 SALES SUMMARY:
        • New Leads: 45 this month
        • Conversion Rate: 18.5%
        • Revenue Potential: ₹45,67,000
        • Actual Revenue: ₹12,34,000

        👥 CLIENT RETENTION:
        • Churn Rate: 1.2%
        • Renewal Rate: 98.5%
        • CLV (Avg): ₹12,45,000

        🎯 TEAM PERFORMANCE:
        • Most Active: Sarah (45 interactions)
        • Best Closer: Mike (8 deals)
        • Support Star: Lisa (98% resolution)

        📊 ENGAGEMENT METRICS:
        • Avg. Response Time: 15 mins
        • Client Health Score: 92/100
        • NPS: 72
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("CRM Analytics")
            .setMessage(report)
            .setPositiveButton("Export PDF", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun crmSettings() {
        val settings = arrayOf(
            "Pipeline Stages",
            "Lead Scoring Rules",
            "Interaction Templates",
            "Notification Rules",
            "User Permissions",
            "Data Export/Import"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("CRM Configuration")
            .setItems(settings) { _, which ->
                Toast.makeText(context, "Opening ${settings[which]} settings...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}