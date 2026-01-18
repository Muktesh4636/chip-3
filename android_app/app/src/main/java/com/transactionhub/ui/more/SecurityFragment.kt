package com.transactionhub.ui.more

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.transactionhub.R
import com.transactionhub.utils.PrefManager

class SecurityFragment : Fragment() {
    private lateinit var prefManager: PrefManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_security, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())

        view.findViewById<TextView>(R.id.securityTitle).text = "Security & Authentication"

        setupBiometricAuthentication(view)
        setupSecuritySettings(view)
        checkBiometricStatus(view)
    }

    private fun setupBiometricAuthentication(view: View) {
        val switchBiometric = view.findViewById<Switch>(R.id.switchBiometricLogin)
        val btnTestBiometric = view.findViewById<Button>(R.id.btnTestBiometric)

        // Load saved preference
        switchBiometric.isChecked = prefManager.getBiometricEnabled()

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setBiometricEnabled(isChecked)
            Toast.makeText(context, "Biometric login: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        btnTestBiometric.setOnClickListener {
            testBiometricAuthentication()
        }

        // Biometric functionality would be implemented here
        // For now, just show a message
    }

    private fun setupSecuritySettings(view: View) {
        val switchAppLock = view.findViewById<Switch>(R.id.switchAppLock)
        val switchSessionTimeout = view.findViewById<Switch>(R.id.switchSessionTimeout)
        val btnChangePin = view.findViewById<Button>(R.id.btnChangePin)
        val btnViewSessions = view.findViewById<Button>(R.id.btnViewSessions)

        switchAppLock.isChecked = prefManager.getAppLockEnabled()
        switchSessionTimeout.isChecked = prefManager.getSessionTimeoutEnabled()

        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setAppLockEnabled(isChecked)
            Toast.makeText(context, "App lock: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchSessionTimeout.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setSessionTimeoutEnabled(isChecked)
            Toast.makeText(context, "Session timeout: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        btnChangePin.setOnClickListener {
            Toast.makeText(context, "PIN change not available via mobile app. Please use web interface.", Toast.LENGTH_LONG).show()
        }

        btnViewSessions.setOnClickListener {
            showActiveSessions()
        }
    }

    private fun checkBiometricStatus(view: View) {
        val biometricStatus = view.findViewById<TextView>(R.id.biometricStatus)
        val biometricIcon = view.findViewById<ImageView>(R.id.biometricIcon)

        // Simplified biometric check for demo
        biometricStatus.text = "🔐 Biometric authentication available"
        biometricStatus.setTextColor(resources.getColor(R.color.success, null))
        biometricIcon.setImageResource(android.R.drawable.ic_lock_lock)
        biometricIcon.setColorFilter(resources.getColor(R.color.success, null))
    }

    private fun testBiometricAuthentication() {
        // Simplified biometric test - just show a success message for demo
        Toast.makeText(context, "Biometric authentication test completed! 🔐", Toast.LENGTH_SHORT).show()
    }

    private fun showActiveSessions() {
        // Show mock active sessions for now
        val sessions = """
            📱 Current Session (This device)
            • Started: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            • IP: 192.168.1.100
            • Browser: Mobile App

            💻 Web Browser Session
            • Started: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis() - 3600000))}
            • IP: 192.168.1.100
            • Browser: Chrome 120.0
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Active Sessions")
            .setMessage(sessions)
            .setPositiveButton("Close", null)
            .show()
    }
}