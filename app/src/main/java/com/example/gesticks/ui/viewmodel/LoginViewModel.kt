package com.example.gesticks.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gesticks.data.model.LoginResponse
import com.example.gesticks.data.repository.TicketRepository
import kotlinx.coroutines.launch
import retrofit2.Response

class LoginViewModel : ViewModel() {
    private val repository = TicketRepository()

    private val _loginResponse = MutableLiveData<Response<LoginResponse>>()
    val loginResponse: LiveData<Response<LoginResponse>> = _loginResponse

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun login(email: String, password: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    _loginResponse.value = response
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    
                    // Intentar extraer el mensaje del JSON del servidor
                    val errorMessage = try {
                        val json = com.google.gson.JsonParser.parseString(rawError).asJsonObject
                        json.get("message")?.asString ?: json.get("error")?.asString ?: "Error desconocido"
                    } catch (e: Exception) {
                        "Credenciales incorrectas"
                    }
                    
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
