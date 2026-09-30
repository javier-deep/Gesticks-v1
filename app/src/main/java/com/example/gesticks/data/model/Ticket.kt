package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class Ticket(
    @SerializedName("_id")
    val objectId: String? = null,
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("titulo")
    val title: String,
    @SerializedName("descripcion")
    val description: String,
    @SerializedName("prioridad")
    val priority: String, // baja|media|alta|critica
    @SerializedName("fecha_creacion")
    val date: String,
    @SerializedName("fecha_asignacion")
    val assignedDate: String? = null,
    @SerializedName("fecha_resolucion")
    val resolutionDate: String? = null,
    @SerializedName("usuario_autor_id")
    val authorId: Int,
    @SerializedName("categoria_id")
    val categoryId: Int,
    @SerializedName("departamento_id")
    val departmentId: Int? = null,
    @SerializedName("asignado_a_id")
    val assignedToId: Int? = null,
    @SerializedName("estado")
    val status: String = "abierto", // abierto|pendiente|resuelto
    @SerializedName("archivo_path", alternate = ["archivo", "archivo_url", "url", "evidencia", "evidencia_url", "file_url"])
    val filePath: String? = null,
    @SerializedName("archivos")
    val files: List<TicketFile> = emptyList()
)

data class TicketFile(
    val id: Int? = null,
    @SerializedName("nombre_original")
    val originalName: String? = null,
    val url: String? = null
)
