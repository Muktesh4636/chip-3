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

    fun clear() {
        clearAll()
    }
    
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
