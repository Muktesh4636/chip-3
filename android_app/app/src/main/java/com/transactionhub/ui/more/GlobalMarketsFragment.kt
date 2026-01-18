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

class GlobalMarketsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_global_markets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.globalTitle).text = "Multi-Currency & Global Markets"

        loadGlobalData(view)
        setupGlobalControls(view)
    }

    private fun loadGlobalData(view: View) {
        // Exchange Rates
        view.findViewById<TextView>(R.id.exchangeRates).text = """
        💱 LIVE EXCHANGE RATES

        🇺🇸 USD/INR: 83.45 (+0.12%)
        🇪🇺 EUR/INR: 90.12 (-0.05%)
        🇬🇧 GBP/INR: 105.67 (+0.23%)
        🇦🇪 AED/INR: 22.72 (0.00%)
        🇸🇬 SGD/INR: 62.34 (+0.08%)

        📊 Currency Trend: INR strengthening against Major Majors
        ⏱️ Last Updated: 30 seconds ago
        """.trimIndent()

        // Global Market Status
        view.findViewById<TextView>(R.id.globalMarketStatus).text = """
        🌍 GLOBAL MARKET STATUS

        🇺🇸 NYSE: OPEN | +1.2% (Bullish)
        🇺🇸 NASDAQ: OPEN | +1.5% (Bullish)
        🇬🇧 FTSE 100: CLOSED | -0.2% (Bearish)
        🇯🇵 NIKKEI 225: CLOSED | +0.8% (Neutral)
        🇮🇳 NIFTY 50: CLOSED | +0.5% (Bullish)

        🛢️ BRENT CRUDE: $78.45 (-1.2%)
        🟡 GOLD: $2,045.60 (+0.3%)
        🪙 BITCOIN: $42,670 (+2.1%)
        """.trimIndent()

        // International Portfolio
        view.findViewById<TextView>(R.id.internationalPortfolio).text = """
        🌐 INTERNATIONAL PORTFOLIO SUMMARY

        💵 USD Assets: $45,670 (₹38,11,161)
        💶 EUR Assets: €12,450 (₹11,21,994)
        💷 GBP Assets: £5,670 (₹5,99,148)

        📈 Total Global Value: ₹55,32,303
        🌍 Exposure: 45% International, 55% Domestic
        🛡️ Currency Hedge: 65% coverage
        """.trimIndent()

        // Market Insights
        view.findViewById<TextView>(R.id.marketInsights).text = """
        💡 GLOBAL MARKET INSIGHTS

        🚀 Fed meeting indicates possible rate cut in Q2
        📉 Oil prices drop on higher supply projections
        📈 Tech sector rallies on strong earnings reports
        ⚠️ Geopolitical tensions in Red Sea impacting trade

        🎯 Recommended Action: Maintain USD exposure, Hedge EUR
        """.trimIndent()
    }

    private fun setupGlobalControls(view: View) {
        view.findViewById<Button>(R.id.btnConvertCurrency).setOnClickListener {
            convertCurrency()
        }

        view.findViewById<Button>(R.id.btnViewMarketNews).setOnClickListener {
            viewMarketNews()
        }

        view.findViewById<Button>(R.id.btnGlobalReports).setOnClickListener {
            showGlobalReports()
        }

        view.findViewById<Button>(R.id.btnGlobalSettings).setOnClickListener {
            globalSettings()
        }
    }

    private fun convertCurrency() {
        val currencies = arrayOf(
            "USD to INR",
            "EUR to INR",
            "GBP to INR",
            "AED to INR",
            "SGD to INR",
            "Custom Conversion"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Currency Converter")
            .setItems(currencies) { _, which ->
                Toast.makeText(context, "Calculating ${currencies[which]}...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Live Rates", null)
            .show()
    }

    private fun viewMarketNews() {
        val newsSources = arrayOf(
            "Bloomberg Finance",
            "Reuters Markets",
            "Economic Times",
            "Financial Times",
            "CNBC Live"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Global Market News")
            .setItems(newsSources) { _, which ->
                Toast.makeText(context, "Opening ${newsSources[which]}...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showGlobalReports() {
        val report = """
        GLOBAL MARKETS ANALYSIS REPORT

        📊 PORTFOLIO EXPOSURE:
        • Domestic Assets: 55% (₹67.5 Lakhs)
        • International Assets: 45% (₹55.3 Lakhs)
        • Major Currency: USD (32% of total)

        📉 PERFORMANCE BY REGION:
        • US Markets: +12.4% (YTD)
        • EU Markets: +4.2% (YTD)
        • Asian Markets: +8.7% (YTD)
        • Indian Markets: +15.2% (YTD)

        🛡️ CURRENCY IMPACT:
        • FX Gain/Loss: +₹1.2 Lakhs (USD strength)
        • Hedging Savings: ₹45,600
        • Net Global Return: +11.8%

        📈 STRATEGIC RECOMMENDATIONS:
        • Increase exposure to US Tech ETFs
        • Hedge 100% of EUR exposure due to volatility
        • Maintain physical Gold as a safe haven
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Global Performance Analytics")
            .setMessage(report)
            .setPositiveButton("Download CSV", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun globalSettings() {
        val settings = arrayOf(
            "Base Currency (INR)",
            "Live Rate Provider",
            "Auto-Hedge Rules",
            "Market Hours Alerts",
            "Regional Reporting Config",
            "API Data Refresh Interval"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Global Markets Settings")
            .setItems(settings) { _, which ->
                Toast.makeText(context, "Opening ${settings[which]} settings...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}