package com.example.gesticks.ui.view

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.gesticks.R
import com.example.gesticks.data.network.NotificationSocketManager
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupNavigation()

        // Notificaciones en tiempo real (igual que la web), vive mientras dure la sesión
        val userId = SessionManager(this).fetchUserId()
        if (userId > 0) {
            NotificationSocketManager.connect(userId)
        }

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            NotificationSocketManager.disconnect()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // No padding bottom for Nav
            insets
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Haptic feedback like in React Native
            binding.bottomNavigation.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_tickets -> {
                    loadFragment(TicketsFragment())
                    true
                }
                R.id.nav_create -> {
                    loadFragment(CreateTicketFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
