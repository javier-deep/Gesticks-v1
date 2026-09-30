package com.example.gesticks.data.network

import com.example.gesticks.data.model.Category
import com.example.gesticks.data.model.Comment
import com.example.gesticks.data.model.DataWrapper
import com.example.gesticks.data.model.Department
import com.example.gesticks.data.model.LoginRequest
import com.example.gesticks.data.model.LoginResponse
import com.example.gesticks.data.model.MessageResponse
import com.example.gesticks.data.model.NotificationsResponse
import com.example.gesticks.data.model.Ticket
import com.example.gesticks.data.model.UpdatePasswordRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface GestixApiService {
    
    // Autenticación
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RequestBody): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<com.example.gesticks.data.model.MeResponse>

    // Tickets
    @GET("tickets")
    suspend fun getTickets(
        @Header("Authorization") token: String
    ): Response<DataWrapper<List<Ticket>>>

    @GET("tickets/{id}")
    suspend fun getTicket(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<DataWrapper<Ticket>>

    @Multipart
    @POST("tickets")
    suspend fun createTicket(
        @Header("Authorization") token: String,
        @Part("titulo") title: RequestBody,
        @Part("descripcion") description: RequestBody,
        @Part("prioridad") priority: RequestBody,
        @Part("categoria_id") categoryId: RequestBody,
        @Part("departamento_id") departmentId: RequestBody,
        @Part archivo: MultipartBody.Part? = null
    ): Response<DataWrapper<Ticket>>

    @PUT("tickets/{id}")
    suspend fun updateTicket(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body ticket: Ticket
    ): Response<DataWrapper<Ticket>>

    @POST("tickets/{id}/resolve")
    suspend fun resolveTicket(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<DataWrapper<Ticket>>

    @PUT("tickets/{id}")
    suspend fun markTicketInProgress(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<DataWrapper<Ticket>>

    // Reabrir: limpia fecha_asignacion y fecha_resolucion para que vuelva a 'abierto'.
    // Se usa un RequestBody crudo porque Gson omite los valores null por defecto.
    @PUT("tickets/{id}")
    suspend fun reopenTicket(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: RequestBody
    ): Response<DataWrapper<Ticket>>

    @DELETE("tickets/{id}")
    suspend fun deleteTicket(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Void>

    // Comentarios
    @GET("tickets/{id}/comentarios")
    suspend fun getComments(@Path("id") ticketId: Int): Response<DataWrapper<List<Comment>>>

    @Multipart
    @POST("comentarios")
    suspend fun createComment(
        @Part("ticket_id") ticketId: RequestBody,
        @Part("comentario") comentario: RequestBody,
        @Part("usuario_autor_id") authorId: RequestBody,
        @Part evidencia: MultipartBody.Part? = null
    ): Response<DataWrapper<Comment>>

    // Categorías
    @GET("categorias")
    suspend fun getCategories(): Response<DataWrapper<List<Category>>>

    // Departamentos
    @GET("departamentos")
    suspend fun getDepartments(): Response<DataWrapper<List<Department>>>

    // Notificaciones
    @GET("notificaciones")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationsResponse>

    @PUT("notificaciones/leer-todas")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") token: String
    ): Response<Void>

    // Usuarios
    @GET("usuarios")
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): Response<DataWrapper<List<com.example.gesticks.data.model.User>>>

    @PUT("tickets/{id}")
    suspend fun assignTicket(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<DataWrapper<Ticket>>

    // Perfil
    @POST("auth/user/update-password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<MessageResponse>
}
