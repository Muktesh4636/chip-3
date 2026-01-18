package com.transactionhub.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.Client
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ClientCreateFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_client_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService
        
        view.findViewById<Button>(R.id.btnCreateClient).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.editClientName).text.toString()
            val code = view.findViewById<EditText>(R.id.editClientCode).text.toString()
            val referredBy = view.findViewById<EditText>(R.id.editClientReferredBy).text.toString()
            val isCompany = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchIsCompany).isChecked
            
            if (name.isEmpty()) {
                Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            createClient(name, code, referredBy, isCompany)
        }
    }

    private fun createClient(name: String, code: String, referredBy: String, isCompany: Boolean) {
        val token = prefManager.getToken() ?: return
        val newClient = Client(
            id = 0, 
            name = name, 
            code = if (code.isEmpty()) null else code, 
            referred_by = if (referredBy.isEmpty()) null else referredBy, 
            is_company_client = isCompany
        )
        
        lifecycleScope.launch {
            try {
                val response = apiService.createClient(ApiClient.getAuthToken(token), newClient)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Client Created!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Error creating client", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
