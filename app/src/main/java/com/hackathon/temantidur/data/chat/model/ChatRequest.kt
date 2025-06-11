package com.hackathon.temantidur.data.chat.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("messages")
    val messages: List<Message>,

    @SerializedName("max_tokens")
    val max_tokens: Int? = null,

    @SerializedName("temperature")
    val temperature: Double? = null,

    @SerializedName("top_p")
    val top_p: Double? = null
)