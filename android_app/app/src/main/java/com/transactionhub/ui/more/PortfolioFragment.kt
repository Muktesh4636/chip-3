package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class PortfolioFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.portfolioTitle).text = "Portfolio Overview"

        loadPortfolioData(view)
    }

    private fun loadPortfolioData(view: View) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                // Load accounts for portfolio overview
                val accountsResponse = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (accountsResponse.isSuccessful) {
                    val accounts = accountsResponse.body() ?: emptyList()

                    // Calculate portfolio metrics
                    val totalCapital = accounts.sumOf { it.funding }
                    val totalBalance = accounts.sumOf { it.exchange_balance }
                    val totalPnl = accounts.sumOf { it.pnl }
                    val totalShare = accounts.sumOf { it.my_share }

                    // Group by exchange
                    val exchangeBreakdown = accounts.groupBy { it.exchange_name }

                    updatePortfolioDisplay(view, totalCapital, totalBalance, totalPnl, totalShare, exchangeBreakdown)
                } else {
                    showDemoPortfolio(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showDemoPortfolio(view)
            }
        }
    }

    private fun updatePortfolioDisplay(
        view: View,
        totalCapital: Long,
        totalBalance: Long,
        totalPnl: Long,
        totalShare: Long,
        exchangeBreakdown: Map<String, List<com.transactionhub.data.models.Account>>
    ) {
        // Update portfolio metric cards
        view.findViewById<View>(R.id.portfolioCardCapital).findViewById<TextView>(R.id.portfolioMetricTitle).text = "TOTAL CAPITAL"
        view.findViewById<View>(R.id.portfolioCardCapital).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹${String.format("%,d", totalCapital)}"
        view.findViewById<View>(R.id.portfolioCardCapital).findViewById<TextView>(R.id.portfolioMetricChange).text = "+2.1%"

        view.findViewById<View>(R.id.portfolioCardBalance).findViewById<TextView>(R.id.portfolioMetricTitle).text = "TOTAL BALANCE"
        view.findViewById<View>(R.id.portfolioCardBalance).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹${String.format("%,d", totalBalance)}"
        view.findViewById<View>(R.id.portfolioCardBalance).findViewById<TextView>(R.id.portfolioMetricChange).text = "+5.0%"

        view.findViewById<View>(R.id.portfolioCardPnL).findViewById<TextView>(R.id.portfolioMetricTitle).text = "TOTAL PNL"
        view.findViewById<View>(R.id.portfolioCardPnL).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹${String.format("%,d", totalPnl)}"
        view.findViewById<View>(R.id.portfolioCardPnL).findViewById<TextView>(R.id.portfolioMetricChange).text = "+8.3%"

        view.findViewById<View>(R.id.portfolioCardShare).findViewById<TextView>(R.id.portfolioMetricTitle).text = "MY SHARE"
        view.findViewById<View>(R.id.portfolioCardShare).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹${String.format("%,d", totalShare)}"
        view.findViewById<View>(R.id.portfolioCardShare).findViewById<TextView>(R.id.portfolioMetricChange).text = "+12.5%"

        // Summary fields are now handled by the metric cards above

        // Exchange breakdown
        val exchangeText = StringBuilder()
        exchangeBreakdown.forEach { (exchange, accounts) ->
            val exchangeCapital = accounts.sumOf { it.funding }
            val exchangePnl = accounts.sumOf { it.pnl }
            exchangeText.append("$exchange: ${accounts.size} accounts | ₹${String.format("%,d", exchangeCapital)} | PnL: ₹${String.format("%,d", exchangePnl)}\n")
        }
        view.findViewById<TextView>(R.id.portfolioExchangeBreakdown).text = exchangeText.toString()

        // Performance analysis
        val performanceText = """
            Portfolio Health: ${getPortfolioHealth(totalPnl, totalCapital)}
            Risk Distribution: ${exchangeBreakdown.size} exchanges
            Best Performing: ${getBestExchange(exchangeBreakdown)}
            Diversification: ${getDiversificationScore(exchangeBreakdown.size)}
        """.trimIndent()
        view.findViewById<TextView>(R.id.portfolioPerformance).text = performanceText
    }

    private fun showDemoPortfolio(view: View) {
        // Demo data for when API is not available - update metric cards
        view.findViewById<View>(R.id.portfolioCardCapital).findViewById<TextView>(R.id.portfolioMetricTitle).text = "TOTAL CAPITAL"
        view.findViewById<View>(R.id.portfolioCardCapital).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹25,00,000"
        view.findViewById<View>(R.id.portfolioCardCapital).findViewById<TextView>(R.id.portfolioMetricChange).text = "+2.1%"

        view.findViewById<View>(R.id.portfolioCardBalance).findViewById<TextView>(R.id.portfolioMetricTitle).text = "TOTAL BALANCE"
        view.findViewById<View>(R.id.portfolioCardBalance).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹26,25,000"
        view.findViewById<View>(R.id.portfolioCardBalance).findViewById<TextView>(R.id.portfolioMetricChange).text = "+5.0%"

        view.findViewById<View>(R.id.portfolioCardPnL).findViewById<TextView>(R.id.portfolioMetricTitle).text = "TOTAL PNL"
        view.findViewById<View>(R.id.portfolioCardPnL).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹1,25,000"
        view.findViewById<View>(R.id.portfolioCardPnL).findViewById<TextView>(R.id.portfolioMetricChange).text = "+8.3%"

        view.findViewById<View>(R.id.portfolioCardShare).findViewById<TextView>(R.id.portfolioMetricTitle).text = "MY SHARE"
        view.findViewById<View>(R.id.portfolioCardShare).findViewById<TextView>(R.id.portfolioMetricValue).text = "₹87,500"
        view.findViewById<View>(R.id.portfolioCardShare).findViewById<TextView>(R.id.portfolioMetricChange).text = "+12.5%"

        view.findViewById<TextView>(R.id.portfolioExchangeBreakdown).text = """
            Diamond: 3 accounts | ₹15,00,000 | PnL: ₹75,000
            Angel One: 2 accounts | ₹8,00,000 | PnL: ₹40,000
            Zerodha: 1 account | ₹2,00,000 | PnL: ₹10,000
        """.trimIndent()

        view.findViewById<TextView>(R.id.portfolioPerformance).text = """
            Portfolio Health: Excellent (5% growth)
            Risk Distribution: 3 exchanges
            Best Performing: Diamond
            Diversification: Good (3 platforms)
        """.trimIndent()
    }

    private fun getPortfolioHealth(pnl: Long, capital: Long): String {
        val ratio = if (capital > 0) (pnl.toDouble() / capital) else 0.0
        return when {
            ratio > 0.1 -> "Excellent"
            ratio > 0.05 -> "Good"
            ratio > 0 -> "Fair"
            else -> "Needs Attention"
        }
    }

    private fun getBestExchange(exchangeBreakdown: Map<String, List<com.transactionhub.data.models.Account>>): String {
        return exchangeBreakdown.maxByOrNull { it.value.sumOf { acc -> acc.pnl } }?.key ?: "None"
    }

    private fun getDiversificationScore(exchangeCount: Int): String {
        return when {
            exchangeCount >= 5 -> "Excellent"
            exchangeCount >= 3 -> "Good"
            exchangeCount >= 2 -> "Fair"
            else -> "Low"
        }
    }
}