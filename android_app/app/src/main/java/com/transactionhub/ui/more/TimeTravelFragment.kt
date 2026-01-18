package com.transactionhub.ui.more

import android.app.DatePickerDialog
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
import com.transactionhub.data.models.Transaction
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TimeTravelFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimeTravelAdapter

    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_time_travel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.timeTravelTitle).text = "Time Travel - Historical Data"

        recyclerView = view.findViewById(R.id.timeTravelRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TimeTravelAdapter(emptyList())
        recyclerView.adapter = adapter

        // Set default date to yesterday
        selectedDate.add(Calendar.DAY_OF_MONTH, -1)
        updateDateDisplay(view)

        view.findViewById<Button>(R.id.btnSelectDate).setOnClickListener {
            showDatePicker(view)
        }

        view.findViewById<Button>(R.id.btnTravelBack).setOnClickListener {
            performTimeTravel(view)
        }

        // Initialize stat cards
        view.findViewById<View>(R.id.historicalFundingCard).findViewById<TextView>(R.id.statTitle).text = "HISTORICAL FUNDING"
        view.findViewById<View>(R.id.historicalBalanceCard).findViewById<TextView>(R.id.statTitle).text = "HISTORICAL BALANCE"
        view.findViewById<View>(R.id.historicalPnLCard).findViewById<TextView>(R.id.statTitle).text = "HISTORICAL PNL"

        // Auto-load yesterday's data
        performTimeTravel(view)
    }

    private fun showDatePicker(view: View) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay(view)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay(view: View) {
        view.findViewById<Button>(R.id.btnSelectDate).text = dateFormat.format(selectedDate.time)
    }

    private fun performTimeTravel(view: View) {
        val token = prefManager.getToken() ?: return
        val dateStr = dateFormat.format(selectedDate.time)

        view.findViewById<TextView>(R.id.timeTravelStatus).text = "Loading historical data for $dateStr..."
        view.findViewById<ProgressBar>(R.id.timeTravelProgress).visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Get transactions up to the selected date
                val response = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val allTransactions = response.body() ?: emptyList()

                    // Filter transactions that existed on or before the selected date
                    val historicalTransactions = allTransactions.filter { txn ->
                        try {
                            val txnDate = dateFormat.parse(txn.date.substring(0, 10))
                            val selectedDateTime = selectedDate.time
                            txnDate != null && !txnDate.after(selectedDateTime)
                        } catch (e: Exception) {
                            false
                        }
                    }.take(50) // Limit for performance

                    adapter.updateTransactions(historicalTransactions)

                    view.findViewById<TextView>(R.id.timeTravelStatus).text =
                        "Showing ${historicalTransactions.size} transactions as of $dateStr"

                    // Calculate historical summary
                    calculateHistoricalSummary(view, historicalTransactions)
                } else {
                    view.findViewById<TextView>(R.id.timeTravelStatus).text = "Failed to load historical data"
                }
            } catch (e: Exception) {
                view.findViewById<TextView>(R.id.timeTravelStatus).text = "Error: ${e.message}"
            } finally {
                view.findViewById<ProgressBar>(R.id.timeTravelProgress).visibility = View.GONE
            }
        }
    }

    private fun calculateHistoricalSummary(view: View, transactions: List<Transaction>) {
        // Calculate totals from historical transactions
        var totalFunding = 0L
        var totalBalance = 0L
        var totalPnL = 0L

        // Group by client-exchange pairs and get latest balances
        val accountBalances = mutableMapOf<String, Pair<Long, Long>>() // funding to balance

        transactions.forEach { txn ->
            val accountKey = "${txn.client_name}|${txn.exchange_name}"

            // Update funding
            if (txn.funding_after != null) {
                accountBalances[accountKey] = Pair(
                    txn.funding_after!!,
                    accountBalances[accountKey]?.second ?: txn.exchange_balance_after ?: 0L
                )
            }

            // Update balance
            if (txn.exchange_balance_after != null) {
                accountBalances[accountKey] = Pair(
                    accountBalances[accountKey]?.first ?: txn.funding_after ?: 0L,
                    txn.exchange_balance_after!!
                )
            }
        }

        // Calculate totals
        accountBalances.values.forEach { (funding, balance) ->
            totalFunding += funding
            totalBalance += balance
            totalPnL += (balance - funding)
        }

        view.findViewById<View>(R.id.historicalFundingCard).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalFunding)}"
        view.findViewById<View>(R.id.historicalBalanceCard).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalBalance)}"

        val pnlCard = view.findViewById<View>(R.id.historicalPnLCard)
        pnlCard.findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalPnL)}"
        pnlCard.findViewById<TextView>(R.id.statValue).setTextColor(
            resources.getColor(if (totalPnL < 0) R.color.danger else R.color.success, null)
        )
    }
}

class TimeTravelAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<TimeTravelAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeText: TextView = itemView.findViewById(R.id.transactionTypeText)
        val detailsText: TextView = itemView.findViewById(R.id.transactionDetailsText)
        val dateText: TextView = itemView.findViewById(R.id.transactionDateText)
        val amountText: TextView = itemView.findViewById(R.id.transactionAmountText)
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