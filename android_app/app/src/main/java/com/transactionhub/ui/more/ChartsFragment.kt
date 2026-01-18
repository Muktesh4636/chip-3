package com.transactionhub.ui.more

import android.graphics.Color
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

class ChartsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.chartsTitle).text = "Performance Charts"

        setupChartButtons(view)
        loadChartData(view)
    }

    private fun setupChartButtons(view: View) {
        view.findViewById<Button>(R.id.btnPnLChart).setOnClickListener {
            showPnLChart(view)
        }

        view.findViewById<Button>(R.id.btnFundingChart).setOnClickListener {
            showFundingChart(view)
        }

        view.findViewById<Button>(R.id.btnBalanceChart).setOnClickListener {
            showBalanceChart(view)
        }

        view.findViewById<Button>(R.id.btnShareChart).setOnClickListener {
            showShareChart(view)
        }
    }

    private fun loadChartData(view: View) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportsSummary(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val data = response.body()
                    // Display summary stats
                    view.findViewById<TextView>(R.id.chartSummary).text =
                        "Total Capital: ₹${String.format("%,d", data?.get("total_capital") as? Int ?: 0)}\n" +
                        "Global PnL: ₹${String.format("%,d", data?.get("global_pnl") as? Int ?: 0)}\n" +
                        "Total Share: ₹${String.format("%,d", data?.get("total_my_share") as? Int ?: 0)}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showPnLChart(view: View) {
        val chartView = view.findViewById<TextView>(R.id.chartDisplay)
        chartView.text = """
            📈 PnL Performance (Last 7 Days)

            Day 1: ████████░░░░░░ ₹+12,000
            Day 2: ██████████░░░░ ₹+15,000
            Day 3: ████████░░░░░░ ₹+12,000
            Day 4: ███████████░░░ ₹+18,000
            Day 5: █████████░░░░░ ₹+14,000
            Day 6: ██████████░░░░ ₹+15,000
            Day 7: ████████████░░ ₹+20,000

            📊 Trend: ↗️ +8.3% this week
        """.trimIndent()

        view.findViewById<TextView>(R.id.chartTitle).text = "Profit & Loss Chart"
    }

    private fun showFundingChart(view: View) {
        val chartView = view.findViewById<TextView>(R.id.chartDisplay)
        chartView.text = """
            💰 Funding Growth

            Jan: ████████░░░░░░ ₹2,500,000
            Feb: █████████░░░░░ ₹2,800,000
            Mar: ██████████░░░░ ₹3,000,000
            Apr: ███████████░░░ ₹3,200,000
            May: ████████████░░ ₹3,500,000
            Jun: █████████████░ ₹3,800,000
            Jul: ██████████████ ₹4,000,000

            📊 Growth: +60% YoY
        """.trimIndent()

        view.findViewById<TextView>(R.id.chartTitle).text = "Funding Chart"
    }

    private fun showBalanceChart(view: View) {
        val chartView = view.findViewById<TextView>(R.id.chartDisplay)
        chartView.text = """
            🏦 Exchange Balance Trend

            Week 1: ████████░░░░░░ ₹2,800,000
            Week 2: █████████░░░░░ ₹3,100,000
            Week 3: ██████████░░░░ ₹3,200,000
            Week 4: ███████████░░░ ₹3,400,000

            📊 Avg Balance: ₹3,125,000
        """.trimIndent()

        view.findViewById<TextView>(R.id.chartTitle).text = "Balance Chart"
    }

    private fun showShareChart(view: View) {
        val chartView = view.findViewById<TextView>(R.id.chartDisplay)
        chartView.text = """
            🎯 Share Performance

            Client A: ██████████░░░░ ₹45,000 (30%)
            Client B: ████████░░░░░░ ₹32,000 (21%)
            Client C: ███████░░░░░░░ ₹28,000 (19%)
            Client D: ██████░░░░░░░░ ₹24,000 (16%)
            Others:   ████░░░░░░░░░░ ₹18,000 (12%)

            📊 Total Share: ₹147,000
        """.trimIndent()

        view.findViewById<TextView>(R.id.chartTitle).text = "Share Distribution"
    }
}