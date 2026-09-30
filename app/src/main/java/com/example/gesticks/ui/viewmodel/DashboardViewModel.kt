package com.example.gesticks.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gesticks.data.model.Notification
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import com.example.gesticks.wear.WearSyncManager
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val NOTIFICATIONS_PREVIEW_SIZE = 3
    }

    private val repository = TicketRepository()
    private val sessionManager = SessionManager(application)
    private val wearSyncManager = WearSyncManager(application)

    private val _countOpen = MutableLiveData(0)
    val countOpen: LiveData<Int> = _countOpen

    private val _countProcess = MutableLiveData(0)
    val countProcess: LiveData<Int> = _countProcess

    private val _countResolved = MutableLiveData(0)
    val countResolved: LiveData<Int> = _countResolved

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private var allNotifications: List<Notification> = emptyList()

    private val _notifications = MutableLiveData<List<Notification>>(emptyList())
    val notifications: LiveData<List<Notification>> = _notifications

    private val _isShowingAllNotifications = MutableLiveData(false)
    val isShowingAllNotifications: LiveData<Boolean> = _isShowingAllNotifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun onLiveNotificationReceived(notification: Notification) {
        if (allNotifications.any { it.id == notification.id }) return

        allNotifications = listOf(notification) + allNotifications
        _notifications.value = if (_isShowingAllNotifications.value == true) {
            allNotifications
        } else {
            allNotifications.take(NOTIFICATIONS_PREVIEW_SIZE)
        }
        _unreadCount.value = (_unreadCount.value ?: 0) + 1
    }

    fun toggleShowAllNotifications() {
        val showAll = !(_isShowingAllNotifications.value ?: false)
        _isShowingAllNotifications.value = showAll
        _notifications.value = if (showAll) allNotifications else allNotifications.take(NOTIFICATIONS_PREVIEW_SIZE)
    }

    fun markNotificationsRead() {
        val token = sessionManager.fetchAuthToken() ?: return
        if ((_unreadCount.value ?: 0) == 0) return

        viewModelScope.launch {
            try {
                val response = repository.markAllNotificationsRead(token)
                if (response.isSuccessful) {
                    _unreadCount.value = 0
                }
            } catch (e: Exception) {
                // Se reintentará en la próxima carga del dashboard
            }
        }
    }

    private fun loadNotifications(token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getNotifications(token)
                if (response.isSuccessful) {
                    val body = response.body()
                    allNotifications = body?.data.orEmpty()
                    _isShowingAllNotifications.value = false
                    _notifications.value = allNotifications.take(NOTIFICATIONS_PREVIEW_SIZE)
                    _unreadCount.value = body?.unreadCount ?: 0
                }
            } catch (e: Exception) {
                // Error de red: se mantienen las notificaciones previamente cargadas
            }
        }
    }

    fun loadData() {
        _userName.value = sessionManager.fetchUserName().split(" ")[0] // Solo el primer nombre

        if (!com.example.gesticks.util.NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "Sin conexión a internet. Revisa tu red."
            return
        }

        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrBlank()) {
            _errorMessage.value = "Sesión no válida o caducada"
            return
        }
        val userId = sessionManager.fetchUserId()

        loadNotifications(token)

        viewModelScope.launch {
            try {
                val response = repository.getTickets(token)
                if (response.isSuccessful) {
                    val allTickets = response.body()?.data ?: emptyList()
                    val userTickets = allTickets.filter { it.authorId == userId || it.assignedToId == userId }

                    // El reloj solo debe mostrar lo que el usuario tiene que atender: sus tickets asignados
                    wearSyncManager.syncTickets(allTickets.filter { it.assignedToId == userId })
                    
                    _countOpen.value = userTickets.count { it.status.lowercase() == "abierto" }
                    _countProcess.value = userTickets.count { it.status.lowercase() == "pendiente" }
                    _countResolved.value = userTickets.count { it.status.lowercase() == "resuelto" }
                } else {
                    if (response.code() == 401 || response.code() == 403) {
                        _errorMessage.value = "Sesión caducada. Por favor vuelve a iniciar sesión."
                    } else {
                        _errorMessage.value = "Error al obtener datos del servidor (${response.code()})"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión al cargar el resumen"
            }
        }
    }
}
