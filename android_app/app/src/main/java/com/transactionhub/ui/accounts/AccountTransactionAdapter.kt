package com.transactionhub.ui.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class AccountTransactionAdapter(
    private var transactions: List<Transaction> = emptyList(),
    private val onDeleteClick: ((Transaction) -> Unit)? = null
) : RecyclerView.Adapter<AccountTransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction, position == 0, onDeleteClick) // Only show delete button for first (latest) transaction
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.transactionDate)
        private val typeText: TextView = itemView.findViewById(R.id.transactionType)
        private val amountText: TextView = itemView.findViewById(R.id.transactionAmount)
        private val balanceAfterText: TextView = itemView.findViewById(R.id.transactionBalanceAfter)
        private val notesText: TextView = itemView.findViewById(R.id.transactionNotes)
        private val deleteButton: Button = itemView.findViewById(R.id.btnDeleteTransaction)

        fun bind(transaction: Transaction, isLatest: Boolean, onDeleteClick: ((Transaction) -> Unit)?) {
            try {
                // Format date safely - transaction.date is a String from API
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateStr = try {
                    // Try to parse the date string
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(transaction.date)
                    dateFormat.format(date ?: Date())
                } catch (e: Exception) {
                    // Fallback: show the raw date string or current date
                    transaction.date.takeIf { it.isNotEmpty() } ?: "No Date"
                }
                dateText.text = dateStr

                // Transaction type with color coding and better labels
                val (typeTextStr, bgColor) = when (transaction.type) {
                    "FUNDING" -> Pair("💰 Funding", android.R.color.holo_green_dark)
                    "BALANCE" -> Pair("⚖️ Balance", android.R.color.holo_blue_dark)
                    "PAYMENT" -> Pair("💳 Payment", android.R.color.holo_orange_dark)
                    "RECORD_PAYMENT" -> Pair("📄 Payment", android.R.color.holo_orange_dark)
                    "FEE" -> Pair("💸 Fee", android.R.color.holo_red_dark)
                    "TRADE" -> Pair("📈 Trade", android.R.color.holo_blue_bright)
                    else -> Pair(transaction.type_display ?: transaction.type, android.R.color.darker_gray)
                }

                typeText.text = typeTextStr
                typeText.setBackgroundResource(bgColor)

                // Amount with better formatting
                val amountFormatted = try {
                    String.format("%,d", transaction.amount)
                } catch (e: Exception) {
                    transaction.amount.toString()
                }
                amountText.text = "₹$amountFormatted"

                // Balance after with better formatting
                val balanceAfter = transaction.exchange_balance_after ?: 0L
                val balanceFormatted = try {
                    String.format("%,d", balanceAfter)
                } catch (e: Exception) {
                    balanceAfter.toString()
                }
                balanceAfterText.text = "₹$balanceFormatted"

                // Notes
                notesText.text = transaction.notes ?: "-"

                // Delete button only for latest transaction
                deleteButton.visibility = if (isLatest) View.VISIBLE else View.GONE
                deleteButton.setOnClickListener {
                    onDeleteClick?.invoke(transaction)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Set fallback values
                dateText.text = "Error"
                typeText.text = "Error"
                amountText.text = "₹0"
                balanceAfterText.text = "₹0"
                notesText.text = "Error loading transaction"
                deleteButton.visibility = View.GONE
            }
        }
    }
}