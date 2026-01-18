package com.transactionhub.ui.exchanges

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

class ExchangeDetailFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExchangeAccountsAdapter

    private var exchangeId: Int = -1
    private var exchangeName: String = ""
    private var exchangeCode: String = ""

    companion object {
        fun newInstance(id: Int, name: String, code: String?): ExchangeDetailFragment {
            val fragment = ExchangeDetailFragment()
            val args = Bundle()
            args.putInt("id", id)
            args.putString("name", name)
            args.putString("code", code)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exchangeId = arguments?.getInt("id") ?: -1
        exchangeName = arguments?.getString("name") ?: ""
        exchangeCode = arguments?.getString("code") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_exchange_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.exchangeDetailName).text = exchangeName
        view.findViewById<TextView>(R.id.exchangeDetailCode).text = "Code: ${exchangeCode.ifEmpty { "---" }}"

        recyclerView = view.findViewById(R.id.exchangeAccountsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExchangeAccountsAdapter(emptyList())
        recyclerView.adapter = adapter

        loadExchangeAccounts()
    }

    private fun loadExchangeAccounts() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val allAccounts = response.body() ?: emptyList()
                    val filteredAccounts = allAccounts.filter { it.exchange == exchangeId }
                    adapter.updateAccounts(filteredAccounts)

                    // Update summary
                    val totalClients = filteredAccounts.size
                    val totalFunding = filteredAccounts.sumOf { it.funding }
                    val totalBalance = filteredAccounts.sumOf { it.exchange_balance }

                    view?.findViewById<TextView>(R.id.exchangeTotalClients)?.text = "Total Clients: $totalClients"
                    view?.findViewById<TextView>(R.id.exchangeTotalFunding)?.text = "₹${String.format("%,d", totalFunding)}"
                    view?.findViewById<TextView>(R.id.exchangeTotalBalance)?.text = "₹${String.format("%,d", totalBalance)}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ExchangeAccountsAdapter(
    private var accounts: List<Account>
) : RecyclerView.Adapter<ExchangeAccountsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clientName: TextView = itemView.findViewById(R.id.accountClientName)
        val funding: TextView = itemView.findViewById(R.id.accountFunding)
        val balance: TextView = itemView.findViewById(R.id.accountBalance)
        val pnl: TextView = itemView.findViewById(R.id.accountPnL)
        val share: TextView = itemView.findViewById(R.id.accountShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exchange_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.clientName.text = account.client_name
        holder.funding.text = "₹${String.format("%,d", account.funding)}"
        holder.balance.text = "₹${String.format("%,d", account.exchange_balance)}"
        holder.pnl.text = "₹${String.format("%,d", account.pnl)}"
        holder.share.text = "₹${String.format("%,d", account.my_share)}"

        if (account.pnl < 0) {
            holder.pnl.setTextColor(holder.itemView.resources.getColor(R.color.danger, null))
        } else {
            holder.pnl.setTextColor(holder.itemView.resources.getColor(R.color.success, null))
        }
    }

    override fun getItemCount() = accounts.size

    fun updateAccounts(newAccounts: List<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }
}