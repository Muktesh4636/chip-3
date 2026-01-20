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

import android.os.Environment
import android.widget.EditText
import android.widget.Toast
import java.io.File
import java.io.IOException
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.app.PendingIntent
import android.content.ContentValues
import android.provider.MediaStore
import androidx.core.content.FileProvider
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
    private lateinit var exportCsvButton: Button

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
        
        // Create notification channel for downloads
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "File Downloads"
            val descriptionText = "Notifications for downloaded reports"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("DOWNLOAD_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showDownloadNotification(fileName: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(requireContext(), "DOWNLOAD_CHANNEL")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText("File saved to Downloads: $fileName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(requireContext())) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationError", "Notification permission missing", e)
        }
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

        exportCsvButton.setOnClickListener {
            exportCsv()
        }
    }


    private fun showPaymentDialog(item: PendingPaymentItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_record_payment, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Record Payment: ${item.client_name}"
        
        // Set up account status info
        val sharePct = String.format("%.1f", item.share_percentage)
        val statusText = "Funding: ₹${formatNumber(item.funding ?: 0)}\n" +
                         "Exchange Balance: ₹${formatNumber(item.exchange_balance ?: 0)}\n" +
                         "Client PnL: ₹${formatNumber(item.pnl)}\n" +
                         "My Share: ₹${formatNumber(item.my_share)}\n" +
                         "Share %: $sharePct%"
        dialogView.findViewById<TextView>(R.id.accountStatusText).text = statusText

        // Amount input - pre-fill with remaining amount
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        amountInput.setText(item.remaining_amount.toString())

        // Set remaining amount hint
        val remainingHint = dialogView.findViewById<TextView>(R.id.remainingAmountHint)
        remainingHint.text = "Maximum: ₹${formatNumber(item.remaining_amount)} (Remaining settlement amount)"

        // Re-add capital checkbox (only for loss cases - when client owes you)
        val reAddCapitalCheckbox = dialogView.findViewById<CheckBox>(R.id.reAddCapitalCheckbox)
        val reAddCapitalHint = dialogView.findViewById<TextView>(R.id.reAddCapitalHint)

        if (item.type == "RECEIVE") { // Client owes you (loss case)
            reAddCapitalCheckbox.visibility = View.VISIBLE
            reAddCapitalHint.visibility = View.VISIBLE
            reAddCapitalCheckbox.isChecked = true // Default to true as in website
        } else {
            reAddCapitalCheckbox.visibility = View.GONE
            reAddCapitalHint.visibility = View.GONE
            reAddCapitalCheckbox.isChecked = false
        }

        // Notes input
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val amount = amountInput.text.toString()
            val reAddCapital = reAddCapitalCheckbox.isChecked
            val notes = notesInput.text.toString()

            if (amount.isEmpty()) {
                Toast.makeText(context, "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val paidAmount = amount.toLongOrNull() ?: 0
            if (paidAmount <= 0) {
                Toast.makeText(context, "Amount must be greater than zero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (paidAmount > item.remaining_amount) {
                Toast.makeText(context, "Amount cannot exceed remaining settlement (₹${formatNumber(item.remaining_amount)})", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Direction is automatic based on item.type (RECEIVE -> FROM_CLIENT, PAY -> TO_CLIENT)
            val direction = if (item.type == "RECEIVE") 1 else 0

            submitPayment(item.account_id, amount, direction, false, "", reAddCapital, notes, dialog)
        }

        dialog.show()
    }

    private fun submitPayment(accountId: Int, amount: String, direction: Int, updateBalance: Boolean, newBalance: String, reAddCapital: Boolean, notes: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return

        val paymentDirection = if (direction == 0) "TO_CLIENT" else "FROM_CLIENT" // 0 = To Client (You Pay), 1 = From Client (You Receive)

        val request = RecordPaymentRequest(
            amount = amount.toLong(),
            payment_direction = paymentDirection,
            notes = notes,
            update_exchange_balance = if (updateBalance) true else null,
            new_exchange_balance = if (updateBalance && newBalance.isNotEmpty()) newBalance.toLong() else null,
            re_add_capital = if (reAddCapital) true else null
        )

        android.util.Log.d("PaymentDebug", "Sending payment request: accountId=$accountId, amount=$amount, direction=$paymentDirection, updateBalance=$updateBalance, newBalance=$newBalance, reAddCapital=$reAddCapital")

        lifecycleScope.launch {
            try {
                val response = apiService.recordPayment(ApiClient.getAuthToken(token), accountId, request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Payment Recorded Successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    // Add a small delay to ensure backend processing is complete
                    view?.postDelayed({
                        loadPendingPayments()
                    }, 500)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorJson = errorBody?.let { org.json.JSONObject(it) }
                        errorJson?.getString("error") ?: "Unknown error"
                    } catch (e: Exception) {
                        errorBody ?: "Unknown error"
                    }
                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    android.util.Log.e("PaymentError", "Error recording payment: $errorBody")
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
                android.util.Log.d("PendingPayments", "Loading pending payments...")
                val response = apiService.getPendingPayments(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val data = response.body()!!
                    android.util.Log.d("PendingPayments", "Loaded ${data.pending_payments.size} pending payments")
                    toReceiveText.text = "₹${formatNumber(data.total_to_receive)}"
                    toPayText.text = "₹${formatNumber(data.total_to_pay)}"

                    // Filter items based on search query
                    var filteredItems = data.pending_payments

                    // Filter by search query
                    if (currentSearchQuery.isNotEmpty()) {
                        filteredItems = filteredItems.filter { item ->
                            item.client_name.contains(currentSearchQuery, ignoreCase = true) ||
                            item.exchange_name.contains(currentSearchQuery, ignoreCase = true) ||
                            (item.client_code?.contains(currentSearchQuery, ignoreCase = true) == true)
                        }
                        android.util.Log.d("PendingPayments", "After search filter: ${filteredItems.size} items")
                    }

                    adapter.updateItems(filteredItems)
                    android.util.Log.d("PendingPayments", "Updated adapter with ${filteredItems.size} items")
                } else {
                    android.util.Log.e("PendingPayments", "Failed to load pending payments: ${response.code()}")
                    Toast.makeText(context, "Failed to load pending payments", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("PendingPayments", "Error loading pending payments", e)
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
                    val uri = saveCsvToPublicDownloads(csvContent)
                    if (uri != null) {
                        val fileName = "Pending_Payments_${System.currentTimeMillis()}.csv"
                        Toast.makeText(context, "CSV saved to Downloads folder", Toast.LENGTH_LONG).show()
                        showDownloadNotification(fileName, uri)
                    } else {
                        Toast.makeText(context, "CSV export succeeded, but saving failed", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Error exporting CSV", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveCsvToPublicDownloads(content: String): Uri? {
        val fileName = "Pending_Payments_${System.currentTimeMillis()}.csv"
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    uri
                } else null
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(content)
                // Use FileProvider for older versions to get a shareable content URI
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
        
        // Show remaining amount to settle
        holder.amount.text = "₹${String.format("%,d", item.remaining_amount)}"
        
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
