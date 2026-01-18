package com.transactionhub.ui.more

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
import com.transactionhub.data.models.Client
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ClientReportsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClientReportsAdapter
    private var clients: List<Client> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_client_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.clientReportsTitle).text = "Client Performance Reports"

        recyclerView = view.findViewById(R.id.clientReportsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ClientReportsAdapter(emptyList()) { client ->
            showClientReport(client)
        }
        recyclerView.adapter = adapter

        loadClients()
    }

    private fun loadClients() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getClients(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    clients = response.body() ?: emptyList()
                    adapter.updateClients(clients)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showClientReport(client: Client) {
        // Navigate to a detailed client report view
        val reportFragment = ClientDetailReportFragment.newInstance(client.id, client.name)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, reportFragment)
            .addToBackStack(null)
            .commit()
    }
}

class ClientReportsAdapter(
    private var clients: List<Client>,
    private val onItemClick: (Client) -> Unit
) : RecyclerView.Adapter<ClientReportsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clientName: TextView = itemView.findViewById(R.id.clientReportName)
        val clientCode: TextView = itemView.findViewById(R.id.clientReportCode)
        val totalAccounts: TextView = itemView.findViewById(R.id.clientTotalAccounts)
        val totalPnL: TextView = itemView.findViewById(R.id.clientTotalPnL)
        val totalShare: TextView = itemView.findViewById(R.id.clientTotalShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val client = clients[position]
        holder.clientName.text = client.name
        holder.clientCode.text = if (client.code.isNullOrEmpty()) "No Code" else client.code
        holder.totalAccounts.text = "0 accounts"  // Will be updated with real data
        holder.totalPnL.text = "₹0"
        holder.totalShare.text = "₹0"

        holder.itemView.setOnClickListener { onItemClick(client) }
    }

    override fun getItemCount() = clients.size

    fun updateClients(newClients: List<Client>) {
        clients = newClients
        notifyDataSetChanged()
    }
}