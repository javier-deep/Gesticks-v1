package com.example.gesticks.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gesticks.data.model.Category
import com.example.gesticks.data.model.Department
import com.example.gesticks.data.network.SessionManager
import com.example.gesticks.data.repository.TicketRepository
import kotlinx.coroutines.launch

class CreateTicketViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TicketRepository()
    private val sessionManager = SessionManager(application)

    private var allCategories: List<Category> = emptyList()

    private val _priority = MutableLiveData("media")
    val priority: LiveData<String> = _priority

    private val _departments = MutableLiveData<List<Department>>(emptyList())
    val departments: LiveData<List<Department>> = _departments

    private val _selectedDepartmentId = MutableLiveData<Int?>(null)
    val selectedDepartmentId: LiveData<Int?> = _selectedDepartmentId

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private val _selectedCategoryId = MutableLiveData<Int?>(null)
    val selectedCategoryId: LiveData<Int?> = _selectedCategoryId

    private val _createResult = MutableLiveData<Boolean>()
    val createResult: LiveData<Boolean> = _createResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun setPriority(newPriority: String) {
        _priority.value = newPriority.lowercase()
    }

    // Igual que en la web: primero se elige el departamento y con eso se filtran las categorías
    fun setDepartment(departmentId: Int) {
        _selectedDepartmentId.value = departmentId
        val filtered = allCategories.filter { it.departmentId == departmentId }
        _categories.value = filtered
        _selectedCategoryId.value = filtered.firstOrNull()?.id
    }

    fun setCategory(categoryId: Int) {
        _selectedCategoryId.value = categoryId
    }

    fun loadFormOptions() {
        viewModelScope.launch {
            try {
                val response = repository.getDepartments()
                if (response.isSuccessful) {
                    _departments.value = response.body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                _errorMessage.value = "No se pudieron cargar los departamentos: ${e.message}"
            }
        }

        viewModelScope.launch {
            try {
                val response = repository.getCategories()
                if (response.isSuccessful) {
                    allCategories = response.body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                _errorMessage.value = "No se pudieron cargar las categorías: ${e.message}"
            }
        }
    }

    fun createTicket(title: String, description: String, archivo: java.io.File? = null) {
        if (_isLoading.value == true) return // Evitar múltiples peticiones simultáneas

        if (!com.example.gesticks.util.NetworkUtils.isNetworkAvailable(getApplication())) {
            _errorMessage.value = "Sin conexión a internet. Revisa tu red para enviar el ticket."
            return
        }

        val token = sessionManager.fetchAuthToken() ?: return
        val departmentId = _selectedDepartmentId.value
        val categoryId = _selectedCategoryId.value

        if (departmentId == null) {
            _errorMessage.value = "Selecciona un departamento para el ticket"
            return
        }
        if (categoryId == null) {
            _errorMessage.value = "Selecciona una categoría para el ticket"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val priority = _priority.value ?: "media"

        viewModelScope.launch {
            try {
                val response = repository.createTicket(
                    token = token,
                    titulo = title,
                    descripcion = description,
                    prioridad = priority,
                    categoriaId = categoryId,
                    departamentoId = departmentId,
                    archivo = archivo
                )
                if (response.isSuccessful) {
                    val createdTicket = response.body()?.data
                    val userId = sessionManager.fetchUserId()
                    if (createdTicket?.id != null && archivo != null && userId > 0) {
                        try {
                            repository.createComment(
                                ticketId = createdTicket.id,
                                text = "Archivo adjunto al ticket",
                                authorId = userId,
                                archivo = archivo
                            )
                        } catch (e: Exception) {
                            // Si falla la evidencia de comentario, el ticket ya fue registrado
                        }
                    }
                    _createResult.value = true
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    val message = try {
                        val json = com.google.gson.JsonParser.parseString(rawError).asJsonObject
                        json.get("message")?.asString 
                            ?: json.get("error")?.asString 
                            ?: json.get("msg")?.asString 
                            ?: if (rawError.isNotBlank()) rawError else "Error del servidor (${response.code()})"
                    } catch (e: Exception) {
                        if (rawError.isNotBlank()) rawError else "Error al crear ticket (${response.code()})"
                    }
                    _errorMessage.value = message
                    _createResult.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}. El ticket no se creó, intenta de nuevo."
                _createResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
