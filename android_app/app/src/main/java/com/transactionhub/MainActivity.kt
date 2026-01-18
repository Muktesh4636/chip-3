package com.transactionhub

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.transactionhub.ui.dashboard.DashboardFragment
import com.transactionhub.ui.clients.ClientsFragment
import com.transactionhub.ui.exchanges.ExchangesFragment
import com.transactionhub.ui.transactions.TransactionsFragment
import com.transactionhub.ui.payments.PendingPaymentsFragment
import com.transactionhub.ui.more.MoreFragment
import com.transactionhub.ui.more.ReportsFragment
import com.transactionhub.ui.more.UserProfileFragment
import com.transactionhub.utils.PrefManager

class MainActivity : AppCompatActivity() {
    private lateinit var prefManager: PrefManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefManager = PrefManager(this)
        
        // Check if logged in
        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        
        // Load default fragment
        loadFragment(DashboardFragment())
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_clients -> {
                    loadFragment(ClientsFragment())
                    true
                }
                R.id.nav_exchanges -> {
                    loadFragment(ExchangesFragment())
                    true
                }
                R.id.nav_reports -> {
                    loadFragment(ReportsFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(UserProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                prefManager.clear()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
