package com.example.gesticks.data.repository

import com.example.gesticks.data.model.Category
import com.example.gesticks.data.model.Comment
import com.example.gesticks.data.model.DataWrapper
import com.example.gesticks.data.model.Department
import com.example.gesticks.data.model.LoginRequest
import com.example.gesticks.data.model.LoginResponse
import com.example.gesticks.data.model.MeResponse
import com.example.gesticks.data.model.MessageResponse
import com.example.gesticks.data.model.NotificationsResponse
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.data.model.UpdatePasswordRequest
import com.example.gesticks.data.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

class TicketRepository {
    private val api = RetrofitInstance.api

    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return api.login(LoginRequest(email, password))
    }

    suspend fun getMe(token: String): Response<MeResponse> {
        return api.getMe("Bearer $token")
    }

    suspend fun getTickets(token: String): Response<DataWrapper<List<Ticket>>> {
        return api.getTickets("Bearer $token")
    }

    suspend fun getTicket(token: String, id: Int): Response<DataWrapper<Ticket>> {
        return api.getTicket("Bearer $token", id)
    }

    suspend fun createTicket(
        token: String,
        titulo: String,
        descripcion: String,
        prioridad: String,
        categoriaId: Int,
        departamentoId: Int,
        archivo: File? = null
    ): Response<DataWrapper<Ticket>> {
        val titleRB = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
        val descRB = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
        val priorityRB = prioridad.toRequestBody("text/plain".toMediaTypeOrNull())
        val categoryIdRB = categoriaId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val departmentIdRB = departamentoId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val bodyArchivo = archivo?.let {
            val requestFile = it.asRequestBody("*/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("archivo", it.name, requestFile)
        }

        return api.createTicket(
            "Bearer $token",
            titleRB,
            descRB,
            priorityRB,
            categoryIdRB,
            departmentIdRB,
            bodyArchivo
        )
    }

    suspend fun resolveTicket(token: String, id: Int): Response<DataWrapper<Ticket>> {
        return api.resolveTicket("Bearer $token", id)
    }

    suspend fun markTicketInProgress(token: String, id: Int): Response<DataWrapper<Ticket>> {
        val nowIso = java.time.Instant.now().toString()
        return api.markTicketInProgress("Bearer $token", id, mapOf("fecha_asignacion" to nowIso))
    }

    suspend fun getUsers(token: String): Response<DataWrapper<List<com.example.gesticks.data.model.User>>> {
        return api.getUsers("Bearer $token")
    }

    suspend fun assignTicket(token: String, id: Int, assignedToUserId: Int?): Response<DataWrapper<Ticket>> {
        val nowIso = java.time.Instant.now().toString()
        return if (assignedToUserId != null) {
            val body = mapOf(
                "asignado_a_id" to assignedToUserId,
                "fecha_asignacion" to nowIso
            )
            api.assignTicket("Bearer $token", id, body)
        } else {
            val json = """{"asignado_a_id":null,"fecha_asignacion":null}"""
                .toRequestBody("application/json".toMediaTypeOrNull())
            api.reopenTicket("Bearer $token", id, json)
        }
    }

    suspend fun reopenTicket(token: String, id: Int): Response<DataWrapper<Ticket>> {
        val body = """{"fecha_asignacion":null,"fecha_resolucion":null}"""
            .toRequestBody("application/json".toMediaTypeOrNull())
        return api.reopenTicket("Bearer $token", id, body)
    }

    suspend fun deleteTicket(token: String, id: Int): Response<Void> {
        return api.deleteTicket("Bearer $token", id)
    }

    suspend fun getCategories(): Response<DataWrapper<List<Category>>> {
        return api.getCategories()
    }

    suspend fun getDepartments(): Response<DataWrapper<List<Department>>> {
        return api.getDepartments()
    }

    suspend fun getNotifications(token: String): Response<NotificationsResponse> {
        return api.getNotifications("Bearer $token")
    }

    suspend fun markAllNotificationsRead(token: String): Response<Void> {
        return api.markAllNotificationsRead("Bearer $token")
    }

    suspend fun updatePassword(token: String, current: String, newPassword: String): Response<MessageResponse> {
        return api.updatePassword("Bearer $token", UpdatePasswordRequest(current, newPassword))
    }

    suspend fun getComments(ticketId: Int): Response<DataWrapper<List<Comment>>> {
        return api.getComments(ticketId)
    }

    suspend fun createComment(ticketId: Int, text: String, authorId: Int, archivo: File? = null): Response<DataWrapper<Comment>> {
        val ticketIdRB = ticketId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val textRB = text.toRequestBody("text/plain".toMediaTypeOrNull())
        val authorIdRB = authorId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val bodyArchivo = archivo?.let {
            val requestFile = it.asRequestBody("*/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("evidencia", it.name, requestFile)
        }

        return api.createComment(ticketIdRB, textRB, authorIdRB, bodyArchivo)
    }
}
