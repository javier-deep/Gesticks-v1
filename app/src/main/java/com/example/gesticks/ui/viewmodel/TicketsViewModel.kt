package com.example.gesticks.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import com.example.gesticks.util.NetworkUtils
import com.example.gesticks.wear.WearSyncManager
import kotlinx.coroutines.launch

class TicketsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TicketRepository()
    private val sessionManager = SessionManager(application)
    private val wearSyncManager = WearSyncManager(application)

    private var allUserTickets: List<Ticket> = emptyList()

    private val _createdTickets = MutableLiveData<List<Ticket>>(emptyList())
    val createdTickets: LiveData<List<Ticket>> = _createdTickets

    private val _assignedTickets = MutableLiveData<List<Ticket>>(emptyList())
    val assignedTickets: LiveData<List<Ticket>> = _assignedTickets

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _activeCount = MutableLiveData(0)
    val activeCount: LiveData<Int> = _activeCount

    private val _resolvedCount = MutableLiveData(0)
    val resolvedCount: LiveData<Int> = _resolvedCount

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Search and filter state
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _statusFilter = MutableLiveData("todos") // "todos", "abierto", "pendiente", "resuelto"
    val statusFilter: LiveData<String> = _statusFilter

    private val _priorityFilter = MutableLiveData("todas") // "todas", "baja", "media", "alta"
    val priorityFilter: LiveData<String> = _priorityFilter

    init {
        fetchTickets()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
        applyFilters()
    }

    fun setPriorityFilter(priority: String) {
        _priorityFilter.value = priority
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value?.trim().orEmpty().lowercase()
        val status = _statusFilter.value?.lowercase() ?: "todos"
        val priority = _priorityFilter.value?.lowercase() ?: "todas"
        val userId = sessionManager.fetchUserId()

        val filtered = allUserTickets.filter { ticket ->
            // Match Search query (title, description, or ticket ID)
            val matchesQuery = query.isEmpty() ||
                    ticket.title.lowercase().contains(query) ||
                    ticket.description.lowercase().contains(query) ||
                    ticket.id?.toString()?.contains(query) == true

            // Match Status filter
            val matchesStatus = when (status) {
                "todos" -> true
                "abierto" -> ticket.status.lowercase() == "abierto"
                "pendiente", "en progreso", "proceso" -> ticket.status.lowercase() == "pendiente"
                "resuelto" -> ticket.status.lowercase() == "resuelto"
                else -> true
            }

            // Match Priority filter
            val matchesPriority = when (priority) {
                "todas" -> true
                else -> ticket.priority.lowercase() == priority
            }

            matchesQuery && matchesStatus && matchesPriority
        }

        _createdTickets.value = filtered.filter { it.authorId == userId }
        _assignedTickets.value = filtered.filter { it.assignedToId == userId && it.authorId != userId }
    }

    fun fetchTickets() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "Sin conexión a internet. Revisa tu red."
            _isLoading.value = false
            return
        }

        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrBlank()) {
            _errorMessage.value = "Sesión no válida o caducada. Por favor vuelve a iniciar sesión."
            return
        }

        val userId = sessionManager.fetchUserId()
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val response = repository.getTickets(token)
                if (response.isSuccessful) {
                    val rawTickets = response.body()?.data ?: emptyList()
                    allUserTickets = rawTickets.filter { it.authorId == userId || it.assignedToId == userId }

                    // El reloj solo debe mostrar lo que el usuario tiene que atender: sus tickets asignados
                    wearSyncManager.syncTickets(rawTickets.filter { it.assignedToId == userId })
                    
                    _activeCount.value = allUserTickets.count { it.status.lowercase() != "resuelto" }
                    _resolvedCount.value = allUserTickets.count { it.status.lowercase() == "resuelto" }

                    applyFilters()
                } else {
                    val code = response.code()
                    if (code == 401 || code == 403) {
                        _errorMessage.value = "Tu sesión ha caducado. Vuelve a iniciar sesión."
                    } else {
                        _errorMessage.value = "Error al obtener tickets del servidor ($code)"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión con el servidor. Verifica tu internet."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
