package com.transactionhub.ui.more

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataManagementFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    private val STORAGE_PERMISSION_CODE = 1001

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_data_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.dataManagementTitle).text = "Data Export & Backup"

        setupButtons(view)
        checkPermissions()
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnExportTransactions).setOnClickListener {
            exportTransactions()
        }

        view.findViewById<Button>(R.id.btnExportClients).setOnClickListener {
            exportClients()
        }

        view.findViewById<Button>(R.id.btnExportAccounts).setOnClickListener {
            exportAccounts()
        }

        view.findViewById<Button>(R.id.btnExportAll).setOnClickListener {
            exportAllData()
        }

        view.findViewById<Button>(R.id.btnBackupData).setOnClickListener {
            createBackup()
        }

        view.findViewById<Button>(R.id.btnShareData).setOnClickListener {
            shareData()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun exportTransactions() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val transactions = response.body() ?: emptyList()
                    val csvContent = generateTransactionsCSV(transactions)
                    saveCSVFile("transactions_${getCurrentDateTime()}.csv", csvContent)
                    Toast.makeText(context, "Transactions exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportClients() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getClients(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val clients = response.body() ?: emptyList()
                    val csvContent = generateClientsCSV(clients)
                    saveCSVFile("clients_${getCurrentDateTime()}.csv", csvContent)
                    Toast.makeText(context, "Clients exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to load clients", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportAccounts() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val accounts = response.body() ?: emptyList()
                    val csvContent = generateAccountsCSV(accounts)
                    saveCSVFile("accounts_${getCurrentDateTime()}.csv", csvContent)
                    Toast.makeText(context, "Accounts exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to load accounts", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportAllData() {
        // Export all data to a single comprehensive CSV
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val allData = StringBuilder()
                allData.append("=== TRANSACTIONS ===\n")
                allData.append("ID,Client,Exchange,Date,Type,Amount,Notes\n")

                val transactionsResponse = apiService.getTransactions(ApiClient.getAuthToken(token))
                if (transactionsResponse.isSuccessful) {
                    val transactions = transactionsResponse.body() ?: emptyList()
                    transactions.forEach { txn ->
                        allData.append("${txn.id},${txn.client_name},${txn.exchange_name},${txn.date},${txn.type_display},${txn.amount},\"${txn.notes ?: ""}\"\n")
                    }
                }

                allData.append("\n=== CLIENTS ===\n")
                allData.append("ID,Name,Code,Referred By,Is Company Client\n")

                val clientsResponse = apiService.getClients(ApiClient.getAuthToken(token))
                if (clientsResponse.isSuccessful) {
                    val clients = clientsResponse.body() ?: emptyList()
                    clients.forEach { client ->
                        allData.append("${client.id},${client.name},${client.code ?: ""},${client.referred_by ?: ""},${client.is_company_client}\n")
                    }
                }

                allData.append("\n=== ACCOUNTS ===\n")
                allData.append("ID,Client,Exchange,Funding,Balance,PnL,My Share,Profit Share,Loss Share\n")

                val accountsResponse = apiService.getAccounts(ApiClient.getAuthToken(token))
                if (accountsResponse.isSuccessful) {
                    val accounts = accountsResponse.body() ?: emptyList()
                    accounts.forEach { account ->
                        allData.append("${account.id},${account.client_name},${account.exchange_name},${account.funding},${account.exchange_balance},${account.pnl},${account.my_share},${account.profit_share_percentage},${account.loss_share_percentage}\n")
                    }
                }

                saveCSVFile("complete_data_${getCurrentDateTime()}.txt", allData.toString())
                Toast.makeText(context, "Complete data exported successfully!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createBackup() {
        // Create a timestamped backup
        exportAllData()
        Toast.makeText(context, "Backup created! Files saved to Downloads folder.", Toast.LENGTH_LONG).show()
    }

    private fun shareData() {
        Toast.makeText(context, "Data sharing not available via mobile app. Please use web interface.", Toast.LENGTH_LONG).show()
    }

    private fun generateTransactionsCSV(transactions: List<com.transactionhub.data.models.Transaction>): String {
        val csv = StringBuilder()
        csv.append("ID,Client,Exchange,Date,Type,Amount,Funding Before,Funding After,Balance Before,Balance After,Sequence No,Notes\n")

        transactions.forEach { txn ->
            csv.append("${txn.id},")
            csv.append("\"${txn.client_name}\",")
            csv.append("\"${txn.exchange_name}\",")
            csv.append("${txn.date},")
            csv.append("\"${txn.type_display}\",")
            csv.append("${txn.amount},")
            csv.append("${txn.funding_before ?: ""},")
            csv.append("${txn.funding_after ?: ""},")
            csv.append("${txn.exchange_balance_before ?: ""},")
            csv.append("${txn.exchange_balance_after ?: ""},")
            csv.append("${txn.sequence_no},")
            csv.append("\"${txn.notes ?: ""}\"\n")
        }

        return csv.toString()
    }

    private fun generateClientsCSV(clients: List<com.transactionhub.data.models.Client>): String {
        val csv = StringBuilder()
        csv.append("ID,Name,Code,Referred By,Is Company Client\n")

        clients.forEach { client ->
            csv.append("${client.id},")
            csv.append("\"${client.name}\",")
            csv.append("\"${client.code ?: ""}\",")
            csv.append("\"${client.referred_by ?: ""}\",")
            csv.append("${client.is_company_client}\n")
        }

        return csv.toString()
    }

    private fun generateAccountsCSV(accounts: List<com.transactionhub.data.models.Account>): String {
        val csv = StringBuilder()
        csv.append("ID,Client,Exchange,Funding,Balance,PnL,My Share,Profit Share %,Loss Share %\n")

        accounts.forEach { account ->
            csv.append("${account.id},")
            csv.append("\"${account.client_name}\",")
            csv.append("\"${account.exchange_name}\",")
            csv.append("${account.funding},")
            csv.append("${account.exchange_balance},")
            csv.append("${account.pnl},")
            csv.append("${account.my_share},")
            csv.append("${account.profit_share_percentage},")
            csv.append("${account.loss_share_percentage}\n")
        }

        return csv.toString()
    }

    private fun saveCSVFile(fileName: String, content: String) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                writer.write(content)
            }

            // Show file location to user
            Toast.makeText(context, "File saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }
}