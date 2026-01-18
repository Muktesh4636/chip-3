package com.transactionhub.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Client
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class ClientsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClientsAdapter
    private var allClients: List<Client> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clients, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        recyclerView = view.findViewById(R.id.clientsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ClientsAdapter(emptyList()) { client ->
            val detailFragment = ClientDetailFragment.newInstance(
                client.id, 
                client.name, 
                client.code,
                client.referred_by,
                client.is_company_client
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter
        
        loadClients()
        
        view.findViewById<View>(R.id.fabAddClient).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ClientCreateFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<EditText>(R.id.searchClients).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterClients(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun filterClients(query: String) {
        val filtered = allClients.filter { 
            it.name.contains(query, ignoreCase = true) || 
            (it.code?.contains(query, ignoreCase = true) ?: false)
        }
        adapter.updateClients(filtered)
    }

    private fun loadClients() {
        val token = prefManager.getToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = apiService.getClients(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    allClients = response.body() ?: emptyList()
                    adapter.updateClients(allClients)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ClientsAdapter(
    private var clients: List<Client>,
    private val onItemClick: (Client) -> Unit
) : RecyclerView.Adapter<ClientsAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: android.widget.TextView = itemView.findViewById(R.id.clientNameText)
        val codeText: android.widget.TextView = itemView.findViewById(R.id.clientCodeText)
        val companyBadge: android.widget.TextView = itemView.findViewById(R.id.companyBadge)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val client = clients[position]
        holder.nameText.text = client.name
        holder.codeText.text = client.code ?: "No Code"
        holder.companyBadge.visibility = if (client.is_company_client) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onItemClick(client) }
    }
    
    override fun getItemCount() = clients.size
    
    fun updateClients(newClients: List<Client>) {
        clients = newClients
        notifyDataSetChanged()
    }
}
