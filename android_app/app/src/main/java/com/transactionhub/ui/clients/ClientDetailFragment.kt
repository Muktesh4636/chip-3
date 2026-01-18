package com.transactionhub.ui.clients

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
import com.transactionhub.data.models.Account
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import com.transactionhub.ui.accounts.ExchangeAccountDetailFragment
import com.transactionhub.ui.accounts.LinkAccountFragment
import kotlinx.coroutines.launch

import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import android.widget.EditText

class ClientDetailFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AccountsAdapter
    
    private var clientId: Int = -1
    private var clientName: String = ""
    private var clientCode: String = ""
    private var clientReferredBy: String = ""
    private var clientIsCompany: Boolean = false

    companion object {
        fun newInstance(id: Int, name: String, code: String?, referredBy: String?, isCompany: Boolean): ClientDetailFragment {
            val fragment = ClientDetailFragment()
            val args = Bundle()
            args.putInt("id", id)
            args.putString("name", name)
            args.putString("code", code)
            args.putString("referred_by", referredBy)
            args.putBoolean("is_company", isCompany)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientId = arguments?.getInt("id") ?: -1
        clientName = arguments?.getString("name") ?: ""
        clientCode = arguments?.getString("code") ?: "---"
        clientReferredBy = arguments?.getString("referred_by") ?: ""
        clientIsCompany = arguments?.getBoolean("is_company") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_client_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        view.findViewById<TextView>(R.id.detailClientName).text = clientName
        view.findViewById<TextView>(R.id.detailClientCode).text = "Code: $clientCode"
        
        view.findViewById<Button>(R.id.btnEditClient).setOnClickListener {
            showEditDialog()
        }

        view.findViewById<Button>(R.id.btnDeleteClient).setOnClickListener {
            showDeleteConfirm()
        }

        recyclerView = view.findViewById(R.id.accountsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AccountsAdapter(emptyList()) { acc ->
            val detailFragment = ExchangeAccountDetailFragment.newInstance(acc.id, acc.client_name, acc.exchange_name)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter
        
        loadAccounts()

        view.findViewById<View>(R.id.fabLinkAccount).setOnClickListener {
            val linkFragment = LinkAccountFragment.newInstance(clientId, clientName)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, linkFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showEditDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_client_create, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<TextView>(R.id.createClientTitle).text = "Edit Client"
        val editName = dialogView.findViewById<EditText>(R.id.editClientName)
        val editCode = dialogView.findViewById<EditText>(R.id.editClientCode)
        val editReferredBy = dialogView.findViewById<EditText>(R.id.editClientReferredBy)
        val switchIsCompany = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchIsCompany)
        
        editName.setText(clientName)
        editCode.setText(clientCode)
        editReferredBy.setText(clientReferredBy)
        switchIsCompany.isChecked = clientIsCompany
        
        dialogView.findViewById<Button>(R.id.btnCreateClient).text = "Update"
        dialogView.findViewById<Button>(R.id.btnCreateClient).setOnClickListener {
            val newName = editName.text.toString()
            val newCode = editCode.text.toString()
            val newReferredBy = editReferredBy.text.toString()
            val newIsCompany = switchIsCompany.isChecked
            
            if (newName.isEmpty()) {
                Toast.makeText(context, "Name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            updateClient(newName, newCode, newReferredBy, newIsCompany, dialog)
        }
        
        dialog.show()
    }

    private fun updateClient(newName: String, newCode: String, newReferredBy: String, newIsCompany: Boolean, dialog: AlertDialog) {
        val token = prefManager.getToken() ?: return
        val client = com.transactionhub.data.models.Client(
            id = clientId, 
            name = newName, 
            code = if (newCode.isEmpty()) null else newCode,
            referred_by = if (newReferredBy.isEmpty()) null else newReferredBy,
            is_company_client = newIsCompany
        )
        
        lifecycleScope.launch {
            try {
                val response = apiService.updateClient(ApiClient.getAuthToken(token), clientId, client)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Updated!", Toast.LENGTH_SHORT).show()
                    clientName = newName
                    clientCode = newCode
                    clientReferredBy = newReferredBy
                    clientIsCompany = newIsCompany
                    view?.findViewById<TextView>(R.id.detailClientName)?.text = clientName
                    view?.findViewById<TextView>(R.id.detailClientCode)?.text = "Code: $clientCode"
                    dialog.dismiss()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showDeleteConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Client?")
            .setMessage("Are you sure you want to delete $clientName? All linked accounts will be lost.")
            .setPositiveButton("Delete") { _, _ -> deleteClient() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteClient() {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = apiService.deleteClient(ApiClient.getAuthToken(token), clientId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Client Deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Error deleting client", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadAccounts() {
        val token = prefManager.getToken()
        android.util.Log.d("ClientDetailFragment", "Token available: ${token != null}")

        if (token == null) {
            Toast.makeText(context, "Please login again - authentication token missing", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val authHeader = ApiClient.getAuthToken(token)
                android.util.Log.d("ClientDetailFragment", "Auth header: $authHeader")

                val response = apiService.getAccounts(authHeader)
                android.util.Log.d("ClientDetailFragment", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    // Filter accounts for this client
                    val allAccounts = response.body() ?: emptyList()
                    android.util.Log.d("ClientDetailFragment", "All accounts received: ${allAccounts.size}, clientId: $clientId")

                    val filteredAccounts = allAccounts.filter { it.client == clientId }
                    android.util.Log.d("ClientDetailFragment", "Filtered accounts for client $clientId: ${filteredAccounts.size}")

                    adapter.updateAccounts(filteredAccounts)

                    // Calculate Summary
                    val totalPnL = filteredAccounts.sumOf { it.pnl }
                    val totalShare = filteredAccounts.sumOf { it.my_share }

                    view?.findViewById<TextView>(R.id.clientTotalPnL)?.let { tv ->
                        tv.text = "₹${String.format("%,d", totalPnL)}"
                        tv.setTextColor(resources.getColor(if (totalPnL < 0) R.color.danger else R.color.success, null))
                    }
                    view?.findViewById<TextView>(R.id.clientTotalShare)?.text = "₹${String.format("%,d", totalShare)}"

                    // Show message if no accounts found
                    if (filteredAccounts.isEmpty()) {
                        view?.findViewById<TextView>(R.id.emptyAccountsText)?.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        Toast.makeText(context, "No linked exchanges found for this client", Toast.LENGTH_SHORT).show()
                    } else {
                        view?.findViewById<TextView>(R.id.emptyAccountsText)?.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        Toast.makeText(context, "Loaded ${filteredAccounts.size} linked exchange(s)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("ClientDetailFragment", "API call failed: ${response.code()}, Error: $errorBody")

                    when (response.code()) {
                        401 -> Toast.makeText(context, "Authentication failed - please login again", Toast.LENGTH_LONG).show()
                        403 -> Toast.makeText(context, "Access denied - insufficient permissions", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(context, "Failed to load accounts (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClientDetailFragment", "Exception loading accounts", e)
                Toast.makeText(context, "Network error - check connection", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}

class AccountsAdapter(
    private var accounts: List<Account>,
    private val onItemClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exchangeName: TextView = itemView.findViewById(R.id.accExchangeName)
        val pnlText: TextView = itemView.findViewById(R.id.accPnL)
        val fundingText: TextView = itemView.findViewById(R.id.accFunding)
        val balanceText: TextView = itemView.findViewById(R.id.accBalance)
        val shareText: TextView = itemView.findViewById(R.id.accMyShare)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val acc = accounts[position]
        holder.exchangeName.text = acc.exchange_name
        holder.pnlText.text = "₹${String.format("%,d", acc.pnl)}"
        holder.fundingText.text = "₹${String.format("%,d", acc.funding)}"
        holder.balanceText.text = "₹${String.format("%,d", acc.exchange_balance)}"
        holder.shareText.text = "₹${String.format("%,d", acc.my_share)}"
        
        holder.itemView.setOnClickListener { onItemClick(acc) }
        
        if (acc.pnl < 0) {
            holder.pnlText.setTextColor(holder.itemView.resources.getColor(R.color.danger, null))
        } else {
            holder.pnlText.setTextColor(holder.itemView.resources.getColor(R.color.success, null))
        }
    }
    
    override fun getItemCount() = accounts.size
    
    fun updateAccounts(newAccounts: List<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }
}
