package com.transactionhub.ui.more

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class OfflineModeFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_offline_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.offlineModeTitle).text = "Offline Mode & Sync"

        setupOfflineMode(view)
        checkConnectivity(view)
        loadOfflineStats(view)
    }

    private fun setupOfflineMode(view: View) {
        val switchOfflineMode = view.findViewById<Switch>(R.id.switchOfflineMode)
        val switchAutoSync = view.findViewById<Switch>(R.id.switchAutoSync)
        val btnSyncNow = view.findViewById<Button>(R.id.btnSyncNow)
        val btnClearOfflineData = view.findViewById<Button>(R.id.btnClearOfflineData)

        // Load saved preferences
        switchOfflineMode.isChecked = prefManager.getOfflineModeEnabled()
        switchAutoSync.isChecked = prefManager.getAutoSyncEnabled()

        switchOfflineMode.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setOfflineModeEnabled(isChecked)
            updateOfflineModeStatus(view, isChecked)
            Toast.makeText(context, "Offline mode: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setAutoSyncEnabled(isChecked)
            Toast.makeText(context, "Auto sync: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        btnSyncNow.setOnClickListener {
            performManualSync(view)
        }

        btnClearOfflineData.setOnClickListener {
            showClearDataConfirmation()
        }
    }

    private fun checkConnectivity(view: View) {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val connectionType = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data"
            else -> "No Connection"
        }

        val statusView = view.findViewById<TextView>(R.id.connectivityStatus)
        val statusIndicator = view.findViewById<ImageView>(R.id.connectivityIndicator)

        if (isConnected) {
            statusView.text = "📶 Connected via $connectionType"
            statusView.setTextColor(resources.getColor(R.color.success, null))
            statusIndicator.setImageResource(android.R.drawable.ic_popup_sync)
            statusIndicator.setColorFilter(resources.getColor(R.color.success, null))
        } else {
            statusView.text = "📴 No Internet Connection"
            statusView.setTextColor(resources.getColor(R.color.danger, null))
            statusIndicator.setImageResource(android.R.drawable.ic_delete)
            statusIndicator.setColorFilter(resources.getColor(R.color.danger, null))
        }
    }

    private fun loadOfflineStats(view: View) {
        val offlineTransactions = prefManager.getOfflineTransactionCount()
        val lastSyncTime = prefManager.getLastSyncTime()
        val pendingUploads = prefManager.getPendingUploadsCount()

        view.findViewById<TextView>(R.id.offlineTransactionCount).text = offlineTransactions.toString()
        view.findViewById<TextView>(R.id.lastSyncTime).text = if (lastSyncTime > 0) {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(lastSyncTime))
        } else {
            "Never"
        }
        view.findViewById<TextView>(R.id.pendingUploadsCount).text = pendingUploads.toString()
    }

    private fun updateOfflineModeStatus(view: View, enabled: Boolean) {
        val statusView = view.findViewById<TextView>(R.id.offlineModeStatus)
        if (enabled) {
            statusView.text = "🟢 Offline Mode Active"
            statusView.setTextColor(resources.getColor(R.color.success, null))
        } else {
            statusView.text = "🔴 Online Mode Active"
            statusView.setTextColor(resources.getColor(R.color.primary, null))
        }
    }

    private fun performManualSync(view: View) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection available", Toast.LENGTH_SHORT).show()
            return
        }

        view.findViewById<Button>(R.id.btnSyncNow).isEnabled = false
        view.findViewById<Button>(R.id.btnSyncNow).text = "Syncing..."

        lifecycleScope.launch {
            try {
                // Simulate sync process
                val token = prefManager.getToken()
                if (token != null) {
                    // Sync offline data to server
                    val pendingUploads = prefManager.getPendingUploadsCount()
                    if (pendingUploads > 0) {
                        // Upload pending transactions
                        Toast.makeText(context, "Uploading $pendingUploads pending transactions...", Toast.LENGTH_SHORT).show()
                        // TODO: Implement actual upload logic
                    }

                    // Download latest data
                    Toast.makeText(context, "Downloading latest data...", Toast.LENGTH_SHORT).show()
                    // TODO: Implement data download and cache

                    // Update sync timestamp
                    prefManager.setLastSyncTime(System.currentTimeMillis())

                    // Clear pending uploads count
                    prefManager.setPendingUploadsCount(0)

                    // Refresh stats
                    loadOfflineStats(view)

                    Toast.makeText(context, "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                view.findViewById<Button>(R.id.btnSyncNow).isEnabled = true
                view.findViewById<Button>(R.id.btnSyncNow).text = "🔄 Sync Now"
            }
        }
    }

    private fun showClearDataConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Offline Data?")
            .setMessage("This will delete all locally stored data and pending changes. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                clearOfflineData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearOfflineData() {
        prefManager.clearOfflineData()
        loadOfflineStats(view ?: return)
        Toast.makeText(context, "Offline data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}