package com.hackathon.temantidur.data.emotion.model

data class EmotionResult(
    val emotion: String,
    val confidence: Float,
    val description: String,
    val recommendations: List<String> = emptyList(),
    val imageFile: String? = null
)