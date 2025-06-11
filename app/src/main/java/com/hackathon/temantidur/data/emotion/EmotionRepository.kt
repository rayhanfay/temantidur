package com.hackathon.temantidur.data.emotion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import com.hackathon.temantidur.R
import com.hackathon.temantidur.common.ApiResult
import com.hackathon.temantidur.data.auth.AuthManager
import com.hackathon.temantidur.data.emotion.api.EmotionApiService
import com.hackathon.temantidur.data.emotion.model.EmotionResult
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class EmotionRepository(private val context: Context) {
    private val api: EmotionApiService
    private val authManager = AuthManager()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastAnalysisResult: EmotionResult? = null
    private var lastAnalysisTimestamp: Long = 0
    private val optimizedFiles = mutableListOf<File>()

    companion object {
        private const val BASE_URL = com.hackathon.temantidur.BuildConfig.BASE_URL
        private const val CONNECT_TIMEOUT = 45L
        private const val READ_TIMEOUT = 90L
        private const val WRITE_TIMEOUT = 45L
        private const val MAX_IMAGE_SIZE = 1024
        private const val COMPRESSION_QUALITY = 85
    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("EMOTION_HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(EmotionApiService::class.java)
    }

    suspend fun initializeAuth(): Boolean {
        return try {
            Log.d("EmotionRepo", "Initializing authentication...")
            val token = authManager.initializeToken()
            val success = token != null
            Log.d("EmotionRepo", "Auth initialization: ${if (success) "success" else "failed"}")
            success
        } catch (e: Exception) {
            Log.e("EmotionRepo", "Auth initialization error: ${e.message}", e)
            false
        }
    }

    suspend fun detectEmotion(imageFile: File, language: String): ApiResult<EmotionResult> {
        Log.d("EmotionRepo", "Starting emotion detection for: ${imageFile.name} with language: $language")
        clearAnalysisCache()

        return try {
            val optimizedFile = optimizeImageForEmotionDetection(imageFile)
            var result = performApiCall(optimizedFile, language)

            if (result is ApiResult.Error && result.code == 401) {
                Log.d("EmotionRepo", "Token expired, attempting to refresh and retry...")
                authManager.forceRefreshToken()?.let {
                    Log.d("EmotionRepo", "Token refreshed. Retrying API call.")
                    result = performApiCall(optimizedFile, language)
                } ?: run {
                    result = ApiResult.Error("Sesi berakhir. Silakan login ulang ya!", 401)
                }
            }

            if (result is ApiResult.Success) {
                lastAnalysisResult = result.data
                lastAnalysisTimestamp = System.currentTimeMillis()
            }

            result
        } catch (e: CancellationException) {
            Log.w("EmotionRepo", "Request cancelled by user.")
            ApiResult.Error("Permintaan dibatalkan")
        } catch (e: Exception) {
            Log.e("EmotionRepo", "An unexpected error occurred during detection flow: ${e.message}", e)
            ApiResult.Error("Terjadi kesalahan jaringan. Coba lagi ya!")
        } finally {
            cleanupOptimizedFiles()
        }
    }

    private suspend fun performApiCall(imageFile: File, language: String): ApiResult<EmotionResult> {
        val token = authManager.getValidToken() ?: return ApiResult.Error(context.getString(R.string.error_auth_failed), 401)

        if (!imageFile.exists() || imageFile.length() == 0L) {
            return ApiResult.Error(context.getString(R.string.error_invalid_image_file))
        }

        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
        val languagePart = MultipartBody.Part.createFormData("language", language)

        return try {
            val response = api.detectEmotion("Bearer $token", imagePart, languagePart)

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                Log.d("EmotionRepo", "API Response - emotion: ${responseBody.emotion}")
                Log.d("EmotionRepo", "API Response - confidence: ${responseBody.confidence}")
                Log.d("EmotionRepo", "API Response - recommendation: ${responseBody.recommendation}")
                Log.d("EmotionRepo", "API Response - message: ${responseBody.message}")

                val recommendations = parseRecommendationString(responseBody.recommendation)
                Log.d("EmotionRepo", "Parsed recommendations count: ${recommendations.size}")
                recommendations.forEachIndexed { index, rec ->
                    Log.d("EmotionRepo", "Recommendation $index: $rec")
                }

                val result = EmotionResult(
                    emotion = responseBody.emotion,
                    confidence = responseBody.confidence,
                    description = responseBody.message ?: "Aku mendeteksi emosi '${responseBody.emotion}' dari ekspresimu.",
                    recommendations = recommendations,
                    imageFile = imageFile.absolutePath
                )
                Log.d("EmotionRepo", "Created EmotionResult with ${result.recommendations.size} recommendations")
                ApiResult.Success(result)
            } else {
                val errorBody = response.errorBody()?.string()
                ApiResult.Error(parseEmotionError(response.code(), errorBody), response.code())
            }
        } catch (e: Exception) {
            Log.e("EmotionRepo", "API call failed: ${e.message}", e)
            ApiResult.Error("Gagal terhubung ke server. Periksa koneksi internetmu.")
        }
    }

    private fun parseRecommendationString(recommendation: String?): List<String> {
        if (recommendation.isNullOrBlank()) {
            Log.d("EmotionRepo", "Recommendation is null or blank")
            return emptyList()
        }
        val recommendations = mutableListOf<String>()
        val sentences = recommendation.split(Regex("[.!?]\\s*"))
            .filter { it.trim().isNotEmpty() }
            .map { it.trim() }

        if (sentences.size > 1) {
            sentences.forEach { sentence ->
                if (sentence.length > 10) {
                    recommendations.add(sentence)
                }
            }
        } else {
            recommendations.add(recommendation.trim())
        }
        Log.d("EmotionRepo", "Original recommendation: $recommendation")
        Log.d("EmotionRepo", "Parsed into ${recommendations.size} recommendations")
        return recommendations
    }

    private fun parseEmotionError(code: Int, errorBody: String?): String {
        return when (code) {
            400 -> context.getString(R.string.error_photo_not_analyzed)
            401 -> context.getString(R.string.error_session_ended)
            403 -> context.getString(R.string.error_access_denied)
            413 -> context.getString(R.string.error_photo_too_large)
            415 -> context.getString(R.string.error_photo_format_not_supported)
            429 -> context.getString(R.string.error_too_many_requests)
            500 -> context.getString(R.string.error_server_issue)
            502, 503, 504 -> context.getString(R.string.error_server_maintenance)
            else -> {
                if (!errorBody.isNullOrEmpty()) {
                    try {
                        val gson = Gson()
                        val errorMap = gson.fromJson(errorBody, Map::class.java)
                        val errorMessage = errorMap["message"] as? String
                        if (!errorMessage.isNullOrEmpty()) {
                            return when {
                                errorMessage.contains("face not detected", ignoreCase = true) -> context.getString(R.string.error_face_not_detected)
                                errorMessage.contains("multiple faces", ignoreCase = true) -> context.getString(R.string.error_multiple_faces_detected)
                                else -> "Server berpesan: $errorMessage 💬"
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("EmotionRepo", "Gagal mem-parsing body error JSON: $errorBody", e)
                    }
                }
                context.getString(R.string.error_generic, code)
            }
        }
    }

    private suspend fun optimizeImageForEmotionDetection(
        originalFile: File,
        forceNew: Boolean = false
    ): File {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("EmotionRepo", "Optimizing image for better emotion detection...")

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(originalFile.absolutePath, options)

                val sampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)

                val decodingOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inJustDecodeBounds = false
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                var bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, decodingOptions)
                    ?: throw IOException("Failed to decode image")

                bitmap = handleImageRotation(originalFile, bitmap)

                if (bitmap.width > MAX_IMAGE_SIZE || bitmap.height > MAX_IMAGE_SIZE) {
                    bitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                }

                bitmap = enhanceImageForEmotionDetection(bitmap)

                val timestamp = System.currentTimeMillis()
                val randomId = java.util.UUID.randomUUID().toString().substring(0, 8)
                val optimizedFile = File(
                    originalFile.parent,
                    "optimized_${timestamp}_${randomId}.jpg"
                )

                optimizedFiles.add(optimizedFile)

                FileOutputStream(optimizedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
                }

                bitmap.recycle()

                Log.d("EmotionRepo", "Image optimized: ${originalFile.length()} -> ${optimizedFile.length()} bytes")
                optimizedFile

            } catch (e: Exception) {
                Log.w("EmotionRepo", "Image optimization failed, using original: ${e.message}")
                originalFile
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun handleImageRotation(imageFile: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(imageFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.w("EmotionRepo", "Failed to handle rotation: ${e.message}")
            bitmap
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun enhanceImageForEmotionDetection(bitmap: Bitmap): Bitmap {
        return try {
            bitmap
        } catch (e: Exception) {
            Log.w("EmotionRepo", "Image enhancement failed: ${e.message}")
            bitmap
        }
    }

    fun cleanup() {
        Log.d("EmotionRepo", "Cleaning up resources...")
        clearAnalysisCache()
        authManager.cleanup()
        repositoryScope.cancel()
    }

    fun clearAuthCache() {
        Log.d("EmotionRepo", "Clearing auth cache...")
        authManager.clearTokenCache()
    }

    fun clearAnalysisCache() {
        Log.d("EmotionRepo", "Clearing analysis cache...")
        lastAnalysisResult = null
        lastAnalysisTimestamp = 0
        cleanupOptimizedFiles()
    }

    private fun cleanupOptimizedFiles() {
        optimizedFiles.toList().forEach { file ->
            try {
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("EmotionRepo", "Optimized file cleanup: ${file.name} - ${if (deleted) "deleted" else "failed"}")
                }
            } catch (e: Exception) {
                Log.w("EmotionRepo", "Failed to cleanup optimized file: ${file.name}", e)
            }
        }
        optimizedFiles.clear()
    }
}