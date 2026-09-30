package com.example.gesticks.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gesticks.R
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import com.example.gesticks.databinding.ActivityLoginBinding
import com.example.gesticks.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val repository = TicketRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        val sessionManager = SessionManager(this)
        val existingToken = sessionManager.fetchAuthToken()
        if (existingToken != null) {
            checkExistingSession(existingToken, sessionManager)
        } else {
            initLoginScreen()
        }
    }

    private fun initLoginScreen() {
        setupAnimations()
        setupListeners()
        observeViewModel()
    }

    private fun checkExistingSession(token: String, sessionManager: SessionManager) {
        // Ocultamos el formulario mientras confirmamos que la sesión guardada sigue siendo válida
        binding.header.visibility = View.INVISIBLE
        binding.form.visibility = View.INVISIBLE

        lifecycleScope.launch {
            try {
                val response = repository.getMe(token)
                if (response.isSuccessful) {
                    response.body()?.user?.let { user ->
                        sessionManager.saveUserName(user.name)
                        sessionManager.saveUserEmail(user.email)
                        sessionManager.saveUserDepartmentId(user.departmentId)
                        sessionManager.saveUserPhone(user.phone)
                    }
                    navigateToMain()
                } else {
                    // Token inválido o expirado: hay que iniciar sesión de nuevo
                    sessionManager.clearSession()
                    binding.header.visibility = View.VISIBLE
                    binding.form.visibility = View.VISIBLE
                    initLoginScreen()
                }
            } catch (e: Exception) {
                // Sin conexión: dejamos entrar con la sesión y datos guardados localmente
                navigateToMain()
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupAnimations() {
        binding.header.alpha = 0f
        binding.header.translationY = 50f
        binding.form.translationY = 100f

        binding.header.animate().alpha(1f).translationY(0f).setDuration(800).start()
        binding.form.animate().translationY(0f).setDuration(1000).setStartDelay(300).start()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                viewModel.login(email, password)
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun observeViewModel() {
        viewModel.loginResponse.observe(this) { response ->
            if (response != null && response.isSuccessful) {
                val loginData = response.body()
                if (loginData?.accessToken != null && loginData.user != null) {
                    val sessionManager = com.example.gesticks.data.network.SessionManager(this)
                    sessionManager.saveAuthToken(loginData.accessToken)
                    sessionManager.saveUserId(loginData.user.id)
                    sessionManager.saveUserName(loginData.user.name)
                    sessionManager.saveUserEmail(loginData.user.email)
                    sessionManager.saveUserDepartmentId(loginData.user.departmentId)
                    sessionManager.saveUserPhone(loginData.user.phone)
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Error: Respuesta del servidor incompleta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
