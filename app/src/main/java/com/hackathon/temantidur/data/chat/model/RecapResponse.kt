package com.hackathon.temantidur.data.chat.model

import com.google.gson.annotations.SerializedName

data class RecapResponse(
    @SerializedName("recap")
    val recap: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("formatted_date")
    val formattedDate: String,

    @SerializedName("language")
    val language: String,

    @SerializedName("message_count")
    val messageCount: Int
)