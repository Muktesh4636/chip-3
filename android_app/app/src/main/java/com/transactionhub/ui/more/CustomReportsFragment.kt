package com.transactionhub.ui.more

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CustomReportsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    private var fromDate: Calendar = Calendar.getInstance()
    private var toDate: Calendar = Calendar.getInstance()
    private var selectedFilter: String = "all"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_custom_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        // Set default dates (last 30 days)
        fromDate.add(Calendar.DAY_OF_MONTH, -30)

        setupDatePickers(view)
        setupFilters(view)
        setupGenerateButton(view)

        // Set initial button texts
        updateDateButtons(view)
    }

    private fun setupDatePickers(view: View) {
        val btnFromDate = view.findViewById<Button>(R.id.btnFromDate)
        val btnToDate = view.findViewById<Button>(R.id.btnToDate)

        btnFromDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    fromDate.set(year, month, day)
                    updateDateButtons(view)
                },
                fromDate.get(Calendar.YEAR),
                fromDate.get(Calendar.MONTH),
                fromDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnToDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    toDate.set(year, month, day)
                    updateDateButtons(view)
                },
                toDate.get(Calendar.YEAR),
                toDate.get(Calendar.MONTH),
                toDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateButtons(view: View) {
        val btnFromDate = view.findViewById<Button>(R.id.btnFromDate)
        val btnToDate = view.findViewById<Button>(R.id.btnToDate)

        btnFromDate.text = dateFormat.format(fromDate.time)
        btnToDate.text = dateFormat.format(toDate.time)
    }

    private fun setupFilters(view: View) {
        val btnAll = view.findViewById<Button>(R.id.btnFilterAll)
        val btnClient = view.findViewById<Button>(R.id.btnFilterClient)
        val btnExchange = view.findViewById<Button>(R.id.btnFilterExchange)

        val buttons = listOf(btnAll, btnClient, btnExchange)

        btnAll.setOnClickListener {
            selectedFilter = "all"
            updateFilterButtons(buttons, btnAll)
        }

        btnClient.setOnClickListener {
            selectedFilter = "client"
            updateFilterButtons(buttons, btnClient)
            // TODO: Show client picker
        }

        btnExchange.setOnClickListener {
            selectedFilter = "exchange"
            updateFilterButtons(buttons, btnExchange)
            // TODO: Show exchange picker
        }
    }

    private fun updateFilterButtons(allButtons: List<Button>, selected: Button) {
        allButtons.forEach { button ->
            if (button == selected) {
                button.setBackgroundColor(resources.getColor(R.color.primary, null))
                button.setTextColor(resources.getColor(R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(R.color.sidebar_bg, null))
                button.setTextColor(resources.getColor(R.color.text_main, null))
            }
        }
    }

    private fun setupGenerateButton(view: View) {
        view.findViewById<Button>(R.id.btnGenerateReport).setOnClickListener {
            if (fromDate.after(toDate)) {
                Toast.makeText(context, "From date cannot be after To date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            generateReport(view)
        }
    }

    private fun generateReport(view: View) {
        val token = prefManager.getToken() ?: return

        val fromDateStr = dateFormat.format(fromDate.time)
        val toDateStr = dateFormat.format(toDate.time)

        lifecycleScope.launch {
            try {
                // Use custom reports API
                val response = apiService.getCustomReports(
                    ApiClient.getAuthToken(token),
                    fromDateStr,
                    toDateStr
                )

                if (response.isSuccessful) {
                    val data = response.body() ?: return@launch
                    val overview = data["overview"] as? Map<String, Any>

                    if (overview != null) {
                        displayReportResults(view, overview, data["transactions"] as? List<Map<String, Any>>)
                    }
                } else {
                    Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Report generation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayReportResults(view: View, overview: Map<String, Any>, transactions: List<Map<String, Any>>?) {
        // Update summary cards
        val totalFunding = (overview["total_funding"] as? Number)?.toLong() ?: 0L
        val totalPnL = (overview["total_pnl"] as? Number)?.toLong() ?: 0L
        val totalMyShare = (overview["total_my_share"] as? Number)?.toLong() ?: 0L
        val myOwnShare = (overview["my_own_share"] as? Number)?.toLong() ?: 0L
        val friendShare = (overview["friend_share"] as? Number)?.toLong() ?: 0L

        view.findViewById<View>(R.id.customTotalFunding).findViewById<TextView>(R.id.statTitle).text = "TOTAL CAPITAL"
        view.findViewById<View>(R.id.customTotalFunding).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalFunding)}"

        view.findViewById<View>(R.id.customTotalPnL).findViewById<TextView>(R.id.statTitle).text = "TOTAL PNL"
        view.findViewById<View>(R.id.customTotalPnL).findViewById<TextView>(R.id.statValue).text = "₹${String.format("%,d", totalPnL)}"
        view.findViewById<View>(R.id.customTotalPnL).findViewById<TextView>(R.id.statValue).setTextColor(
            resources.getColor(if (totalPnL < 0) R.color.danger else R.color.success, null)
        )

        view.findViewById<View>(R.id.customMyShare).findViewById<TextView>(R.id.repTotalMyShare).text = "₹${String.format("%,d", totalMyShare)}"
        view.findViewById<View>(R.id.customMyShare).findViewById<TextView>(R.id.repMyOwnSplit).text = "₹${String.format("%,d", myOwnShare)}"
        view.findViewById<View>(R.id.customMyShare).findViewById<TextView>(R.id.repFriendSplit).text = "₹${String.format("%,d", friendShare)}"

        // Display transactions
        val container = view.findViewById<LinearLayout>(R.id.customTransactionsContainer)
        container.removeAllViews()

        transactions?.take(20)?.forEach { txn ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_transaction, container, false)

            row.findViewById<TextView>(R.id.transactionTypeText).text = txn["type_display"] as? String ?: ""
            row.findViewById<TextView>(R.id.transactionDetailsText).text = "${txn["client_name"]} | ${txn["exchange_name"]}"
            row.findViewById<TextView>(R.id.transactionDateText).text = txn["date"] as? String ?: ""
            row.findViewById<TextView>(R.id.transactionAmountText).text = "₹${String.format("%,d", (txn["amount"] as? Number)?.toLong() ?: 0L)}"
            row.findViewById<TextView>(R.id.txnSeqNo).visibility = View.GONE

            container.addView(row)
        }
    }
}