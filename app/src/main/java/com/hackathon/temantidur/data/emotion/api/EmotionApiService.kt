package com.hackathon.temantidur.data.emotion.api

import com.hackathon.temantidur.data.emotion.model.EmotionResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface EmotionApiService {
    @Multipart
    @POST("detect-emotion")
    suspend fun detectEmotion(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<EmotionResponse>
}