package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Account
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ClientDetailReportFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    private var clientId: Int = -1
    private var clientName: String = ""

    companion object {
        fun newInstance(clientId: Int, clientName: String): ClientDetailReportFragment {
            val fragment = ClientDetailReportFragment()
            val args = Bundle()
            args.putInt("clientId", clientId)
            args.putString("clientName", clientName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientId = arguments?.getInt("clientId") ?: -1
        clientName = arguments?.getString("clientName") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_client_detail_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.clientDetailReportName).text = "$clientName - Performance Report"

        loadClientReport(view)
    }

    private fun loadClientReport(view: View) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val allAccounts = response.body() ?: emptyList()
                    val clientAccounts = allAccounts.filter { it.client == clientId }

                    // Calculate totals
                    val totalFunding = clientAccounts.sumOf { it.funding }
                    val totalPnL = clientAccounts.sumOf { it.pnl }
                    val totalShare = clientAccounts.sumOf { it.my_share }

                    // Update stat cards
                    view.findViewById<View>(R.id.clientReportTotalAccountsCard).findViewById<TextView>(R.id.statTitle).text = "TOTAL ACCOUNTS"
                    view.findViewById<View>(R.id.clientReportTotalAccountsCard).findViewById<TextView>(R.id.statValue).text = clientAccounts.size.toString()

                    view.findViewById<View>(R.id.clientReportTotalFundingCard).findViewById<TextView>(R.id.statTitle).text = "TOTAL FUNDING"
                    view.findViewById<View>(R.id.clientReportTotalFundingCard).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalFunding)}"

                    view.findViewById<View>(R.id.clientReportTotalPnLCard).findViewById<TextView>(R.id.statTitle).text = "TOTAL PNL"
                    view.findViewById<View>(R.id.clientReportTotalPnLCard).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalPnL)}"
                    view.findViewById<View>(R.id.clientReportTotalPnLCard).findViewById<TextView>(R.id.statValue).setTextColor(
                        resources.getColor(if (totalPnL < 0) R.color.danger else R.color.success, null)
                    )

                    view.findViewById<View>(R.id.clientReportTotalShareCard).findViewById<TextView>(R.id.statTitle).text = "TOTAL MY SHARE"
                    view.findViewById<View>(R.id.clientReportTotalShareCard).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalShare)}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}