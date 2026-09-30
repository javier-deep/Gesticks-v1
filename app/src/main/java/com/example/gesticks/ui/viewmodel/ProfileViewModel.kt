package com.example.gesticks.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TicketRepository()
    private val sessionManager = SessionManager(application)

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userPhone = MutableLiveData<String>()
    val userPhone: LiveData<String> = _userPhone

    private val _departmentName = MutableLiveData<String>()
    val departmentName: LiveData<String> = _departmentName

    private val _totalTickets = MutableLiveData(0)
    val totalTickets: LiveData<Int> = _totalTickets

    private val _resolvedTickets = MutableLiveData(0)
    val resolvedTickets: LiveData<Int> = _resolvedTickets

    private val _activeTickets = MutableLiveData(0)
    val activeTickets: LiveData<Int> = _activeTickets

    private val _passwordUpdateResult = MutableLiveData<Pair<Boolean, String>?>()
    val passwordUpdateResult: LiveData<Pair<Boolean, String>?> = _passwordUpdateResult

    fun changePassword(currentPassword: String, newPassword: String) {
        val token = sessionManager.fetchAuthToken() ?: return

        viewModelScope.launch {
            try {
                val response = repository.updatePassword(token, currentPassword, newPassword)
                if (response.isSuccessful) {
                    _passwordUpdateResult.value = true to (response.body()?.message ?: "Contraseña actualizada con éxito")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        com.google.gson.JsonParser.parseString(errorBody).asJsonObject.get("message")?.asString
                            ?: "No se pudo actualizar la contraseña"
                    } catch (e: Exception) {
                        "No se pudo actualizar la contraseña"
                    }
                    _passwordUpdateResult.value = false to message
                }
            } catch (e: Exception) {
                _passwordUpdateResult.value = false to "Error de conexión: ${e.message}"
            }
        }
    }

    fun loadUserData() {
        _userName.value = sessionManager.fetchUserName()
        _userEmail.value = sessionManager.fetchUserEmail()
        _userPhone.value = sessionManager.fetchUserPhone().ifBlank { "No registrado" }

        if (!com.example.gesticks.util.NetworkUtils.isNetworkAvailable(getApplication())) {
            _departmentName.value = "Sin departamento (Sin conexión)"
            return
        }

        val token = sessionManager.fetchAuthToken() ?: return
        val userId = sessionManager.fetchUserId()
        val departmentId = sessionManager.fetchUserDepartmentId()

        viewModelScope.launch {
            try {
                val response = repository.getDepartments()
                if (response.isSuccessful) {
                    val department = response.body()?.data.orEmpty().find { it.id == departmentId }
                    _departmentName.value = department?.name ?: "Sin departamento"
                }
            } catch (e: Exception) {
                _departmentName.value = "Sin departamento"
            }
        }

        viewModelScope.launch {
            try {
                val response = repository.getTickets(token)
                if (response.isSuccessful) {
                    val allTickets = response.body()?.data ?: emptyList()
                    val userTickets = allTickets.filter { it.authorId == userId || it.assignedToId == userId }
                    
                    _totalTickets.value = userTickets.size
                    _resolvedTickets.value = userTickets.count { it.status.lowercase() == "resuelto" }
                    _activeTickets.value = userTickets.count { it.status.lowercase() != "resuelto" }
                }
            } catch (e: Exception) {
                // Error
            }
        }
    }
}
