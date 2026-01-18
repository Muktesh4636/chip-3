package com.transactionhub.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.LoginActivity
import com.transactionhub.R
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {
    private lateinit var prefManager: PrefManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())

        // Display user information
        view.findViewById<TextView>(R.id.tvUsername).text = prefManager.getUsername() ?: "User"
        view.findViewById<TextView>(R.id.tvUserId).text = "ID: ${prefManager.getUserId() ?: "N/A"}"
        view.findViewById<TextView>(R.id.tvJoinDate).text = "Joined: Recently"

        // Logout button
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }

        // Change password button
        view.findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun logout() {
        // Clear local data and navigate to login
        prefManager.clearAll()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Change Password")
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                val currentPassword = dialogView.findViewById<android.widget.EditText>(R.id.editCurrentPassword).text.toString()
                val newPassword = dialogView.findViewById<android.widget.EditText>(R.id.editNewPassword).text.toString()
                val confirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.editConfirmPassword).text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(context, "New passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (newPassword.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                changePassword(currentPassword, newPassword)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        // TODO: Implement password change API
        Toast.makeText(context, "Password change feature coming soon", Toast.LENGTH_SHORT).show()
    }
}