package com.hackathon.temantidur.data.chat.model

import com.google.gson.annotations.SerializedName

data class RecapRequest(
    @SerializedName("date")
    val date: String,

    @SerializedName("messages")
    val messages: List<Message>
)