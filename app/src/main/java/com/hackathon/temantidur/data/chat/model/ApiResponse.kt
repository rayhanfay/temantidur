package com.hackathon.temantidur.data.chat.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("message")
    val message: String?,

    @SerializedName("status")
    val status: String?
)