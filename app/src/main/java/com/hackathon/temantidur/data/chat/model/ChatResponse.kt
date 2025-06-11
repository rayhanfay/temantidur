package com.hackathon.temantidur.data.chat.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("reply")
    val reply: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("error")
    val error: String?
)