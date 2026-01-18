package com.transactionhub.ui.transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class TransactionsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private var allTransactions: List<Transaction> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        recyclerView = view.findViewById(R.id.transactionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionsAdapter(
            emptyList(),
            onItemClick = { txn -> showTxnDetail(txn) },
            onItemLongClick = { txn -> showTxnActionMenu(txn) }
        )
        recyclerView.adapter = adapter
        
        loadTransactions()

        view.findViewById<EditText>(R.id.searchTransactions).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTransactions(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<Button>(R.id.btnAdvancedFilter).setOnClickListener {
            showAdvancedFilterDialog()
        }

        view.findViewById<Button>(R.id.btnBulkActions).setOnClickListener {
            showBulkActionsDialog()
        }
    }

    private fun filterTransactions(query: String) {
        val filtered = allTransactions.filter { 
            it.client_name.contains(query, ignoreCase = true) || 
            it.exchange_name.contains(query, ignoreCase = true) ||
            it.type_display.contains(query, ignoreCase = true)
        }
        adapter.updateTransactions(filtered)
    }

    private fun showTxnDetail(txn: Transaction) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_detail, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.detType).text = txn.type_display
        dialogView.findViewById<TextView>(R.id.detSeq).text = "Transaction #${txn.sequence_no}"
        dialogView.findViewById<TextView>(R.id.detDate).text = txn.date
        dialogView.findViewById<TextView>(R.id.detAmount).text = "₹${String.format("%,d", txn.amount)}"
        dialogView.findViewById<TextView>(R.id.detAccount).text = "${txn.client_name} | ${txn.exchange_name}"

        // Funding changes
        if (txn.funding_before != null && txn.funding_after != null) {
            dialogView.findViewById<TextView>(R.id.detFundingBefore).text = "₹${String.format("%,d", txn.funding_before!!)}"
            dialogView.findViewById<TextView>(R.id.detFundingAfter).text = "₹${String.format("%,d", txn.funding_after!!)}"
        } else {
            dialogView.findViewById<TextView>(R.id.detFundingBefore).text = "N/A"
            dialogView.findViewById<TextView>(R.id.detFundingAfter).text = "N/A"
        }

        // Balance changes
        if (txn.exchange_balance_before != null && txn.exchange_balance_after != null) {
            dialogView.findViewById<TextView>(R.id.detBalanceBefore).text = "₹${String.format("%,d", txn.exchange_balance_before!!)}"
            dialogView.findViewById<TextView>(R.id.detBalanceAfter).text = "₹${String.format("%,d", txn.exchange_balance_after!!)}"
        } else {
            dialogView.findViewById<TextView>(R.id.detBalanceBefore).text = "N/A"
            dialogView.findViewById<TextView>(R.id.detBalanceAfter).text = "N/A"
        }

        dialogView.findViewById<TextView>(R.id.detNotes).text = if (txn.notes.isNullOrEmpty()) "No notes available." else txn.notes

        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showTxnActionMenu(txn: Transaction) {
        val actions = arrayOf("📝 Edit Transaction", "🗑️ Delete Transaction", "📋 Copy Details")

        AlertDialog.Builder(requireContext())
            .setTitle("Transaction Actions")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showEditTxnDialog(txn)
                    1 -> showDeleteConfirm(txn)
                    2 -> copyTxnDetails(txn)
                }
            }
            .show()
    }

    private fun showDeleteConfirm(txn: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction?")
            .setMessage("Are you sure you want to delete this ${txn.type_display} of ₹${String.format("%,d", txn.amount)}?")
            .setPositiveButton("Delete") { _, _ -> deleteTxn(txn.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyTxnDetails(txn: Transaction) {
        val details = """
            Transaction #${txn.sequence_no}
            Type: ${txn.type_display}
            Client: ${txn.client_name}
            Exchange: ${txn.exchange_name}
            Date: ${txn.date}
            Amount: ₹${String.format("%,d", txn.amount)}
            Notes: ${txn.notes ?: "None"}
        """.trimIndent()

        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Transaction Details", details)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Transaction details copied!", Toast.LENGTH_SHORT).show()
    }

    private fun showAdvancedFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_advanced_filter, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Advanced Filters")
            .create()

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerFilterType)
        val spinnerClient = dialogView.findViewById<Spinner>(R.id.spinnerFilterClient)
        val spinnerExchange = dialogView.findViewById<Spinner>(R.id.spinnerFilterExchange)
        val editMinAmount = dialogView.findViewById<EditText>(R.id.editMinAmount)
        val editMaxAmount = dialogView.findViewById<EditText>(R.id.editMaxAmount)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApplyFilters)
        val btnClear = dialogView.findViewById<Button>(R.id.btnClearFilters)

        // Setup type spinner
        val types = arrayOf("All Types", "FUNDING", "TRADE", "RECORD_PAYMENT", "SETTLEMENT_SHARE")
        spinnerType.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

        // Setup client spinner
        lifecycleScope.launch {
            try {
                val token = prefManager.getToken() ?: return@launch
                val clientsResponse = apiService.getClients(ApiClient.getAuthToken(token))
                if (clientsResponse.isSuccessful) {
                    val clients = listOf("All Clients") + clientsResponse.body()!!.map { it.name }
                    spinnerClient.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, clients)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Setup exchange spinner
        lifecycleScope.launch {
            try {
                val token = prefManager.getToken() ?: return@launch
                val exchangesResponse = apiService.getExchanges(ApiClient.getAuthToken(token))
                if (exchangesResponse.isSuccessful) {
                    val exchanges = listOf("All Exchanges") + exchangesResponse.body()!!.map { it.name }
                    spinnerExchange.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, exchanges)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnApply.setOnClickListener {
            applyAdvancedFilters(
                spinnerType.selectedItem.toString(),
                spinnerClient.selectedItem?.toString() ?: "All Clients",
                spinnerExchange.selectedItem?.toString() ?: "All Exchanges",
                editMinAmount.text.toString(),
                editMaxAmount.text.toString()
            )
            dialog.dismiss()
        }

        btnClear.setOnClickListener {
            clearAdvancedFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyAdvancedFilters(type: String, client: String, exchange: String, minAmount: String, maxAmount: String) {
        var filtered = allTransactions

        // Filter by type
        if (type != "All Types") {
            filtered = filtered.filter { it.type == type }
        }

        // Filter by client
        if (client != "All Clients") {
            filtered = filtered.filter { it.client_name == client }
        }

        // Filter by exchange
        if (exchange != "All Exchanges") {
            filtered = filtered.filter { it.exchange_name == exchange }
        }

        // Filter by amount range
        if (minAmount.isNotEmpty()) {
            val min = minAmount.replace(",", "").toLongOrNull()
            if (min != null) {
                filtered = filtered.filter { it.amount >= min }
            }
        }

        if (maxAmount.isNotEmpty()) {
            val max = maxAmount.replace(",", "").toLongOrNull()
            if (max != null) {
                filtered = filtered.filter { it.amount <= max }
            }
        }

        adapter.updateTransactions(filtered)
        Toast.makeText(context, "Filtered to ${filtered.size} transactions", Toast.LENGTH_SHORT).show()
    }

    private fun clearAdvancedFilters() {
        adapter.updateTransactions(allTransactions)
        Toast.makeText(context, "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showBulkActionsDialog() {
        val actions = arrayOf("Enable Selection Mode", "Select All", "Select None", "Export Selected", "Delete Selected")

        AlertDialog.Builder(requireContext())
            .setTitle("Bulk Actions")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        adapter.setSelectionMode(true)
                        Toast.makeText(context, "Selection mode enabled. Tap transactions to select.", Toast.LENGTH_SHORT).show()
                    }
                    1 -> adapter.selectAll()
                    2 -> adapter.clearSelection()
                    3 -> exportSelectedTransactions()
                    4 -> deleteSelectedTransactions()
                }
            }
            .show()
    }

    private fun exportSelectedTransactions() {
        val selectedIds = adapter.getSelectedTransactionIds()
        if (selectedIds.isEmpty()) {
            Toast.makeText(context, "No transactions selected", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Exporting ${selectedIds.size} transactions...", Toast.LENGTH_SHORT).show()
        // TODO: Implement CSV export
    }

    private fun deleteSelectedTransactions() {
        val selectedIds = adapter.getSelectedTransactionIds()
        if (selectedIds.isEmpty()) {
            Toast.makeText(context, "No transactions selected", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${selectedIds.size} Transactions?")
            .setMessage("This action cannot be undone. Are you sure?")
            .setPositiveButton("Delete All") { _, _ ->
                bulkDeleteTransactions(selectedIds)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bulkDeleteTransactions(ids: List<Int>) {
        lifecycleScope.launch {
            val token = prefManager.getToken() ?: return@launch
            var successCount = 0
            var errorCount = 0

            for (id in ids) {
                try {
                    val response = apiService.deleteTransaction(ApiClient.getAuthToken(token), id)
                    if (response.isSuccessful) {
                        successCount++
                    } else {
                        errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }

            Toast.makeText(context, "Deleted $successCount, failed $errorCount", Toast.LENGTH_SHORT).show()
            if (successCount > 0) {
                loadTransactions()
            }
        }
    }

    private fun showEditTxnDialog(txn: Transaction) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_form, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        
        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Edit Transaction"
        val inputAmount = dialogView.findViewById<EditText>(R.id.inputAmount)
        val inputNotes = dialogView.findViewById<EditText>(R.id.inputNotes)
        
        inputAmount.setText(txn.amount.toString())
        inputNotes.setText(txn.notes ?: "")
        
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val newAmount = inputAmount.text.toString()
            val newNotes = inputNotes.text.toString()
            
            if (newAmount.isEmpty()) {
                Toast.makeText(context, "Amount required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performEditTxn(txn.id, newAmount, newNotes, dialog)
        }
        dialog.show()
    }

    private fun performEditTxn(id: Int, amount: String, notes: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return
        val data = mapOf("amount" to amount, "notes" to notes)
        
        lifecycleScope.launch {
            try {
                val response = apiService.editTransaction(ApiClient.getAuthToken(token), id, data)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Updated!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadTransactions()
                } else {
                    Toast.makeText(context, "Error updating", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun deleteTxn(id: Int) {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = apiService.deleteTransaction(ApiClient.getAuthToken(token), id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    loadTransactions()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun loadTransactions() {
        val token = prefManager.getToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    allTransactions = response.body() ?: emptyList()
                    adapter.updateTransactions(allTransactions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit = {},
    private val onItemLongClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    private var selectionMode = false

    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(transactions.map { it.id })
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedTransactionIds(): List<Int> = selectedItems.toList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeText: TextView = itemView.findViewById(R.id.transactionTypeText)
        val detailsText: TextView = itemView.findViewById(R.id.transactionDetailsText)
        val dateText: TextView = itemView.findViewById(R.id.transactionDateText)
        val amountText: TextView = itemView.findViewById(R.id.transactionAmountText)
        val seqText: TextView = itemView.findViewById(R.id.txnSeqNo)
        val checkbox: CheckBox? = itemView.findViewById(R.id.transactionCheckbox)
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

        // Handle selection mode
        if (selectionMode && holder.checkbox != null) {
            holder.checkbox.visibility = View.VISIBLE
            holder.checkbox.isChecked = selectedItems.contains(txn.id)
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(txn.id)
                } else {
                    selectedItems.remove(txn.id)
                }
            }
        } else if (holder.checkbox != null) {
            holder.checkbox.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (selectionMode && holder.checkbox != null) {
                holder.checkbox.isChecked = !holder.checkbox.isChecked
            } else {
                onItemClick(txn)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                onItemLongClick(txn)
            }
            true
        }

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
    
    fun updateTransactions(newTxns: List<Transaction>) {
        transactions = newTxns
        notifyDataSetChanged()
    }
}
