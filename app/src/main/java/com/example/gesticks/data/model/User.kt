package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id")
    val objectId: String? = null,
    val id: Int,
    @SerializedName("nombre")
    val name: String,
    @SerializedName("correo")
    val email: String,
    @SerializedName("telefono")
    val phone: String? = null,
    @SerializedName("estatus")
    val status: Int,
    @SerializedName("departamento_id")
    val departmentId: Int,
    @SerializedName("permisos")
    val permissions: List<Int>,
    @SerializedName("must_change_password")
    val mustChangePassword: Boolean = false,
    @SerializedName("categorias_asignables")
    val assignableCategories: List<Int>? = null
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("token_type")
    val tokenType: String?,
    val user: User?
)

data class DataWrapper<T>(
    val data: T
)

data class MeResponse(
    val success: Boolean,
    val user: User
)
