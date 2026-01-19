package com.transactionhub.ui.payments

import android.content.Intent
import android.net.Uri
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
import com.transactionhub.data.models.PendingPaymentItem
import com.transactionhub.data.models.RecordPaymentRequest
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.transactionhub.ui.accounts.AccountDetailFragment

class PendingPaymentsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PendingPaymentsAdapter
    private lateinit var toReceiveText: TextView
    private lateinit var toPayText: TextView
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var clearSearchButton: Button
    private lateinit var btnClientsOwe: Button
    private lateinit var btnYouOwe: Button
    private lateinit var exportCsvButton: Button

    private var currentSection = "clients_owe" // clients_owe or you_owe
    private var currentSearchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pending_payments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        // Initialize views
        toReceiveText = view.findViewById(R.id.toReceiveText)
        toPayText = view.findViewById(R.id.toPayText)
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)
        clearSearchButton = view.findViewById(R.id.clearSearchButton)
        btnClientsOwe = view.findViewById(R.id.btnClientsOwe)
        btnYouOwe = view.findViewById(R.id.btnYouOwe)
        exportCsvButton = view.findViewById(R.id.exportCsvButton)

        recyclerView = view.findViewById(R.id.pendingRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PendingPaymentsAdapter(emptyList()) { item ->
            showPaymentDialog(item)
        }
        recyclerView.adapter = adapter

        // Set up event listeners
        setupEventListeners()

        // Load initial data
        loadPendingPayments()
    }

    private fun setupEventListeners() {
        searchButton.setOnClickListener {
            currentSearchQuery = searchInput.text.toString().trim()
            clearSearchButton.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
            loadPendingPayments()
        }

        clearSearchButton.setOnClickListener {
            searchInput.text.clear()
            currentSearchQuery = ""
            clearSearchButton.visibility = View.GONE
            loadPendingPayments()
        }

        btnClientsOwe.setOnClickListener {
            currentSection = "clients_owe"
            updateSectionButtons()
            loadPendingPayments()
        }

        btnYouOwe.setOnClickListener {
            currentSection = "you_owe"
            updateSectionButtons()
            loadPendingPayments()
        }

        exportCsvButton.setOnClickListener {
            exportCsv()
        }
    }

    private fun updateSectionButtons() {
        if (currentSection == "clients_owe") {
            btnClientsOwe.setBackgroundColor(resources.getColor(R.color.primary, null))
            btnClientsOwe.setTextColor(resources.getColor(android.R.color.white, null))
            btnYouOwe.setBackgroundColor(resources.getColor(R.color.bg_card, null))
            btnYouOwe.setTextColor(resources.getColor(R.color.text_main, null))
        } else {
            btnYouOwe.setBackgroundColor(resources.getColor(R.color.primary, null))
            btnYouOwe.setTextColor(resources.getColor(android.R.color.white, null))
            btnClientsOwe.setBackgroundColor(resources.getColor(R.color.bg_card, null))
            btnClientsOwe.setTextColor(resources.getColor(R.color.text_main, null))
        }
    }

    private fun showPaymentDialog(item: PendingPaymentItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_record_payment, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Record Payment: ${item.client_name} - ${item.exchange_name}"

        // Set up payment direction spinner
        val paymentDirectionSpinner = dialogView.findViewById<Spinner>(R.id.paymentDirectionSpinner)
        val directions = arrayOf("To Client (You Pay)", "From Client (You Receive)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, directions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentDirectionSpinner.adapter = adapter

        // Set default direction based on item type
        val defaultDirection = if (item.type == "PAY") 0 else 1 // 0 = To Client, 1 = From Client
        paymentDirectionSpinner.setSelection(defaultDirection)

        // Amount input
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        amountInput.setText(Math.abs(item.my_share).toString())

        // Update exchange balance checkbox
        val updateBalanceCheckbox = dialogView.findViewById<CheckBox>(R.id.updateBalanceCheckbox)
        val newBalanceInput = dialogView.findViewById<EditText>(R.id.newBalanceInput)

        updateBalanceCheckbox.setOnCheckedChangeListener { _, isChecked ->
            newBalanceInput.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                // Pre-fill with current balance (this would need to be fetched from API)
                newBalanceInput.setText(item.exchange_balance.toString())
            }
        }

        // Notes input
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val direction = paymentDirectionSpinner.selectedItemPosition
            val amount = amountInput.text.toString()
            val updateBalance = updateBalanceCheckbox.isChecked
            val newBalance = if (updateBalance) newBalanceInput.text.toString() else ""
            val notes = notesInput.text.toString()

            if (amount.isEmpty()) {
                Toast.makeText(context, "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (updateBalance && newBalance.isEmpty()) {
                Toast.makeText(context, "Enter new exchange balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitPayment(item.account_id, amount, direction, updateBalance, newBalance, notes, dialog)
        }

        dialog.show()
    }

    private fun submitPayment(accountId: Int, amount: String, direction: Int, updateBalance: Boolean, newBalance: String, notes: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return

        val paymentDirection = if (direction == 0) "TO_CLIENT" else "FROM_CLIENT" // 0 = To Client (You Pay), 1 = From Client (You Receive)

        val request = RecordPaymentRequest(
            amount = amount.toLong(),
            payment_direction = paymentDirection,
            notes = notes,
            update_exchange_balance = if (updateBalance) true else null,
            new_exchange_balance = if (updateBalance && newBalance.isNotEmpty()) newBalance.toLong() else null
        )

        lifecycleScope.launch {
            try {
                val response = apiService.recordPayment(ApiClient.getAuthToken(token), accountId, request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Payment Recorded Successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadPendingPayments()
                } else {
                    Toast.makeText(context, "Error recording payment", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadPendingPayments() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getPendingPayments(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val data = response.body()!!
                    toReceiveText.text = "₹${formatNumber(data.total_to_receive)}"
                    toPayText.text = "₹${formatNumber(data.total_to_pay)}"

                    // Filter items based on current section and search query
                    var filteredItems = data.pending_payments

                    // Filter by section
                    filteredItems = filteredItems.filter { item ->
                        when (currentSection) {
                            "clients_owe" -> item.type == "RECEIVE" // Clients owe you = you receive
                            "you_owe" -> item.type == "PAY" // You owe clients = you pay
                            else -> true
                        }
                    }

                    // Filter by search query
                    if (currentSearchQuery.isNotEmpty()) {
                        filteredItems = filteredItems.filter { item ->
                            item.client_name.contains(currentSearchQuery, ignoreCase = true) ||
                            item.exchange_name.contains(currentSearchQuery, ignoreCase = true) ||
                            (item.client_code?.contains(currentSearchQuery, ignoreCase = true) == true)
                        }
                    }

                    adapter.updateItems(filteredItems)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading pending payments", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportCsv() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.exportPendingPayments(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val csvContent = response.body()?.string() ?: ""
                    // For now, just show a toast - in a real app you'd save to file and share
                    Toast.makeText(context, "CSV export would be available here", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Error exporting CSV", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatNumber(number: Long): String {
        return String.format("%,d", number)
    }
}

class PendingPaymentsAdapter(
    private var items: List<PendingPaymentItem>,
    private val onItemClick: (PendingPaymentItem) -> Unit
) : RecyclerView.Adapter<PendingPaymentsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clientName: TextView = itemView.findViewById(R.id.clientNameText)
        val exchangeName: TextView = itemView.findViewById(R.id.exchangeNameText)
        val pnlLabel: TextView = itemView.findViewById(R.id.pnlLabel)
        val amount: TextView = itemView.findViewById(R.id.amountText)
        val typeLabel: TextView = itemView.findViewById(R.id.typeLabel)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_payment, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.clientName.text = item.client_name
        holder.exchangeName.text = item.exchange_name
        holder.pnlLabel.text = "Client PnL: ₹${String.format("%,d", item.pnl)}"
        holder.amount.text = "₹${String.format("%,d", Math.abs(item.my_share))}"
        
        holder.itemView.setOnClickListener { onItemClick(item) }
        
        if (item.type == "RECEIVE") {
            holder.amount.setTextColor(holder.itemView.resources.getColor(R.color.success, null))
            holder.typeLabel.text = "RECEIVE"
            holder.typeLabel.setTextColor(holder.itemView.resources.getColor(R.color.success, null))
            holder.typeLabel.setBackgroundResource(R.drawable.pill_label_bg)
            holder.typeLabel.background?.setTint(holder.itemView.resources.getColor(R.color.success_soft, null))
        } else {
            holder.amount.setTextColor(holder.itemView.resources.getColor(R.color.danger, null))
            holder.typeLabel.text = "PAY"
            holder.typeLabel.setTextColor(holder.itemView.resources.getColor(R.color.danger, null))
            holder.typeLabel.setBackgroundResource(R.drawable.pill_label_bg)
            holder.typeLabel.background?.setTint(holder.itemView.resources.getColor(R.color.danger_soft, null))
        }
    }
    
    override fun getItemCount() = items.size
    
    fun updateItems(newItems: List<PendingPaymentItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
