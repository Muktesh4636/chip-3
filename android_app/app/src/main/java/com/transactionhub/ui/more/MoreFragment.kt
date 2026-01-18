package com.transactionhub.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.LoginActivity
import com.transactionhub.R
import com.transactionhub.ui.exchanges.ExchangeCreateFragment
import com.transactionhub.ui.exchanges.ExchangesFragment
import com.transactionhub.ui.clients.ClientCreateFragment
import com.transactionhub.ui.payments.PendingPaymentsFragment
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoreFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        
        view.findViewById<View>(R.id.menuExchanges).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExchangesFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<View>(R.id.menuReports).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuPendingPayments).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PendingPaymentsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCustomReports).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CustomReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuClientReports).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ClientReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuExchangeReports).setOnClickListener {
            // TODO: Implement ExchangeReportsFragment
            Toast.makeText(context, "Exchange reports coming soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuTimeTravel).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TimeTravelFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuAdvancedSearch).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdvancedSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuUserProfile).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuAuditLogs).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AuditLogsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCharts).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChartsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuDataManagement).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DataManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuMarketIntelligence).setOnClickListener {
            Toast.makeText(context, "Opening Market Intelligence...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuInvestmentBanking).setOnClickListener {
            Toast.makeText(context, "Opening Investment Banking...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuPrivateEquity).setOnClickListener {
            Toast.makeText(context, "Opening Private Equity & VC...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuRealEstate).setOnClickListener {
            Toast.makeText(context, "Opening Real Estate & Assets...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuInsurance).setOnClickListener {
            Toast.makeText(context, "Opening Insurance & Wealth...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuLegal).setOnClickListener {
            Toast.makeText(context, "Opening Legal & Contracts...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuSupport).setOnClickListener {
            Toast.makeText(context, "Opening Support & Helpdesk...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuWiki).setOnClickListener {
            Toast.makeText(context, "Opening Knowledge Base...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuCompanyShare).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CompanyShareSummaryFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuNotifications).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuAppearance).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AppearanceFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuEmailIntegration).setOnClickListener {
            handleEmailIntegration()
        }

        view.findViewById<View>(R.id.menuOfflineMode).setOnClickListener {
            handleOfflineMode()
        }

        view.findViewById<View>(R.id.menuCRM).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CRMFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuProjectManagement).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProjectManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuMarketing).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MarketingAutomationFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuHR).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HRManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuSecurity).setOnClickListener {
            handleSecurity()
        }

        view.findViewById<View>(R.id.menuTasks).setOnClickListener {
            handleTasks()
        }

        view.findViewById<View>(R.id.menuFinancialDashboard).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FinancialDashboardFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuPortfolio).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PortfolioFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuRiskManagementNew).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RiskManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuTaxManagementNew).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TaxComplianceFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuGlobalMarketsNew).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GlobalMarketsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuDeveloperPortalNew).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DeveloperPortalFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCompliance).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ComplianceFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuAnalytics).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdvancedAnalyticsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuDocuments).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DocumentManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuBilling).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BillingInvoicingFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuTasks).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TaskCalendarFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuWorkflow).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, WorkflowAutomationFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuSystemAdmin).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SystemAdminFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuTraining).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TrainingCenterFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCommunication).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CommunicationHubFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuHelpSupport).setOnClickListener {
            Toast.makeText(context, "Help & support center coming soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuDocuments).setOnClickListener {
            handleDocuments()
        }

        view.findViewById<View>(R.id.menuCreateExchange).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExchangeCreateFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCreateClient).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ClientCreateFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuExportPending).setOnClickListener {
            exportPendingPayments()
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefManager.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun exportPendingPayments() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.exportPendingPayments(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Save CSV to Downloads folder
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName = "pending_payments_$timestamp.csv"

                        val downloadsDir = File(requireContext().getExternalFilesDir(null), "Downloads")
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }

                        val file = File(downloadsDir, fileName)
                        val fos = FileOutputStream(file)
                        fos.write(body.bytes())
                        fos.close()

                        Toast.makeText(
                            requireContext(),
                            "CSV exported to: ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleEmailIntegration() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, EmailIntegrationFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun handleOfflineMode() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OfflineModeFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun handleSecurity() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SecurityFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun handleTasks() {
        Toast.makeText(context, "Task management coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun handleDocuments() {
        Toast.makeText(context, "Document management coming soon!", Toast.LENGTH_SHORT).show()
    }
}
