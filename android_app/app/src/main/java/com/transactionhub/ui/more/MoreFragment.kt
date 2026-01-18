package com.transactionhub.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.LoginActivity
import com.transactionhub.R
import com.transactionhub.ui.exchanges.ExchangeCreateFragment
import com.transactionhub.ui.exchanges.ExchangesFragment
import com.transactionhub.ui.clients.ClientCreateFragment
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoreFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        
        view.findViewById<View>(R.id.menuExchanges).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExchangesFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<View>(R.id.menuReports).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCustomReports).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CustomReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuClientReports).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ClientReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuExchangeReports).setOnClickListener {
            // TODO: Implement ExchangeReportsFragment
            Toast.makeText(context, "Exchange reports coming soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.menuTimeTravel).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TimeTravelFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuAdvancedSearch).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdvancedSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuUserProfile).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuAuditLogs).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AuditLogsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCharts).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChartsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCreateExchange).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ExchangeCreateFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuCreateClient).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ClientCreateFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.menuExportPending).setOnClickListener {
            exportPendingPayments()
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefManager.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun exportPendingPayments() {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.exportPendingPayments(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Save CSV to Downloads folder
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName = "pending_payments_$timestamp.csv"

                        val downloadsDir = File(requireContext().getExternalFilesDir(null), "Downloads")
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }

                        val file = File(downloadsDir, fileName)
                        val fos = FileOutputStream(file)
                        fos.write(body.bytes())
                        fos.close()

                        Toast.makeText(
                            requireContext(),
                            "CSV exported to: ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
