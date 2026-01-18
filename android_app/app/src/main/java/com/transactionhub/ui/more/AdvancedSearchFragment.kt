package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Client
import com.transactionhub.data.models.Exchange
import com.transactionhub.data.models.Transaction
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class AdvancedSearchFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchResultsAdapter

    private var clients: List<Client> = emptyList()
    private var exchanges: List<Exchange> = emptyList()
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_advanced_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.advancedSearchTitle).text = "Advanced Search & Filter"

        recyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = SearchResultsAdapter(emptyList())
        recyclerView.adapter = adapter

        setupFilters(view)
        loadData()
    }

    private fun setupFilters(view: View) {
        val spinnerClient = view.findViewById<Spinner>(R.id.spinnerFilterClient)
        val spinnerExchange = view.findViewById<Spinner>(R.id.spinnerFilterExchange)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerFilterType)
        val editSearchText = view.findViewById<EditText>(R.id.editSearchText)
        val btnSearch = view.findViewById<Button>(R.id.btnPerformSearch)
        val btnClear = view.findViewById<Button>(R.id.btnClearFilters)

        // Setup type spinner
        val transactionTypes = arrayOf("All Types", "FUNDING", "TRADE", "RECORD_PAYMENT", "SETTLEMENT_SHARE")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, transactionTypes)
        spinnerType.adapter = typeAdapter

        btnSearch.setOnClickListener {
            performSearch(view)
        }

        btnClear.setOnClickListener {
            clearFilters(view)
        }
    }

    private fun loadData() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                // Load clients
                val clientsResponse = apiService.getClients(ApiClient.getAuthToken(token))
                if (clientsResponse.isSuccessful) {
                    clients = clientsResponse.body() ?: emptyList()
                    setupClientSpinner()
                }

                // Load exchanges
                val exchangesResponse = apiService.getExchanges(ApiClient.getAuthToken(token))
                if (exchangesResponse.isSuccessful) {
                    exchanges = exchangesResponse.body() ?: emptyList()
                    setupExchangeSpinner()
                }

                // Load transactions
                val transactionsResponse = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (transactionsResponse.isSuccessful) {
                    allTransactions = transactionsResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupClientSpinner() {
        val view = view ?: return
        val spinnerClient = view.findViewById<Spinner>(R.id.spinnerFilterClient)

        val clientNames = mutableListOf("All Clients")
        clientNames.addAll(clients.map { it.name })

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, clientNames)
        spinnerClient.adapter = adapter
    }

    private fun setupExchangeSpinner() {
        val view = view ?: return
        val spinnerExchange = view.findViewById<Spinner>(R.id.spinnerFilterExchange)

        val exchangeNames = mutableListOf("All Exchanges")
        exchangeNames.addAll(exchanges.map { it.name })

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, exchangeNames)
        spinnerExchange.adapter = adapter
    }

    private fun performSearch(view: View) {
        val spinnerClient = view.findViewById<Spinner>(R.id.spinnerFilterClient)
        val spinnerExchange = view.findViewById<Spinner>(R.id.spinnerFilterExchange)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerFilterType)
        val editSearchText = view.findViewById<EditText>(R.id.editSearchText)

        val selectedClientIndex = spinnerClient.selectedItemPosition
        val selectedExchangeIndex = spinnerExchange.selectedItemPosition
        val selectedType = spinnerType.selectedItem.toString()
        val searchText = editSearchText.text.toString().lowercase()

        var filteredTransactions = allTransactions

        // Filter by client
        if (selectedClientIndex > 0) {
            val selectedClient = clients[selectedClientIndex - 1]
            filteredTransactions = filteredTransactions.filter { it.client_name == selectedClient.name }
        }

        // Filter by exchange
        if (selectedExchangeIndex > 0) {
            val selectedExchange = exchanges[selectedExchangeIndex - 1]
            filteredTransactions = filteredTransactions.filter { it.exchange_name == selectedExchange.name }
        }

        // Filter by type
        if (selectedType != "All Types") {
            filteredTransactions = filteredTransactions.filter { it.type == selectedType }
        }

        // Filter by search text
        if (searchText.isNotEmpty()) {
            filteredTransactions = filteredTransactions.filter {
                it.client_name.lowercase().contains(searchText) ||
                it.exchange_name.lowercase().contains(searchText) ||
                it.type_display.lowercase().contains(searchText) ||
                (it.notes?.lowercase()?.contains(searchText) ?: false)
            }
        }

        adapter.updateTransactions(filteredTransactions.take(100)) // Limit results

        view.findViewById<TextView>(R.id.searchResultsCount).text =
            "Found ${filteredTransactions.size} transactions (showing ${minOf(filteredTransactions.size, 100)})"
    }

    private fun clearFilters(view: View) {
        val spinnerClient = view.findViewById<Spinner>(R.id.spinnerFilterClient)
        val spinnerExchange = view.findViewById<Spinner>(R.id.spinnerFilterExchange)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerFilterType)
        val editSearchText = view.findViewById<EditText>(R.id.editSearchText)

        spinnerClient.setSelection(0)
        spinnerExchange.setSelection(0)
        spinnerType.setSelection(0)
        editSearchText.text.clear()

        adapter.updateTransactions(emptyList())
        view.findViewById<TextView>(R.id.searchResultsCount).text = "Apply filters to search transactions"
    }
}

class SearchResultsAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeText: TextView = itemView.findViewById(R.id.transactionTypeText)
        val detailsText: TextView = itemView.findViewById(R.id.transactionDetailsText)
        val dateText: TextView = itemView.findViewById(R.id.transactionDateText)
        val amountText: TextView = itemView.findViewById(R.id.transactionAmountText)
        val seqText: TextView = itemView.findViewById(R.id.txnSeqNo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = transactions[position]
        holder.typeText.text = txn.type_display
        holder.detailsText.text = "${txn.client_name} | ${txn.exchange_name}"
        holder.dateText.text = txn.date
        holder.amountText.text = "₹${String.format("%,d", txn.amount)}"
        holder.seqText.text = "#${txn.sequence_no}"

        // Color coding for transactions
        if (txn.type.contains("SETTLEMENT") || txn.type.contains("PAYMENT")) {
            holder.amountText.setTextColor(holder.itemView.resources.getColor(R.color.danger, null))
        } else if (txn.type.contains("FUNDING") || txn.type.contains("PROFIT")) {
            holder.amountText.setTextColor(holder.itemView.resources.getColor(R.color.success, null))
        } else {
            holder.amountText.setTextColor(holder.itemView.resources.getColor(R.color.text_main, null))
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}