package com.transactionhub.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Exchange
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class LinkAccountFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private var clientId: Int = -1
    private var clientName: String = ""
    private var exchanges: List<Exchange> = emptyList()
    private var isUpdating = false

    companion object {
        fun newInstance(clientId: Int, clientName: String): LinkAccountFragment {
            val fragment = LinkAccountFragment()
            val args = Bundle()
            args.putInt("clientId", clientId)
            args.putString("clientName", clientName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientId = arguments?.getInt("clientId") ?: -1
        clientName = arguments?.getString("clientName") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_link_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.linkClientName).text = clientName

        loadExchanges(view)
        setupPercentageValidation(view)

        view.findViewById<Button>(R.id.btnLinkAccount).setOnClickListener {
            performLinkAccount(view)
        }
    }

    private fun loadExchanges(view: View) {
        val token = prefManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = apiService.getExchanges(ApiClient.getAuthToken(token))
                if (response.isSuccessful) {
                    exchanges = response.body() ?: emptyList()
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                        exchanges.map { "${it.name}${if (it.version_name?.isNotEmpty() == true) " - ${it.version_name}" else ""}${if (it.code?.isNotEmpty() == true) " (${it.code})" else ""}" })
                    view.findViewById<Spinner>(R.id.spinnerExchanges).adapter = adapter

                    // Setup exchange version display
                    val spinner = view.findViewById<Spinner>(R.id.spinnerExchanges)
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            updateExchangeVersionDisplay(position)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateExchangeVersionDisplay(position: Int) {
        val exchangeVersionDisplay = view?.findViewById<TextView>(R.id.exchangeVersionDisplay)
        if (position >= 0 && position < exchanges.size) {
            val exchange = exchanges[position]
            val displayText = "Exchange: ${exchange.name}${if (exchange.code?.isNotEmpty() == true) " (${exchange.code})" else ""}\nVersion: ${exchange.version_name?.ifEmpty { "Not specified" } ?: "Not specified"}"
            exchangeVersionDisplay?.text = displayText
            exchangeVersionDisplay?.visibility = View.VISIBLE
        } else {
            exchangeVersionDisplay?.visibility = View.GONE
        }
    }

    private fun setupPercentageValidation(view: View) {
        val myTotalEdit = view.findViewById<EditText>(R.id.editMyTotalPercentage)
        val friendEdit = view.findViewById<EditText>(R.id.editFriendPercentage)
        val myOwnEdit = view.findViewById<EditText>(R.id.editMyOwnPercentage)
        val summaryText = view.findViewById<TextView>(R.id.percentageSummary)

        // Add text change listeners
        myTotalEdit.addTextChangedListener { updateValidation() }
        friendEdit.addTextChangedListener { autoCalculateMyOwn() }
        myOwnEdit.addTextChangedListener { autoCalculateFriend() }

        // Initial validation
        updateValidation()
    }

    private fun updateValidation() {
        if (isUpdating) return

        val myTotal = view?.findViewById<EditText>(R.id.editMyTotalPercentage)?.text.toString().toFloatOrNull() ?: 0f
        val friend = view?.findViewById<EditText>(R.id.editFriendPercentage)?.text.toString().toFloatOrNull() ?: 0f
        val myOwn = view?.findViewById<EditText>(R.id.editMyOwnPercentage)?.text.toString().toFloatOrNull() ?: 0f
        val summary = friend + myOwn

        val epsilon = 0.01f

        val summaryTextView = view?.findViewById<TextView>(R.id.percentageSummary)
        if (Math.abs(myTotal) < epsilon) {
            summaryTextView?.setTextColor(resources.getColor(R.color.text_muted, null))
            summaryTextView?.text = "Current: Company % (${"%.2f".format(friend)}) + My Own % (${"%.2f".format(myOwn)}) = ${"%.2f".format(summary)} | My Total %: 0"
        } else if (Math.abs(summary - myTotal) < epsilon) {
            summaryTextView?.setTextColor(resources.getColor(R.color.success, null))
            summaryTextView?.text = "✅ Valid: Company % (${"%.2f".format(friend)}) + My Own % (${"%.2f".format(myOwn)}) = ${"%.2f".format(summary)} | My Total %: ${"%.2f".format(myTotal)}"
        } else {
            summaryTextView?.setTextColor(resources.getColor(R.color.danger, null))
            val diff = Math.abs(summary - myTotal)
            summaryTextView?.text = "❌ Invalid: Company % (${"%.2f".format(friend)}) + My Own % (${"%.2f".format(myOwn)}) = ${"%.2f".format(summary)} | My Total %: ${"%.2f".format(myTotal)} (Difference: ${"%.2f".format(diff)})"
        }
    }

    private fun autoCalculateMyOwn() {
        if (isUpdating) return
        isUpdating = true

        val myTotal = view?.findViewById<EditText>(R.id.editMyTotalPercentage)?.text.toString().toFloatOrNull() ?: 0f
        val friend = view?.findViewById<EditText>(R.id.editFriendPercentage)?.text.toString().toFloatOrNull() ?: 0f

        if (myTotal > 0) {
            // Ensure Company % doesn't exceed My Total %
            if (friend > myTotal) {
                view?.findViewById<EditText>(R.id.editFriendPercentage)?.setText("%.2f".format(myTotal))
                view?.findViewById<EditText>(R.id.editMyOwnPercentage)?.setText("0.00")
            } else {
                // Calculate My Own % = My Total % - Company %
                val calculatedMyOwn = myTotal - friend
                view?.findViewById<EditText>(R.id.editMyOwnPercentage)?.setText("%.2f".format(Math.max(0f, Math.min(100f, calculatedMyOwn))))
            }
        } else {
            view?.findViewById<EditText>(R.id.editMyOwnPercentage)?.setText("0.00")
        }

        isUpdating = false
        updateValidation()
    }

    private fun autoCalculateFriend() {
        if (isUpdating) return
        isUpdating = true

        val myTotal = view?.findViewById<EditText>(R.id.editMyTotalPercentage)?.text.toString().toFloatOrNull() ?: 0f
        val myOwn = view?.findViewById<EditText>(R.id.editMyOwnPercentage)?.text.toString().toFloatOrNull() ?: 0f

        if (myTotal > 0) {
            // Ensure My Own % doesn't exceed My Total %
            if (myOwn > myTotal) {
                view?.findViewById<EditText>(R.id.editMyOwnPercentage)?.setText("%.2f".format(myTotal))
                view?.findViewById<EditText>(R.id.editFriendPercentage)?.setText("0.00")
            } else {
                // Calculate Company % = My Total % - My Own %
                val calculatedFriend = myTotal - myOwn
                view?.findViewById<EditText>(R.id.editFriendPercentage)?.setText("%.2f".format(Math.max(0f, Math.min(100f, calculatedFriend))))
            }
        } else {
            view?.findViewById<EditText>(R.id.editFriendPercentage)?.setText("0.00")
        }

        isUpdating = false
        updateValidation()
    }

    private fun performLinkAccount(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerExchanges)
        val myTotalPercentage = view.findViewById<EditText>(R.id.editMyTotalPercentage).text.toString()
        val friendPercentage = view.findViewById<EditText>(R.id.editFriendPercentage).text.toString()
        val myOwnPercentage = view.findViewById<EditText>(R.id.editMyOwnPercentage).text.toString()

        if (spinner.selectedItemPosition < 0 || exchanges.isEmpty()) {
            Toast.makeText(context, "Please select an exchange", Toast.LENGTH_SHORT).show()
            return
        }

        if (myTotalPercentage.isEmpty()) {
            Toast.makeText(context, "Please enter My Total %", Toast.LENGTH_SHORT).show()
            return
        }

        val myTotal = myTotalPercentage.toFloatOrNull() ?: 0f
        val friend = friendPercentage.toFloatOrNull() ?: 0f
        val myOwn = myOwnPercentage.toFloatOrNull() ?: 0f
        val sum = friend + myOwn
        val epsilon = 0.01f

        if (myTotal > 0 && Math.abs(sum - myTotal) >= epsilon) {
            Toast.makeText(context, "Validation Error: Company % + My Own % must equal My Total %", Toast.LENGTH_LONG).show()
            return
        }

        val exchange = exchanges[spinner.selectedItemPosition]

        // Map to API fields - exactly matching the website backend
        val data = mapOf(
            "client" to clientId.toString(),
            "exchange" to exchange.id.toString(),
            "my_percentage" to myTotalPercentage, // Required: My Total %
            "friend_percentage" to friendPercentage, // Optional: Company %
            "my_own_percentage" to myOwnPercentage // Optional: My Own %
        )

        val token = prefManager.getToken() ?: return

        lifecycleScope.launch {
            try {
                val response = apiService.linkAccount(ApiClient.getAuthToken(token), data)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Account Linked Successfully!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Error: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
