package com.transactionhub.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Exchange
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class LinkAccountFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private var clientId: Int = -1
    private var clientName: String = ""
    private var exchanges: List<Exchange> = emptyList()
    
    companion object {
        fun newInstance(clientId: Int, clientName: String): LinkAccountFragment {
            val fragment = LinkAccountFragment()
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
        return inflater.inflate(R.layout.fragment_link_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        view.findViewById<TextView>(R.id.linkClientName).text = "Client: $clientName"
        
        loadExchanges(view)
        
        view.findViewById<Button>(R.id.btnLinkAccount).setOnClickListener {
            val spinner = view.findViewById<Spinner>(R.id.spinnerExchanges)
            val funding = view.findViewById<EditText>(R.id.editInitialFunding).text.toString()
            val profitShare = view.findViewById<EditText>(R.id.editProfitShare).text.toString()
            val lossShare = view.findViewById<EditText>(R.id.editLossShare).text.toString()

            if (spinner.selectedItemPosition < 0 || exchanges.isEmpty()) {
                Toast.makeText(context, "Select an exchange", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (profitShare.isEmpty() || lossShare.isEmpty()) {
                Toast.makeText(context, "Enter profit and loss share percentages", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profitPct = profitShare.toIntOrNull() ?: 0
            val lossPct = lossShare.toIntOrNull() ?: 0

            if (profitPct < 0 || profitPct > 100 || lossPct < 0 || lossPct > 100) {
                Toast.makeText(context, "Percentages must be between 0-100", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exchange = exchanges[spinner.selectedItemPosition]
            performLink(exchange.id, funding, profitPct, lossPct)
        }
    }

    private fun loadExchanges(view: View) {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = apiService.getExchanges(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    exchanges = response.body() ?: emptyList()
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, exchanges.map { it.name })
                    view.findViewById<Spinner>(R.id.spinnerExchanges).adapter = adapter
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun performLink(exchangeId: Int, funding: String, profitShare: Int, lossShare: Int) {
        val token = prefManager.getToken() ?: return
        val data = mapOf(
            "client_id" to clientId.toString(),
            "exchange_id" to exchangeId.toString(),
            "funding" to (if (funding.isEmpty()) "0" else funding),
            "profit_share_percentage" to profitShare.toString(),
            "loss_share_percentage" to lossShare.toString()
        )
        
        lifecycleScope.launch {
            try {
                val response = apiService.linkAccount(ApiClient.getAuthToken(token), data)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Account Linked!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Error: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
