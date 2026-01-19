package com.transactionhub.ui.accounts

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
import com.transactionhub.data.models.RecordPaymentRequest
import com.transactionhub.data.models.Transaction
import com.transactionhub.ui.transactions.TransactionsFragment
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ExchangeAccountDetailFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private var accountId: Int = -1
    private var clientName: String = ""
    private var exchangeName: String = ""

    companion object {
        fun newInstance(accountId: Int, clientName: String, exchangeName: String): ExchangeAccountDetailFragment {
            val fragment = ExchangeAccountDetailFragment()
            val args = Bundle()
            args.putInt("accountId", accountId)
            args.putString("clientName", clientName)
            args.putString("exchangeName", exchangeName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = arguments?.getInt("accountId") ?: -1
        clientName = arguments?.getString("clientName") ?: ""
        exchangeName = arguments?.getString("exchangeName") ?: ""

        // Debug logging
        android.util.Log.d("ExchangeAccountDetail", "Fragment created - accountId: $accountId, client: $clientName, exchange: $exchangeName")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_exchange_account_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            android.util.Log.d("ExchangeAccountDetail", "onViewCreated called")

            // Validate required data
            if (accountId == -1) {
                context?.let { Toast.makeText(it, "Invalid account ID", Toast.LENGTH_LONG).show() }
                parentFragmentManager.popBackStack()
                return
            }

            if (clientName.isEmpty() || exchangeName.isEmpty()) {
                context?.let { Toast.makeText(it, "Missing client or exchange information", Toast.LENGTH_LONG).show() }
                parentFragmentManager.popBackStack()
                return
            }

            prefManager = PrefManager(requireContext())
            apiService = ApiClient.apiService

            val titleView = view.findViewById<TextView>(R.id.accountTitle)
            titleView?.text = "$clientName - $exchangeName"

            loadAccountDetails(view)
            loadTransactions(view)
            setupButtons(view)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ExchangeAccountDetail", "Error in onViewCreated", e)
            context?.let {
                Toast.makeText(it, "Error initializing view: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToEditPercentage() {
        val editFragment = EditPercentageFragment.newInstance(accountId, clientName, exchangeName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, editFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToTransactions() {
        val fragment = TransactionsFragment.newInstance(accountId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadAccountDetails(view: View) {
        val token = prefManager.getToken()
        if (token == null) {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val accounts = response.body() ?: emptyList()
                    val account = accounts.find { it.id == accountId }

                    if (account != null) {
                        // Update summary cards with null safety
                        view.findViewById<TextView>(R.id.fundingValue)?.text = "₹${String.format("%,d", account.funding)}"
                        view.findViewById<TextView>(R.id.balanceValue)?.text = "₹${String.format("%,d", account.exchange_balance)}"

                        val pnl = account.pnl
                        val pnlView = view.findViewById<TextView>(R.id.pnlValue)
                        pnlView?.text = "₹${String.format("%,d", pnl)}"
                        pnlView?.setTextColor(resources.getColor(
                            if (pnl > 0L) R.color.success else if (pnl < 0L) R.color.danger else R.color.text_main,
                            null
                        ))

                        // Update Account Information section
                        view.findViewById<TextView>(R.id.clientInfoValue)?.text = "${account.client_name} (${account.client})"
                        view.findViewById<TextView>(R.id.exchangeInfoValue)?.text = "${account.exchange_name} (${account.exchange})"
                        view.findViewById<TextView>(R.id.myTotalPercentageValue)?.text = "${account.loss_share_percentage}%"
                        view.findViewById<TextView>(R.id.lossSharePercentageValue)?.text = "${account.loss_share_percentage}%"
                        view.findViewById<TextView>(R.id.profitSharePercentageValue)?.text = "${account.profit_share_percentage}%"
                        // TODO: Add created and updated dates when available in API

                        val share = account.my_share
                        // Update Final Share value (was My Share)
                        val finalShareView = view.findViewById<TextView>(R.id.finalShareValue)
                        finalShareView?.text = if (share == 0L) "₹0" else "₹${String.format("%,d", share)}"

                        // Update Share Percentage
                        val sharePercentageView = view.findViewById<TextView>(R.id.sharePercentageValue)
                        if (pnl < 0L) {
                            sharePercentageView?.text = "${account.loss_share_percentage}% (Loss)"
                        } else if (pnl > 0L) {
                            sharePercentageView?.text = "${account.profit_share_percentage}% (Profit)"
                        } else {
                            sharePercentageView?.text = "${account.loss_share_percentage}%"
                        }

                        // TODO: Calculate and display remaining settlement
                        val remainingSettlementView = view.findViewById<TextView>(R.id.remainingSettlementValue)
                        remainingSettlementView?.text = "₹${String.format("%,d", share)}" // Placeholder - needs actual calculation

                        // Load report config to get detailed share breakdown
                        loadReportConfig(view, accountId, share)

                        // Account information with null safety
                        // Account information - simplified for now
                        // TODO: Add detailed account info section back
                    } else {
                        Toast.makeText(context, "Account not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to load account data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading account details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTransactions(view: View) {
        val token = prefManager.getToken()
        if (token == null) {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val allTransactions = response.body() ?: emptyList()
                    android.util.Log.d("ExchangeAccountDetail", "Loaded ${allTransactions.size} total transactions")
                    val accountTransactions = allTransactions.filter { it.client_exchange == accountId }.take(20)
                    android.util.Log.d("ExchangeAccountDetail", "Filtered ${accountTransactions.size} transactions for account $accountId")

                    if (accountTransactions.isNotEmpty()) {
                        val recyclerView = view.findViewById<RecyclerView>(R.id.transactionsRecyclerView)
                        if (recyclerView?.adapter == null) {
                            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
                            recyclerView?.adapter = AccountTransactionAdapter(accountTransactions) { transaction ->
                                deleteTransaction(transaction)
                            }
                        } else {
                            (recyclerView.adapter as? AccountTransactionAdapter)?.updateTransactions(accountTransactions)
                        }
                        view.findViewById<TextView>(R.id.emptyTransactionsText)?.visibility = View.GONE
                        android.util.Log.d("ExchangeAccountDetail", "Transactions displayed successfully")
                    } else {
                        view.findViewById<TextView>(R.id.emptyTransactionsText)?.visibility = View.VISIBLE
                        android.util.Log.d("ExchangeAccountDetail", "No transactions found for account $accountId")
                    }
                } else {
                    Toast.makeText(context, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtons(view: View) {
        try {
            view.findViewById<Button>(R.id.btnAddFunding)?.setOnClickListener {
                showTransactionDialog("Add Funding", "FUNDING")
            }

            view.findViewById<Button>(R.id.btnUpdateBalance)?.setOnClickListener {
                showTransactionDialog("Update Balance", "BALANCE")
            }

            view.findViewById<Button>(R.id.btnEditPercentage)?.setOnClickListener {
                navigateToEditPercentage()
            }

            view.findViewById<Button>(R.id.btnEditPercentageSmall)?.setOnClickListener {
                navigateToEditPercentage()
            }

            view.findViewById<Button>(R.id.btnViewAllTransactions)?.setOnClickListener {
                navigateToTransactions()
            }

            view.findViewById<Button>(R.id.btnBackToClient)?.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error setting up buttons: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTransactionDialog(title: String, actionType: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_form, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.dialogTitle).text = title

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val amount = dialogView.findViewById<EditText>(R.id.inputAmount).text.toString()
            val notes = dialogView.findViewById<EditText>(R.id.inputNotes).text.toString()

            if (amount.isEmpty()) {
                Toast.makeText(context, "Please enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitTransaction(amount, notes, actionType, dialog)
        }

        dialog.show()
    }

    private fun submitTransaction(amount: String, notes: String, actionType: String, dialog: androidx.appcompat.app.AlertDialog) {
        val token = prefManager.getToken()
        if (token == null) {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        val data = mapOf("amount" to amount, "notes" to notes)

        lifecycleScope.launch {
            try {
                val response = when (actionType) {
                    "FUNDING" -> apiService.addFunding(ApiClient.getAuthToken(token), accountId, data)
                    "BALANCE" -> apiService.updateBalance(ApiClient.getAuthToken(token), accountId, data)
                    "PAYMENT" -> {
                        val request = RecordPaymentRequest(
                            amount = amount.toLong(),
                            payment_direction = "FROM_CLIENT", // Default for general payments
                            notes = notes
                        )
                        apiService.recordPayment(ApiClient.getAuthToken(token), accountId, request)
                    }
                    else -> null
                }

                if (response != null && response.isSuccessful) {
                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                        // Refresh data safely
                    view?.let { currentView ->
                        try {
                            loadAccountDetails(currentView)
                            loadTransactions(currentView)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error refreshing data", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Error saving data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This will revert account balances to the state before this transaction.")
            .setPositiveButton("Delete") { _, _ ->
                performTransactionDelete(transaction)
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun performTransactionDelete(transaction: Transaction) {
        val token = prefManager.getToken()
        if (token == null) {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.deleteTransaction(ApiClient.getAuthToken(token), transaction.id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                    // Refresh the account details and transactions
                    view?.let { loadAccountDetails(it) }
                    view?.let { loadTransactions(it) }
                } else {
                    Toast.makeText(context, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error deleting transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReportConfig(view: View, accountId: Int, totalShare: Long) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportConfig(ApiClient.getAuthToken(token), accountId)
                if (response.isSuccessful) {
                    val config = response.body()
                    if (config != null && config.isNotEmpty()) {
                        // Show report configuration section
                        view.findViewById<androidx.cardview.widget.CardView>(R.id.reportConfigCard)?.visibility = View.VISIBLE

                        val friendPct = (config["friend_percentage"] as? Double ?: 0.0)
                        val myOwnPct = (config["my_own_percentage"] as? Double ?: 0.0)

                        // Update Report Configuration section
                        view.findViewById<TextView>(R.id.companyPercentageValue)?.text = "${friendPct}%"
                        view.findViewById<TextView>(R.id.myOwnPercentageValue)?.text = "${myOwnPct}%"

                        // Calculate actual amounts from total share for detailed breakdown
                        val myOwnAmount = (totalShare * (myOwnPct / 100.0)).toLong()
                        val friendAmount = (totalShare * (friendPct / 100.0)).toLong()

                        // Update Share Breakdown section with detailed breakdown
                        view.findViewById<TextView>(R.id.mySharePercentage)?.text = "₹${String.format("%,d", myOwnAmount)} (${String.format("%.1f", myOwnPct)}%)"
                        view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "₹${String.format("%,d", friendAmount)} (${String.format("%.1f", friendPct)}%)"
                    } else {
                        // Hide report configuration section if no config exists
                        view.findViewById<androidx.cardview.widget.CardView>(R.id.reportConfigCard)?.visibility = View.GONE

                        // Fallback: show basic percentage info in share breakdown
                        view.findViewById<TextView>(R.id.mySharePercentage)?.text = "₹${String.format("%,d", totalShare)} (100%)"
                        view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "₹0 (0%)"
                    }
                } else {
                    // Hide report configuration section
                    view.findViewById<androidx.cardview.widget.CardView>(R.id.reportConfigCard)?.visibility = View.GONE

                    // Fallback: show basic percentage info
                    view.findViewById<TextView>(R.id.mySharePercentage)?.text = "₹${String.format("%,d", totalShare)} (100%)"
                    view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "₹0 (0%)"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Hide report configuration section
                view.findViewById<androidx.cardview.widget.CardView>(R.id.reportConfigCard)?.visibility = View.GONE

                // Fallback: show basic percentage info
                view.findViewById<TextView>(R.id.mySharePercentage)?.text = "₹${String.format("%,d", totalShare)} (100%)"
                view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "₹0 (0%)"
            }
        }
    }

    private fun saveReportConfig(newMyOwn: Int, newFriend: Int) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val data = mapOf(
                    "my_own_percentage" to newMyOwn.toString(),
                    "friend_percentage" to newFriend.toString()
                )

                val response = apiService.updateReportConfig(ApiClient.getAuthToken(token), accountId, data)

                if (response.isSuccessful) {
                    android.widget.Toast.makeText(context, "Report configuration updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                    // Refresh the account details and report config
                    view?.let { currentView ->
                        loadAccountDetails(currentView)
                    }
                } else {
                    android.widget.Toast.makeText(context, "Failed to update configuration", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error updating configuration: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}