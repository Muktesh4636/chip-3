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

class FinancialDashboardFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_financial_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.financialDashboardTitle).text = "Financial Dashboard & KPIs"

        loadFinancialMetrics(view)
        setupKPICards(view)
    }

    private fun loadFinancialMetrics(view: View) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportsSummary(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val data = response.body()
                    updateFinancialMetrics(view, data)
                } else {
                    showDemoMetrics(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showDemoMetrics(view)
            }
        }
    }

    private fun updateFinancialMetrics(view: View, data: Map<String, Any>?) {
        // Update main KPIs
        val totalCapital = data?.get("total_capital") as? Int ?: 2500000
        val totalPnl = data?.get("global_pnl") as? Int ?: 125000
        val totalShare = data?.get("total_my_share") as? Int ?: 87500

        // Update KPI cards
        view.findViewById<View>(R.id.kpiCardCapital).findViewById<TextView>(R.id.kpiTitle).text = "TOTAL CAPITAL"
        view.findViewById<View>(R.id.kpiCardCapital).findViewById<TextView>(R.id.kpiValue).text = "₹${String.format("%,d", totalCapital)}"
        view.findViewById<View>(R.id.kpiCardCapital).findViewById<TextView>(R.id.kpiSubtitle).text = "Current Investment"

        view.findViewById<View>(R.id.kpiCardPnL).findViewById<TextView>(R.id.kpiTitle).text = "TOTAL PNL"
        view.findViewById<View>(R.id.kpiCardPnL).findViewById<TextView>(R.id.kpiValue).text = "₹${String.format("%,d", totalPnl)}"
        view.findViewById<View>(R.id.kpiCardPnL).findViewById<TextView>(R.id.kpiSubtitle).text = "Profit & Loss"

        view.findViewById<View>(R.id.kpiCardShare).findViewById<TextView>(R.id.kpiTitle).text = "MY SHARE"
        view.findViewById<View>(R.id.kpiCardShare).findViewById<TextView>(R.id.kpiValue).text = "₹${String.format("%,d", totalShare)}"
        view.findViewById<View>(R.id.kpiCardShare).findViewById<TextView>(R.id.kpiSubtitle).text = "Commission Earned"

        // Calculate and update additional KPIs
        val roi = if (totalCapital > 0) (totalPnl.toDouble() / totalCapital * 100) else 0.0
        val sharePercentage = if (totalPnl > 0) (totalShare.toDouble() / totalPnl * 100) else 0.0

        view.findViewById<View>(R.id.kpiCardROI).findViewById<TextView>(R.id.kpiTitle).text = "ROI"
        view.findViewById<View>(R.id.kpiCardROI).findViewById<TextView>(R.id.kpiValue).text = "${String.format("%.1f", roi)}%"
        view.findViewById<View>(R.id.kpiCardROI).findViewById<TextView>(R.id.kpiSubtitle).text = "Return on Investment"

        // Update hidden summary fields for compatibility
        view.findViewById<TextView>(R.id.kpiTotalCapital).text = "₹${String.format("%,d", totalCapital)}"
        view.findViewById<TextView>(R.id.kpiTotalPnL).text = "₹${String.format("%,d", totalPnl)}"
        view.findViewById<TextView>(R.id.kpiTotalShare).text = "₹${String.format("%,d", totalShare)}"
        view.findViewById<TextView>(R.id.kpiROI).text = "${String.format("%.1f", roi)}%"
        view.findViewById<TextView>(R.id.kpiSharePercentage).text = "${String.format("%.1f", sharePercentage)}%"

        // Update trend indicators (simplified)
        updateTrendIndicators(view, totalPnl, totalShare)
    }

    private fun showDemoMetrics(view: View) {
        // Demo data for when API is not available
        view.findViewById<TextView>(R.id.kpiTotalCapital).text = "₹25,00,000"
        view.findViewById<TextView>(R.id.kpiTotalPnL).text = "₹1,25,000"
        view.findViewById<TextView>(R.id.kpiTotalShare).text = "₹87,500"
        view.findViewById<TextView>(R.id.kpiROI).text = "5.0%"
        view.findViewById<TextView>(R.id.kpiSharePercentage).text = "70.0%"

        updateTrendIndicators(view, 125000, 87500)
    }

    private fun updateTrendIndicators(view: View, pnl: Int, share: Int) {
        // Simplified trend logic
        val pnlTrend = if (pnl > 0) "↗️ +12.5%" else "↘️ -8.3%"
        val shareTrend = if (share > 0) "↗️ +15.2%" else "↘️ -5.7%"

        view.findViewById<TextView>(R.id.pnlTrend).text = pnlTrend
        view.findViewById<TextView>(R.id.shareTrend).text = shareTrend

        // Color coding
        val pnlColor = if (pnl > 0) R.color.success else R.color.danger
        val shareColor = if (share > 0) R.color.success else R.color.danger

        view.findViewById<TextView>(R.id.pnlTrend).setTextColor(resources.getColor(pnlColor, null))
        view.findViewById<TextView>(R.id.shareTrend).setTextColor(resources.getColor(shareColor, null))
    }

    private fun setupKPICards(view: View) {
        // Monthly breakdown (demo data)
        val monthlyData = mapOf(
            "Jan" to mapOf("capital" to 2200000, "pnl" to 110000, "share" to 77000),
            "Feb" to mapOf("capital" to 2350000, "pnl" to 117500, "share" to 82250),
            "Mar" to mapOf("capital" to 2500000, "pnl" to 125000, "share" to 87500)
        )

        val monthlyBreakdown = StringBuilder()
        monthlyData.forEach { (month, data) ->
            monthlyBreakdown.append("$month: ₹${String.format("%,d", data["capital"] as Int)} | ")
            monthlyBreakdown.append("PnL: ₹${String.format("%,d", data["pnl"] as Int)} | ")
            monthlyBreakdown.append("Share: ₹${String.format("%,d", data["share"] as Int)}\n")
        }

        view.findViewById<TextView>(R.id.monthlyBreakdown).text = monthlyBreakdown.toString()

        // Risk metrics (demo)
        view.findViewById<TextView>(R.id.riskMetrics).text = """
            Risk Exposure: Medium
            Max Drawdown: 8.5%
            Sharpe Ratio: 2.1
            Win Rate: 68%
            Risk/Reward: 1:2.3
        """.trimIndent()

        // Performance indicators
        view.findViewById<TextView>(R.id.performanceIndicators).text = """
            • Best Month: March (+₹12,500)
            • Worst Month: January (+₹11,000)
            • Avg Monthly Return: +₹12,833
            • Consistency Score: 8.5/10
            • Client Satisfaction: 9.2/10
        """.trimIndent()
    }
}