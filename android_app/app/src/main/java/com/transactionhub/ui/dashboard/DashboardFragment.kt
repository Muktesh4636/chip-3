package com.transactionhub.ui.dashboard

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

class DashboardFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    
    private lateinit var totalClientsValue: TextView
    private lateinit var totalFundingValue: TextView
    private lateinit var totalBalanceValue: TextView
    private lateinit var totalPnLValue: TextView
    private lateinit var totalShareValue: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        // Setup Card Titles
        view.findViewById<View>(R.id.cardClients).findViewById<TextView>(R.id.statTitle).text = "Total Clients"
        view.findViewById<View>(R.id.cardFunding).findViewById<TextView>(R.id.statTitle).text = "Total Funding"
        view.findViewById<View>(R.id.cardBalance).findViewById<TextView>(R.id.statTitle).text = "Total Balance"
        view.findViewById<View>(R.id.cardPnL).findViewById<TextView>(R.id.statTitle).text = "Total Profit/Loss"
        view.findViewById<View>(R.id.cardShare).findViewById<TextView>(R.id.statTitle).text = "My Share"

        // Get Value TextViews
        totalClientsValue = view.findViewById<View>(R.id.cardClients).findViewById(R.id.statValue)
        totalFundingValue = view.findViewById<View>(R.id.cardFunding).findViewById(R.id.statValue)
        totalBalanceValue = view.findViewById<View>(R.id.cardBalance).findViewById(R.id.statValue)
        totalPnLValue = view.findViewById<View>(R.id.cardPnL).findViewById(R.id.statValue)
        totalShareValue = view.findViewById<View>(R.id.cardShare).findViewById(R.id.statValue)
        
        loadDashboard()
    }
    
    private fun loadDashboard() {
        val token = prefManager.getToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = apiService.getDashboard(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val data = response.body()!!
                    updateUI(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun updateUI(data: com.transactionhub.data.models.DashboardResponse) {
        totalClientsValue.text = data.total_clients.toString()
        totalFundingValue.text = "₹${formatNumber(data.total_funding)}"
        totalBalanceValue.text = "₹${formatNumber(data.total_balance)}"
        totalPnLValue.text = "₹${formatNumber(data.total_pnl)}"
        totalShareValue.text = "₹${formatNumber(data.total_my_share)}"
        
        // Color code PnL
        if (data.total_pnl < 0) {
            totalPnLValue.setTextColor(resources.getColor(R.color.danger, null))
        } else if (data.total_pnl > 0) {
            totalPnLValue.setTextColor(resources.getColor(R.color.success, null))
        }
    }
    
    private fun formatNumber(number: Long): String {
        return String.format("%,d", number)
    }
}
