package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class Comment(
    val id: Int,
    val comentario: String,
    @SerializedName("ticket_id")
    val ticketId: Int,
    @SerializedName("usuario_autor_id")
    val authorId: Int,
    @SerializedName("usuario_autor_nombre")
    val authorName: String,
    val fecha: String,
    @SerializedName("url_evidencia", alternate = ["evidencia", "evidencia_url", "archivo", "archivo_url", "archivo_path", "url", "file_url"])
    val evidenceUrl: String? = null,
    @SerializedName("archivos")
    val files: List<TicketFile> = emptyList()
)
