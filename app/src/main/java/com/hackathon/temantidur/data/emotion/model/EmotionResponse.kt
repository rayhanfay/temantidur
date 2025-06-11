package com.hackathon.temantidur.data.emotion.model

data class EmotionResponse(
    val emotion: String,
    val confidence: Float,
    val recommendation: String? = null,
    val message: String? = null
)