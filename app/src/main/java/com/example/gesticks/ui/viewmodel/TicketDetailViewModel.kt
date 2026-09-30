package com.example.gesticks.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gesticks.data.model.Comment
import com.example.gesticks.data.model.DataWrapper
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import kotlinx.coroutines.launch
import retrofit2.Response

class TicketDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TicketRepository()
    private val sessionManager = SessionManager(application)

    val currentUserId: Int get() = sessionManager.fetchUserId()

    private val _ticket = MutableLiveData<Ticket?>(null)
    val ticket: LiveData<Ticket?> = _ticket

    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPostingComment = MutableLiveData(false)
    val isPostingComment: LiveData<Boolean> = _isPostingComment

    private val _actionInProgress = MutableLiveData(false)
    val actionInProgress: LiveData<Boolean> = _actionInProgress

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _users = MutableLiveData<List<com.example.gesticks.data.model.User>>(emptyList())
    val users: LiveData<List<com.example.gesticks.data.model.User>> = _users

    fun loadUsers() {
        val token = sessionManager.fetchAuthToken() ?: return
        viewModelScope.launch {
            try {
                val response = repository.getUsers(token)
                if (response.isSuccessful) {
                    _users.value = response.body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                // Si falla cargar usuarios
            }
        }
    }

    fun assignTicketToUser(ticketId: Int, targetUserId: Int?) {
        val token = sessionManager.fetchAuthToken() ?: return
        if (_actionInProgress.value == true) return

        _actionInProgress.value = true
        viewModelScope.launch {
            try {
                val response = repository.assignTicket(token, ticketId, targetUserId)
                if (response.isSuccessful) {
                    loadTicket(ticketId)
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    val message = try {
                        val json = com.google.gson.JsonParser.parseString(rawError).asJsonObject
                        json.get("message")?.asString 
                            ?: json.get("error")?.asString 
                            ?: json.get("msg")?.asString
                            ?: "No se pudo reasignar el ticket (${response.code()})"
                    } catch (e: Exception) {
                        "No se pudo reasignar el ticket (${response.code()})"
                    }
                    _errorMessage.value = message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _actionInProgress.value = false
            }
        }
    }

    fun loadTicket(ticketId: Int) {
        if (!com.example.gesticks.util.NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "Sin conexión a internet. Revisa tu red."
            return
        }

        val token = sessionManager.fetchAuthToken() ?: return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getTicket(token, ticketId)
                if (response.isSuccessful) {
                    _ticket.value = response.body()?.data
                } else {
                    _errorMessage.value = "No se pudo cargar el ticket"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        loadComments(ticketId)
    }

    fun loadComments(ticketId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getComments(ticketId)
                if (response.isSuccessful) {
                    _comments.value = response.body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                // Se reintenta la próxima vez que se abra el ticket
            }
        }
    }

    fun submitComment(ticketId: Int, text: String, attachment: java.io.File? = null) {
        if (text.isBlank() || _isPostingComment.value == true) return
        val userId = sessionManager.fetchUserId()

        _isPostingComment.value = true
        viewModelScope.launch {
            try {
                val response = repository.createComment(ticketId, text.trim(), userId, attachment)
                if (response.isSuccessful) {
                    loadComments(ticketId)
                } else {
                    _errorMessage.value = "No se pudo enviar el comentario"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isPostingComment.value = false
            }
        }
    }

    fun markInProgress(ticketId: Int) {
        performStatusAction(ticketId) { token -> repository.markTicketInProgress(token, ticketId) }
    }

    fun resolve(ticketId: Int) {
        performStatusAction(ticketId) { token -> repository.resolveTicket(token, ticketId) }
    }

    fun reopen(ticketId: Int) {
        performStatusAction(ticketId) { token -> repository.reopenTicket(token, ticketId) }
    }

    private fun performStatusAction(ticketId: Int, action: suspend (String) -> Response<DataWrapper<Ticket>>) {
        val token = sessionManager.fetchAuthToken() ?: return
        if (_actionInProgress.value == true) return

        _actionInProgress.value = true
        viewModelScope.launch {
            try {
                val response = action(token)
                if (response.isSuccessful) {
                    loadTicket(ticketId)
                } else {
                    _errorMessage.value = "No se pudo actualizar el ticket"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _actionInProgress.value = false
            }
        }
    }
}
