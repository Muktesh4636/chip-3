package com.transactionhub.ui.more

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.data.models.ChangePasswordRequest
import com.transactionhub.data.models.UserProfile
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.profileTitle).text = "User Profile & Settings"

        setupProfileView(view)
        setupButtons(view)
    }

    private fun setupProfileView(view: View) {
        val username = prefManager.getUsername() ?: "Unknown"
        view.findViewById<TextView>(R.id.profileUsername).text = username
        view.findViewById<TextView>(R.id.profileEmail).text = "Loading..."

        // Load user profile (this would need a backend API endpoint)
        loadUserProfile(view)
    }

    private fun loadUserProfile(view: View) {
        lifecycleScope.launch {
            try {
                // For now, just show basic info from login
                val username = prefManager.getUsername()
                val userId = prefManager.getUserId()

                view.findViewById<TextView>(R.id.profileUserId).text = "User ID: $userId"
                view.findViewById<TextView>(R.id.profileUsername).text = "Username: $username"
                view.findViewById<TextView>(R.id.profileEmail).text = "Email: ${username}@example.com" // Placeholder
                view.findViewById<TextView>(R.id.profileJoinDate).text = "Member since: Loading..."
                view.findViewById<TextView>(R.id.profileLastLogin).text = "Last login: Loading..."

            } catch (e: Exception) {
                e.printStackTrace()
                view.findViewById<TextView>(R.id.profileEmail).text = "Error loading profile"
            }
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }

        view.findViewById<Button>(R.id.btnLogoutAll).setOnClickListener {
            showLogoutAllDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Change Password")
            .create()

        val editCurrentPassword = dialogView.findViewById<EditText>(R.id.editCurrentPassword)
        val editNewPassword = dialogView.findViewById<EditText>(R.id.editNewPassword)
        val editConfirmPassword = dialogView.findViewById<EditText>(R.id.editConfirmPassword)
        val btnChangePassword = dialogView.findViewById<Button>(R.id.btnChangePassword)

        btnChangePassword.setOnClickListener {
            val current = editCurrentPassword.text.toString()
            val new = editNewPassword.text.toString()
            val confirm = editConfirmPassword.text.toString()

            if (current.isEmpty() || new.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(context, "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (new != confirm) {
                Toast.makeText(context, "New passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (new.length < 8) {
                Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performPasswordChange(current, new, dialog)
        }

        dialog.show()
    }

    private fun performPasswordChange(currentPassword: String, newPassword: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                // This would need a backend API endpoint for password change
                Toast.makeText(context, "Password change not available via mobile app. Please use web interface.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error changing password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _: android.content.DialogInterface, _: Int ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutAllDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout from All Devices")
            .setMessage("This will logout you from all devices where you're currently logged in. Continue?")
            .setPositiveButton("Logout All") { _: android.content.DialogInterface, _: Int ->
                performLogoutAll()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        prefManager.clearAll()
        requireActivity().finishAffinity()
        // This would typically navigate back to login screen
    }

    private fun performLogoutAll() {
        lifecycleScope.launch {
            try {
                // This would need a backend API endpoint
                Toast.makeText(context, "Logout from all devices not available via mobile app.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}