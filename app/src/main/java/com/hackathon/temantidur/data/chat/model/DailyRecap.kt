package com.hackathon.temantidur.data.chat.model

import java.util.UUID

data class DailyRecap(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val dayLabel: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)