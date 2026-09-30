package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class Department(
    val id: Int,
    @SerializedName("nombre")
    val name: String,
    val estatus: Int
)
