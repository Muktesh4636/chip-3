package com.transactionhub.ui.more

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
import com.transactionhub.data.models.AuditLogEntry
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class AuditLogsFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AuditLogsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_audit_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.auditLogsTitle).text = "Audit Logs"

        recyclerView = view.findViewById(R.id.auditLogsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AuditLogsAdapter(emptyList())
        recyclerView.adapter = adapter

        loadAuditLogs(view)
    }

    private fun loadAuditLogs(view: View) {
        val token = prefManager.getToken() ?: return

        view.findViewById<TextView>(R.id.auditLogsStatus).text = "Loading audit logs..."
        view.findViewById<TextView>(R.id.auditLogsStatus).visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // This would need a backend API endpoint for audit logs
                // For now, show sample data
                val sampleLogs = listOf(
                    AuditLogEntry(1, "2024-01-20 10:30:00", "LOGIN", "User logged in successfully", "192.168.1.1", "Chrome/120.0"),
                    AuditLogEntry(2, "2024-01-20 10:35:00", "TRANSACTION_CREATED", "Created funding transaction #1234", "192.168.1.1", "Chrome/120.0"),
                    AuditLogEntry(3, "2024-01-20 10:40:00", "ACCOUNT_UPDATED", "Updated account settings for client ABC", "192.168.1.1", "Chrome/120.0"),
                    AuditLogEntry(4, "2024-01-20 11:00:00", "TRANSACTION_EDITED", "Edited transaction #1234 amount", "192.168.1.1", "Chrome/120.0"),
                    AuditLogEntry(5, "2024-01-20 11:15:00", "PAYMENT_RECORDED", "Recorded payment of ₹50,000", "192.168.1.1", "Chrome/120.0")
                )

                adapter.updateLogs(sampleLogs)
                view.findViewById<TextView>(R.id.auditLogsStatus).visibility = View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
                view.findViewById<TextView>(R.id.auditLogsStatus).text = "Error loading audit logs"
            }
        }
    }
}

class AuditLogsAdapter(
    private var logs: List<AuditLogEntry>
) : RecyclerView.Adapter<AuditLogsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampText: TextView = itemView.findViewById(R.id.logTimestamp)
        val actionText: TextView = itemView.findViewById(R.id.logAction)
        val detailsText: TextView = itemView.findViewById(R.id.logDetails)
        val ipText: TextView = itemView.findViewById(R.id.logIP)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audit_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        holder.timestampText.text = log.timestamp
        holder.actionText.text = log.action.replace("_", " ")
        holder.detailsText.text = log.details
        holder.ipText.text = log.ip_address ?: "Unknown"

        // Color coding based on action type
        when {
            log.action.contains("LOGIN") -> {
                holder.actionText.setTextColor(holder.itemView.resources.getColor(R.color.success, null))
            }
            log.action.contains("DELETE") -> {
                holder.actionText.setTextColor(holder.itemView.resources.getColor(R.color.danger, null))
            }
            log.action.contains("EDIT") -> {
                holder.actionText.setTextColor(holder.itemView.resources.getColor(R.color.warning, null))
            }
            else -> {
                holder.actionText.setTextColor(holder.itemView.resources.getColor(R.color.primary, null))
            }
        }
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<AuditLogEntry>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}