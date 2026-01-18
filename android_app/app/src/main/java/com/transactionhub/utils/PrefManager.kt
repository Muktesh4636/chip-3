package com.transactionhub.utils

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("TransactionHubPrefs", Context.MODE_PRIVATE)
    
    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }
    
    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }
    
    fun saveUserId(userId: Int) {
        prefs.edit().putInt("user_id", userId).apply()
    }
    
    fun getUserId(): Int {
        return prefs.getInt("user_id", -1)
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun setOfflineModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("offline_mode_enabled", enabled).apply()
    }

    fun getOfflineModeEnabled(): Boolean {
        return prefs.getBoolean("offline_mode_enabled", false)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun getAutoSyncEnabled(): Boolean {
        return prefs.getBoolean("auto_sync_enabled", true)
    }

    fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong("last_sync_time", timestamp).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_time", 0)
    }

    fun getOfflineTransactionCount(): Int {
        return prefs.getInt("offline_transaction_count", 0)
    }

    fun setOfflineTransactionCount(count: Int) {
        prefs.edit().putInt("offline_transaction_count", count).apply()
    }

    fun getPendingUploadsCount(): Int {
        return prefs.getInt("pending_uploads_count", 0)
    }

    fun setPendingUploadsCount(count: Int) {
        prefs.edit().putInt("pending_uploads_count", count).apply()
    }

    fun clearOfflineData() {
        prefs.edit()
            .remove("offline_transaction_count")
            .remove("pending_uploads_count")
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun getBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
    }

    fun getAppLockEnabled(): Boolean {
        return prefs.getBoolean("app_lock_enabled", false)
    }

    fun setSessionTimeoutEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("session_timeout_enabled", enabled).apply()
    }

    fun getSessionTimeoutEnabled(): Boolean {
        return prefs.getBoolean("session_timeout_enabled", true)
    }

    fun clear() {
        clearAll()
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
