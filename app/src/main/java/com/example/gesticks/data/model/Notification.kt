package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class Notification(
    val id: String,
    val tipo: String, // ticket_creado | ticket_asignado | ticket_resuelto
    val titulo: String,
    val mensaje: String,
    @SerializedName("receptor_id")
    val receiverId: Int,
    @SerializedName("emisor_id")
    val senderId: Int?,
    @SerializedName("ticket_id")
    val ticketId: Int?,
    val leida: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class NotificationsResponse(
    val data: List<Notification>,
    @SerializedName("no_leidas")
    val unreadCount: Int
)
