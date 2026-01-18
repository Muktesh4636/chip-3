package com.transactionhub.ui.exchanges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Exchange
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ExchangesFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExchangesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchanges, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        recyclerView = view.findViewById(R.id.exchangesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ExchangesAdapter(
            emptyList(),
            onItemClick = { exchange ->
                showExchangeDetails(exchange)
            },
            onItemLongClick = { exchange ->
                showDeleteConfirm(exchange)
            }
        )
        recyclerView.adapter = adapter
        
        loadExchanges()

        view.findViewById<View>(R.id.fabAddExchange).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExchangeCreateFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showExchangeDetails(exchange: Exchange) {
        val detailFragment = ExchangeDetailFragment.newInstance(exchange.id, exchange.name, exchange.code)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDeleteConfirm(exchange: Exchange) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Exchange?")
            .setMessage("Are you sure you want to delete ${exchange.name}? This will remove all associated client accounts.")
            .setPositiveButton("Delete") { _, _ -> deleteExchange(exchange.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExchange(id: Int) {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = apiService.deleteExchange(ApiClient.getAuthToken(token), id)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Exchange Deleted", Toast.LENGTH_SHORT).show()
                    loadExchanges()
                } else {
                    Toast.makeText(context, "Error deleting exchange", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun loadExchanges() {
        val token = prefManager.getToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = apiService.getExchanges(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    adapter.updateExchanges(response.body()!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ExchangesAdapter(
    private var exchanges: List<Exchange>,
    private val onItemClick: (Exchange) -> Unit,
    private val onItemLongClick: (Exchange) -> Unit
) : RecyclerView.Adapter<ExchangesAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.exchangeNameText)
        val versionText: TextView = itemView.findViewById(R.id.exchangeVersionText)
        val codeText: TextView = itemView.findViewById(R.id.exchangeCodeText)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exchange, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exchange = exchanges[position]
        holder.nameText.text = exchange.name
        holder.versionText.text = exchange.version_name ?: "Default Version"
        holder.codeText.text = exchange.code ?: "---"

        holder.itemView.setOnClickListener { onItemClick(exchange) }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(exchange)
            true
        }
    }
    
    override fun getItemCount() = exchanges.size
    
    fun updateExchanges(newExchanges: List<Exchange>) {
        exchanges = newExchanges
        notifyDataSetChanged()
    }
}
