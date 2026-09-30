package com.example.gesticks.data.model

import com.google.gson.annotations.SerializedName

data class UpdatePasswordRequest(
    val current: String,
    @SerializedName("new")
    val newPassword: String
)

data class MessageResponse(
    val message: String
)
