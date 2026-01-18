package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

import android.widget.Button

class ReportsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private var currentView = "DAILY"
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        setupTabs(view)
        loadReports(view)
    }

    private fun setupTabs(view: View) {
        val btnDaily = view.findViewById<Button>(R.id.btnDaily)
        val btnWeekly = view.findViewById<Button>(R.id.btnWeekly)
        val btnMonthly = view.findViewById<Button>(R.id.btnMonthly)

        val buttons = listOf(btnDaily, btnWeekly, btnMonthly)

        btnDaily.setOnClickListener { 
            updateTabUI(btnDaily, buttons)
            currentView = "DAILY"
            loadReports(view)
        }
        btnWeekly.setOnClickListener { 
            updateTabUI(btnWeekly, buttons)
            currentView = "WEEKLY"
            loadReports(view)
        }
        btnMonthly.setOnClickListener { 
            updateTabUI(btnMonthly, buttons)
            currentView = "MONTHLY"
            loadReports(view)
        }
    }

    private fun updateTabUI(selected: Button, all: List<Button>) {
        all.forEach {
            it.setTextColor(resources.getColor(if (it == selected) R.color.primary else R.color.text_muted, null))
        }
    }

    private fun loadReports(view: View) {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = apiService.getReportsSummary(ApiClient.getAuthToken(token), currentView)
                if (response.isSuccessful) {
                    val data = response.body() ?: return@launch
                    val overview = data["overview"] as? Map<String, Any>
                    
                    if (overview != null) {
                        val totalFunding = (overview["total_funding"] as? Number)?.toLong() ?: 0L
                        val totalPnL = (overview["total_pnl"] as? Number)?.toLong() ?: 0L
                        val totalMyShare = (overview["total_my_share"] as? Number)?.toLong() ?: 0L
                        val myOwnShare = (overview["my_own_share"] as? Number)?.toLong() ?: 0L
                        val friendShare = (overview["friend_share"] as? Number)?.toLong() ?: 0L

                        view.findViewById<View>(R.id.repTotalFunding).findViewById<TextView>(R.id.statTitle).text = "TOTAL CAPITAL"
                        view.findViewById<View>(R.id.repTotalFunding).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalFunding)}"
                        
                        view.findViewById<View>(R.id.repTotalPnL).findViewById<TextView>(R.id.statTitle).text = "GLOBAL PNL"
                        view.findViewById<View>(R.id.repTotalPnL).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalPnL)}"
                        view.findViewById<View>(R.id.repTotalPnL).findViewById<TextView>(R.id.statValue).setTextColor(
                            resources.getColor(if (totalPnL < 0) R.color.danger else R.color.success, null)
                        )
                        
                        view.findViewById<View>(R.id.repMyShareLayout).findViewById<TextView>(R.id.repTotalMyShare).text = "₹${String.format("%,d", totalMyShare)}"
                        view.findViewById<View>(R.id.repMyShareLayout).findViewById<TextView>(R.id.repMyOwnSplit).text = "₹${String.format("%,d", myOwnShare)}"
                        view.findViewById<View>(R.id.repMyShareLayout).findViewById<TextView>(R.id.repFriendSplit).text = "₹${String.format("%,d", friendShare)}"

                        // Daily Performance
                        val dailyPerf = data["daily_performance"] as? List<Map<String, Any>>
                        val container = view.findViewById<LinearLayout>(R.id.dailyStatsContainer)
                        container.removeAllViews()
                        
                        dailyPerf?.forEach { day ->
                            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_transaction, container, false)
                            val date = day["date"] as? String ?: ""
                            val pnl = (day["pnl"] as? Number)?.toLong() ?: 0L
                            val txCount = (day["tx_count"] as? Number)?.toInt() ?: 0
                            
                            row.findViewById<TextView>(R.id.transactionTypeText).text = "PnL Outcome"
                            row.findViewById<TextView>(R.id.transactionDetailsText).text = "$txCount transactions"
                            row.findViewById<TextView>(R.id.transactionDateText).text = date
                            row.findViewById<TextView>(R.id.transactionAmountText).text = "₹${String.format("%,d", pnl)}"
                            row.findViewById<TextView>(R.id.transactionAmountText).setTextColor(
                                resources.getColor(if (pnl < 0) R.color.danger else R.color.success, null)
                            )
                            row.findViewById<TextView>(R.id.txnSeqNo).visibility = View.GONE
                            
                            container.addView(row)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
