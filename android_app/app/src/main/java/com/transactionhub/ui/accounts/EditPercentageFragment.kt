package com.transactionhub.ui.accounts

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
import kotlin.math.abs

class EditPercentageFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private var accountId: Int = -1
    private var clientName: String = ""
    private var exchangeName: String = ""

    companion object {
        fun newInstance(accountId: Int, clientName: String, exchangeName: String): EditPercentageFragment {
            val fragment = EditPercentageFragment()
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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_percentage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        // Set title
        view.findViewById<TextView>(R.id.editTitle).text = "Edit $exchangeName Configuration for $clientName"

        // Load current account data
        loadAccountData(view)

        // Setup buttons
        view.findViewById<Button>(R.id.btnSave)?.setOnClickListener {
            saveChanges(view)
        }

        view.findViewById<Button>(R.id.btnCancel)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup validation and auto-calculation
        setupValidation(view)
    }

    private fun loadAccountData(view: View) {
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
                        // Populate form fields - use loss_share_percentage as default since it represents the total percentage
                        val totalPercentage = if (account.loss_share_percentage > 0) account.loss_share_percentage else 100
                        view.findViewById<EditText>(R.id.myPercentageInput)?.setText(totalPercentage.toString())

                        // Load report configuration
                        loadReportConfig(view)
                    } else {
                        Toast.makeText(context, "Account not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to load account data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading account data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReportConfig(view: View) {
        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.getReportConfig(ApiClient.getAuthToken(token), accountId)
                if (response.isSuccessful) {
                    val config = response.body()
                    if (config != null && config.isNotEmpty()) {
                        val friendPct = (config["friend_percentage"] as? Double ?: 0.0).toInt()
                        val myOwnPct = (config["my_own_percentage"] as? Double ?: 0.0).toInt()

                        view.findViewById<EditText>(R.id.friendPercentageInput)?.setText(friendPct.toString())
                        view.findViewById<EditText>(R.id.myOwnPercentageInput)?.setText(myOwnPct.toString())

                        // Show report configuration section
                        view.findViewById<LinearLayout>(R.id.reportConfigSection)?.visibility = View.VISIBLE
                    } else {
                        // Hide report configuration section
                        view.findViewById<LinearLayout>(R.id.reportConfigSection)?.visibility = View.GONE
                    }
                } else {
                    // Hide report configuration section
                    view.findViewById<LinearLayout>(R.id.reportConfigSection)?.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Hide report configuration section
                view.findViewById<LinearLayout>(R.id.reportConfigSection)?.visibility = View.GONE
            }
        }
    }

    private fun setupValidation(view: View) {
        val myTotalInput = view.findViewById<EditText>(R.id.myPercentageInput)
        val friendInput = view.findViewById<EditText>(R.id.friendPercentageInput)
        val myOwnInput = view.findViewById<EditText>(R.id.myOwnPercentageInput)
        val summaryDiv = view.findViewById<TextView>(R.id.percentageSummary)

        if (myTotalInput == null || friendInput == null || myOwnInput == null || summaryDiv == null) return

        var isUpdating = false // Prevent infinite loops

        fun updateValidation() {
            if (isUpdating) return

            val myTotal = myTotalInput.text.toString().toDoubleOrNull() ?: 0.0
            val friend = friendInput.text.toString().toDoubleOrNull() ?: 0.0
            val myOwn = myOwnInput.text.toString().toDoubleOrNull() ?: 0.0
            val sum = friend + myOwn

            // Update validation message (round to 2 decimal places for display)
            val myTotalDisplay = String.format("%.2f", myTotal)
            val friendDisplay = String.format("%.2f", friend)
            val myOwnDisplay = String.format("%.2f", myOwn)
            val sumDisplay = String.format("%.2f", sum)
            val diffDisplay = String.format("%.2f", abs(sum - myTotal))

            // Use small epsilon for floating point comparison
            val epsilon = 0.01

            if (abs(myTotal) < epsilon) {
                summaryDiv.setTextColor(resources.getColor(R.color.text_muted, null))
                summaryDiv.text = "Current: Company % ($friendDisplay) + My Own % ($myOwnDisplay) = $sumDisplay | My Total %: 0"
            } else if (abs(sum - myTotal) < epsilon) {
                summaryDiv.setTextColor(resources.getColor(R.color.success, null))
                summaryDiv.text = "✅ Valid: Company % ($friendDisplay) + My Own % ($myOwnDisplay) = $sumDisplay | My Total %: $myTotalDisplay"
            } else {
                summaryDiv.setTextColor(resources.getColor(R.color.danger, null))
                summaryDiv.text = "❌ Invalid: Company % ($friendDisplay) + My Own % ($myOwnDisplay) = $sumDisplay | My Total %: $myTotalDisplay (Difference: $diffDisplay)"
            }
        }

        // Auto-calculate when inputs change
        myTotalInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                isUpdating = true

                val myTotal = myTotalInput.text.toString().toDoubleOrNull() ?: 0.0
                val friend = friendInput.text.toString().toDoubleOrNull() ?: 0.0
                val myOwn = myOwnInput.text.toString().toDoubleOrNull() ?: 0.0
                val currentSum = friend + myOwn
                val epsilon = 0.01

                if (myTotal > 0) {
                    // If user is currently editing Friend %, keep it and calculate My Own %
                    if (friendInput.hasFocus() && friend > 0 && friend <= myTotal) {
                        myOwnInput.setText(String.format("%.2f", myTotal - friend))
                    }
                    // If user is currently editing My Own %, keep it and calculate Friend %
                    else if (myOwnInput.hasFocus() && myOwn > 0 && myOwn <= myTotal) {
                        friendInput.setText(String.format("%.2f", myTotal - myOwn))
                    }
                    // If both fields already have values and they sum correctly, keep them
                    else if (abs(currentSum - myTotal) < epsilon && friend > 0 && myOwn > 0) {
                        // Values already match, no change needed
                    }
                    // When My Total % is entered/changed, auto-fill both fields
                    else {
                        friendInput.setText("0.00")
                        myOwnInput.setText(String.format("%.2f", myTotal))
                    }
                } else {
                    friendInput.setText("0.00")
                    myOwnInput.setText("0.00")
                }

                isUpdating = false
                updateValidation()
            }
        })

        // When Company % changes, auto-calculate My Own %
        friendInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating || friendInput.hasFocus()) return
                val myTotal = myTotalInput.text.toString().toDoubleOrNull() ?: 0.0
                val friend = friendInput.text.toString().toDoubleOrNull() ?: 0.0
                if (myTotal > 0) {
                    myOwnInput.setText(String.format("%.2f", maxOf(0.0, minOf(100.0, myTotal - friend))))
                }
                updateValidation()
            }
        })

        // When My Own % changes, auto-calculate Company %
        myOwnInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating || myOwnInput.hasFocus()) return
                val myTotal = myTotalInput.text.toString().toDoubleOrNull() ?: 0.0
                val myOwn = myOwnInput.text.toString().toDoubleOrNull() ?: 0.0
                if (myTotal > 0) {
                    friendInput.setText(String.format("%.2f", maxOf(0.0, minOf(100.0, myTotal - myOwn))))
                }
                updateValidation()
            }
        })

        // Initial validation
        updateValidation()
    }

    private fun saveChanges(view: View) {
        val myPercentage = view.findViewById<EditText>(R.id.myPercentageInput)?.text.toString().toDoubleOrNull() ?: 0.0
        val friendPercentage = view.findViewById<EditText>(R.id.friendPercentageInput)?.text.toString().toDoubleOrNull() ?: 0.0
        val myOwnPercentage = view.findViewById<EditText>(R.id.myOwnPercentageInput)?.text.toString().toDoubleOrNull() ?: 0.0

        // Validation
        if (myPercentage < 0 || myPercentage > 100) {
            Toast.makeText(context, "My Total % must be between 0 and 100", Toast.LENGTH_SHORT).show()
            return
        }

        val sum = friendPercentage + myOwnPercentage
        val epsilon = 0.01
        if (myPercentage > 0 && abs(sum - myPercentage) >= epsilon) {
            Toast.makeText(context, "Company % + My Own % must equal My Total %", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Save changes to backend
        Toast.makeText(context, "Percentage update functionality coming soon", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }
}