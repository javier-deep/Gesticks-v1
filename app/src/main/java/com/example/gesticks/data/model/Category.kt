package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    val id: Int,
    @SerializedName("nombre")
    val name: String,
    @SerializedName("departamento_id")
    val departmentId: Int,
    val estatus: Int
)
