package com.transactionhub.ui.accounts

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
import com.transactionhub.data.models.RecordPaymentRequest
import com.transactionhub.ui.transactions.TransactionsAdapter
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class AccountDetailFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    
    private var accountId: Int = -1
    private var accountName: String = ""

    companion object {
        fun newInstance(id: Int, name: String): AccountDetailFragment {
            val fragment = AccountDetailFragment()
            val args = Bundle()
            args.putInt("id", id)
            args.putString("name", name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = arguments?.getInt("id") ?: -1
        accountName = arguments?.getString("name") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        view.findViewById<TextView>(R.id.accDetailName).text = accountName
        
        recyclerView = view.findViewById(R.id.accHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionsAdapter(emptyList())
        recyclerView.adapter = adapter
        
        view.findViewById<Button>(R.id.btnFunding).setOnClickListener {
            showTransactionDialog("Add Funding", actionType = "FUNDING")
        }
        
        view.findViewById<Button>(R.id.btnUpdateBalance).setOnClickListener {
            showTransactionDialog("Update Balance", actionType = "BALANCE")
        }

        view.findViewById<Button>(R.id.btnPayment).setOnClickListener {
            showTransactionDialog("Record Payment", actionType = "PAYMENT")
        }

        view.findViewById<Button>(R.id.btnAccSettings).setOnClickListener {
            showSettingsDialog()
        }

        view.findViewById<Button>(R.id.btnReportConfig).setOnClickListener {
            showReportConfigDialog()
        }

        view.findViewById<Button>(R.id.btnAccountActions).setOnClickListener {
            showAccountActionsDialog()
        }

        loadHistory()
    }

    private fun showReportConfigDialog() {
        val token = prefManager.getToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = apiService.getReportConfig(ApiClient.getAuthToken(token), accountId)
                if (response.isSuccessful) {
                    val config = response.body() ?: return@launch
                    val friendPct = config["friend_percentage"] as? Double ?: 0.0
                    val myOwnPct = config["my_own_percentage"] as? Double ?: 0.0
                    val totalShare = config["my_total_percentage"] as? Double ?: 0.0
                    
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_config, null)
                    val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
                    
                    dialogView.findViewById<TextView>(R.id.totalShareInfo).text = "Total Profit Share: ${totalShare.toInt()}%"
                    val editMyOwn = dialogView.findViewById<EditText>(R.id.editMyOwnPct)
                    val editFriend = dialogView.findViewById<EditText>(R.id.editFriendPct)
                    
                    editMyOwn.setText(myOwnPct.toInt().toString())
                    editFriend.setText(friendPct.toInt().toString())
                    
                    dialogView.findViewById<Button>(R.id.btnSaveConfig).setOnClickListener {
                        val newMyOwnStr = editMyOwn.text.toString()
                        val newFriendStr = editFriend.text.toString()
                        
                        if (newMyOwnStr.isEmpty() || newFriendStr.isEmpty()) {
                            Toast.makeText(context, "Both percentages required", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        
                        val newMyOwn = newMyOwnStr.toInt()
                        val newFriend = newFriendStr.toInt()
                        
                        // Validation: total must equal account's profit share percentage
                        if (newMyOwn + newFriend != totalShare.toInt()) {
                            Toast.makeText(context, "Sum must equal total share (${totalShare.toInt()}%)", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        
                        updateReportConfig(newMyOwnStr, newFriendStr, dialog)
                    }
                    dialog.show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateReportConfig(myOwn: String, friend: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return
        val data = mapOf("my_own_percentage" to myOwn, "friend_percentage" to friend)
        
        lifecycleScope.launch {
            try {
                val response = apiService.updateReportConfig(ApiClient.getAuthToken(token), accountId, data)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Config Saved!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Error saving config", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showAccountActionsDialog() {
        val actions = arrayOf(
            "Edit Account Settings",
            "Add Funding",
            "Update Balance",
            "Record Payment",
            "View Account Details",
            "Delete Account"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Account Actions")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showSettingsDialog()
                    1 -> showTransactionDialog("Add Funding", actionType = "FUNDING")
                    2 -> showTransactionDialog("Update Balance", actionType = "BALANCE")
                    3 -> showTransactionDialog("Record Payment", actionType = "PAYMENT")
                    4 -> showAccountInfoDialog()
                    5 -> showDeleteAccountDialog()
                }
            }
            .show()
    }

    private fun showAccountInfoDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_account_info, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Account Information")
            .create()

        // Populate account info
        lifecycleScope.launch {
            try {
                val token = prefManager.getToken() ?: return@launch
                val response = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val accounts = response.body() ?: emptyList()
                    val account = accounts.find { it.id == accountId }
                    if (account != null) {
                        dialogView.findViewById<TextView>(R.id.accountInfoClient).text = account.client_name
                        dialogView.findViewById<TextView>(R.id.accountInfoExchange).text = account.exchange_name
                        dialogView.findViewById<TextView>(R.id.accountInfoFunding).text = "₹${String.format("%,d", account.funding)}"
                        dialogView.findViewById<TextView>(R.id.accountInfoBalance).text = "₹${String.format("%,d", account.exchange_balance)}"
                        dialogView.findViewById<TextView>(R.id.accountInfoPnL).text = "₹${String.format("%,d", account.pnl)}"
                        dialogView.findViewById<TextView>(R.id.accountInfoShare).text = "₹${String.format("%,d", account.my_share)}"
                        dialogView.findViewById<TextView>(R.id.accountInfoProfitShare).text = "${account.profit_share_percentage}%"
                        dialogView.findViewById<TextView>(R.id.accountInfoLossShare).text = "${account.loss_share_percentage}%"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dialogView.findViewById<Button>(R.id.btnCloseAccountInfo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account?")
            .setMessage("Are you sure you want to delete this account? This will remove all associated transactions and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                // Note: We don't have a direct delete account API, so we'll show a message
                Toast.makeText(context, "Account deletion not available via mobile app. Please use web interface.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_account_settings, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        val editProfit = dialogView.findViewById<EditText>(R.id.editProfitShare)
        val editLoss = dialogView.findViewById<EditText>(R.id.editLossShare)
        
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val pShare = editProfit.text.toString()
            val lShare = editLoss.text.toString()
            
            if (pShare.isEmpty() || lShare.isEmpty()) {
                Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveSettings(pShare, lShare, dialog)
        }
        
        dialog.show()
    }

    private fun saveSettings(pShare: String, lShare: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return
        val data = mapOf("profit_share" to pShare, "loss_share" to lShare)
        
        lifecycleScope.launch {
            try {
                val response = apiService.updateAccountSettings(ApiClient.getAuthToken(token), accountId, data)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Settings Updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showTransactionDialog(title: String, actionType: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_transaction_form, null)
        val dialog = AlertDialog.Builder(requireContext())
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

    private fun submitTransaction(amount: String, notes: String, actionType: String, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = when (actionType) {
                    "FUNDING" -> {
                        val data = mapOf("amount" to amount, "notes" to notes)
                        apiService.addFunding(ApiClient.getAuthToken(token), accountId, data)
                    }
                    "BALANCE" -> {
                        val data = mapOf("amount" to amount, "notes" to notes)
                        apiService.updateBalance(ApiClient.getAuthToken(token), accountId, data)
                    }
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
                    loadHistory() // Refresh the list
                } else {
                    Toast.makeText(context, "Error saving data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadHistory() {
        val token = prefManager.getToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val allTxns = response.body() ?: emptyList()
                    val filtered = allTxns.filter { it.client_exchange == accountId }
                    adapter.updateTransactions(filtered)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
