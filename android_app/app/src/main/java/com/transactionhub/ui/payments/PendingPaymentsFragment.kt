package com.transactionhub.ui.payments

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
import com.transactionhub.data.models.PendingPaymentItem
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
        
        toReceiveText = view.findViewById(R.id.toReceiveText)
        toPayText = view.findViewById(R.id.toPayText)
        
        recyclerView = view.findViewById(R.id.pendingRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PendingPaymentsAdapter(emptyList()) { item ->
            showPaymentDialog(item)
        }
        recyclerView.adapter = adapter
        
        loadPendingPayments()
    }

    private fun showPaymentDialog(item: PendingPaymentItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_form, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Record Payment: ${item.client_name}"
        dialogView.findViewById<EditText>(R.id.inputAmount).setText(Math.abs(item.my_share).toString())
        
        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.btnSubmit).setOnClickListener {
            val amount = dialogView.findViewById<EditText>(R.id.inputAmount).text.toString()
            val notes = dialogView.findViewById<EditText>(R.id.inputNotes).text.toString()
            
            if (amount.isEmpty()) {
                Toast.makeText(context, "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            submitPayment(item.account_id, amount, notes, dialog)
        }
        
        dialog.show()
    }

    private fun submitPayment(accountId: Int, amount: String, notes: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return
        val data = mapOf("amount" to amount, "notes" to notes)
        
        lifecycleScope.launch {
            try {
                val response = apiService.recordPayment(ApiClient.getAuthToken(token), accountId, data)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Payment Recorded!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadPendingPayments()
                } else {
                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
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
                    adapter.updateItems(data.pending_payments)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
