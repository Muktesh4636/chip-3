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
import com.transactionhub.data.models.Transaction
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

                        val share = account.my_share
                        // Update My Share value
                        val myShareView = view.findViewById<TextView>(R.id.myShareValue)
                        myShareView?.text = if (share == 0L) "₹0" else "₹${String.format("%,d", share)}"

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
                            recyclerView?.adapter = AccountTransactionAdapter(accountTransactions)
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
                showEditPercentageDialog()
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
                    "PAYMENT" -> apiService.recordPayment(ApiClient.getAuthToken(token), accountId, data)
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

    private fun showEditPercentageDialog() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportConfig(ApiClient.getAuthToken(token), accountId)
                if (response.isSuccessful) {
                    val config = response.body() ?: return@launch
                    val friendPct = config["friend_percentage"] as? Double ?: 0.0
                    val myOwnPct = config["my_own_percentage"] as? Double ?: 0.0
                    val totalShare = config["my_total_percentage"] as? Double ?: 0.0

                    val dialogView = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_config, null)
                    val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setNegativeButton("Cancel", null)
                        .create()

                    dialogView.findViewById<android.widget.TextView>(R.id.totalShareInfo).text = "Total Profit Share: ${totalShare.toInt()}%"
                    val editMyOwn = dialogView.findViewById<android.widget.EditText>(R.id.editMyOwnPct)
                    val editFriend = dialogView.findViewById<android.widget.EditText>(R.id.editFriendPct)

                    editMyOwn.setText(myOwnPct.toInt().toString())
                    editFriend.setText(friendPct.toInt().toString())

                    dialogView.findViewById<android.widget.Button>(R.id.btnSaveConfig).setOnClickListener {
                        val newMyOwnStr = editMyOwn.text.toString()
                        val newFriendStr = editFriend.text.toString()

                        if (newMyOwnStr.isEmpty() || newFriendStr.isEmpty()) {
                            android.widget.Toast.makeText(context, "Both percentages required", android.widget.Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val newMyOwn = newMyOwnStr.toInt()
                        val newFriend = newFriendStr.toInt()

                        if (newMyOwn + newFriend != 100) {
                            android.widget.Toast.makeText(context, "My Own + Friend percentages must add up to 100%", android.widget.Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        saveReportConfig(newMyOwn, newFriend)
                        dialog.dismiss()
                    }

                    dialog.show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to load current configuration", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error loading configuration", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReportConfig(view: View, accountId: Int, totalShare: Long) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportConfig(ApiClient.getAuthToken(token), accountId)
                if (response.isSuccessful) {
                    val config = response.body() ?: return@launch
                    val friendPct = (config["friend_percentage"] as? Double ?: 0.0) / 100.0
                    val myOwnPct = (config["my_own_percentage"] as? Double ?: 0.0) / 100.0

                    // Calculate actual amounts from total share
                    val myOwnAmount = (totalShare * myOwnPct).toLong()
                    val friendAmount = (totalShare * friendPct).toLong()

                    // Update UI with detailed breakdown
                    view.findViewById<TextView>(R.id.mySharePercentage)?.text = "₹${String.format("%,d", myOwnAmount)} (${String.format("%.1f", myOwnPct * 100)}%)"
                    view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "₹${String.format("%,d", friendAmount)} (${String.format("%.1f", friendPct * 100)}%)"
                } else {
                    // Fallback: show basic percentage info
                    val myPercentage = 50 // Default fallback
                    val friendPercentage = 50
                    view.findViewById<TextView>(R.id.mySharePercentage)?.text = "$myPercentage%"
                    view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "$friendPercentage%"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: show basic percentage info
                val myPercentage = 50 // Default fallback
                val friendPercentage = 50
                view.findViewById<TextView>(R.id.mySharePercentage)?.text = "$myPercentage%"
                view.findViewById<TextView>(R.id.friendSharePercentage)?.text = "$friendPercentage%"
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