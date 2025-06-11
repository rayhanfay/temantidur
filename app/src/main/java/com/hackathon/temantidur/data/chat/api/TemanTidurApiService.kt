// app/src/main/java/com/hackathon/temantidur/data/chat/api/TemanTidurApiService.kt
package com.hackathon.temantidur.data.chat.api

import com.hackathon.temantidur.data.chat.model.ChatRequest
import com.hackathon.temantidur.data.chat.model.ChatResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TemanTidurApiService {

    @GET("/")
    @Headers("Content-Type: application/json")
    suspend fun getRoot(): Response<ChatResponse>

    @POST("/chat")
    @Headers("Content-Type: application/json")
    suspend fun sendChat(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @Multipart
    @POST("voice-chat")
    suspend fun sendVoiceChat(
        @Header("Authorization") authorization: String,
        @Part audio: MultipartBody.Part
    ): Response<ResponseBody>
}