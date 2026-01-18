package com.transactionhub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefManager = PrefManager(this)
        
        // Check if already logged in
        if (prefManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_login)
        
        apiService = ApiClient.apiService
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        
        loginButton.setOnClickListener {
            performLogin()
        }
    }
    
    private fun performLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }
        
        loginButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val response = apiService.login(
                    com.transactionhub.data.models.LoginRequest(username, password)
                )
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()!!
                    prefManager.saveToken(loginResponse.token)
                    prefManager.saveUserId(loginResponse.user_id)
                    
                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                loginButton.isEnabled = true
            }
        }
    }
}
