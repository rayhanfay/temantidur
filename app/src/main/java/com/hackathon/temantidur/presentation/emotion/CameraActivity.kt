package com.hackathon.temantidur.presentation.emotion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hackathon.temantidur.R
import com.hackathon.temantidur.common.ApiResult
import com.hackathon.temantidur.data.emotion.EmotionRepository
import com.hackathon.temantidur.data.emotion.model.EmotionResult
import com.hackathon.temantidur.databinding.ActivityCameraLayoutBinding
import com.hackathon.temantidur.utils.dialogs.LoadingDialogFragment
import com.hackathon.temantidur.data.emotion.EmotionStorageManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraLayoutBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private val createdFiles = mutableListOf<File>()
    private lateinit var emotionRepository: EmotionRepository
    private var loadingDialog: LoadingDialogFragment? = null
    private var isProcessing = false
    private lateinit var emotionStorageManager: EmotionStorageManager
    private lateinit var frozenPreview: ImageView
    private lateinit var flashOverlay: View
    private var isFlashOn = false

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.camera_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("CameraActivity", "onCreate called")

        frozenPreview = binding.frozenPreview
        flashOverlay = binding.flashOverlay

        updateFlashIcon()

        emotionRepository = EmotionRepository(this)
        setupUI()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        emotionStorageManager = EmotionStorageManager(this)
        initializeAuth()
    }

    private fun initializeAuth() {
        lifecycleScope.launch {
            try {
                val success = emotionRepository.initializeAuth()
                if (!success) {
                    Toast.makeText(
                        this@CameraActivity,
                        getString(R.string.auth_init_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("CameraActivity", "Auth initialization error", e)
                Toast.makeText(
                    this@CameraActivity,
                    getString(R.string.init_error, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupUI() {
        Log.d("CameraActivity", "Setting up UI listeners")
        binding.captureButton.setOnClickListener {
            if (!isProcessing) {
                Log.d("CameraActivity", "Capture button clicked")
                animateCaptureButton()
                takePhotoAndAnalyze()
            }
        }

        binding.flashButton.setOnClickListener {
            isFlashOn = !isFlashOn
            updateFlashState()
        }
    }

    private fun updateFlashIcon() {
        val flashIcon = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        binding.btnFlash.setImageResource(flashIcon)
    }

    private fun animateCaptureButton() {
        binding.captureButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.captureButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = binding.cameraPreview.surfaceProvider
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetRotation(windowManager.defaultDisplay.rotation)
                    .setJpegQuality(95)
                    .build()
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Log.d("CameraActivity", "Camera started successfully")
            } catch (exc: Exception) {
                Log.e("CameraActivity", "Failed to start camera", exc)
                Toast.makeText(this, getString(R.string.camera_start_failed, exc.message), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndAnalyze() {
        val imageCapture = imageCapture ?: return
        if (isProcessing) return

        Log.d("CameraActivity", "Taking photo and analyzing...")
        isProcessing = true
        showLoadingDialog()

        freezePreview()

        val timestamp = System.currentTimeMillis()
        val randomId = (1000..9999).random()
        val outputFile = File(externalMediaDirs.firstOrNull() ?: filesDir, "emotion_${timestamp}_${randomId}.jpg")
        createdFiles.add(outputFile)

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraActivity", "Photo capture failed: ${exception.message}", exception)
                    runOnUiThread {
                        resetPreview()
                        isProcessing = false
                        hideLoadingDialog()
                        Toast.makeText(baseContext, getString(R.string.photo_capture_failed, exception.message), Toast.LENGTH_LONG).show()
                        cleanupFile(outputFile)
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraActivity", "Photo saved successfully: ${outputFile.absolutePath}")
                    emotionRepository.clearAnalysisCache()
                    analyzeEmotion(outputFile)
                }
            }
        )
    }

    private fun freezePreview() {
        val bitmap = binding.cameraPreview.bitmap
        if (bitmap != null) {
            frozenPreview.setImageBitmap(bitmap)
            frozenPreview.visibility = View.VISIBLE
            binding.cameraPreview.visibility = View.INVISIBLE
        } else {
            Log.e("CameraActivity", "Failed to get bitmap from preview")
        }
    }

    private fun resetPreview() {
        frozenPreview.visibility = View.GONE
        binding.cameraPreview.visibility = View.VISIBLE
    }

    private fun updateFlashState() {
        val flashIcon = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        binding.btnFlash.setImageResource(flashIcon)

        if (isFlashOn) {
            binding.flashOverlay.visibility = View.VISIBLE
            binding.flashOverlay.alpha = 0.8f

            val layoutParams = window.attributes
            layoutParams.screenBrightness = 1.0f
            window.attributes = layoutParams

        } else {
            binding.flashOverlay.visibility = View.GONE

            val layoutParams = window.attributes
            layoutParams.screenBrightness = -1f
            window.attributes = layoutParams
        }
    }

    private fun analyzeEmotion(imageFile: File) {
        lifecycleScope.launch {
            try {
                when (val result = emotionRepository.detectEmotion(imageFile)) {
                    is ApiResult.Success -> {
                        Log.d("CameraActivity", "Emotion analysis successful: ${result.data}")
                        Log.d("CameraActivity", "Emotion: ${result.data.emotion}")
                        Log.d("CameraActivity", "Confidence: ${result.data.confidence}")
                        Log.d("CameraActivity", "Description: ${result.data.description}")
                        Log.d("CameraActivity", "Recommendations count: ${result.data.recommendations.size}")
                        result.data.recommendations.forEachIndexed { index, rec ->
                            Log.d("CameraActivity", "Recommendation $index: $rec")
                        }

                        runOnUiThread {
                            returnEmotionResultToFragment(result.data)
                            cleanupFile(imageFile)
                        }
                    }
                    is ApiResult.Error -> {
                        Log.e("CameraActivity", "Emotion analysis failed: ${result.message}")
                        runOnUiThread {
                            hideLoadingDialog()
                            isProcessing = false
                            Toast.makeText(this@CameraActivity, getString(R.string.analysis_failed, result.message), Toast.LENGTH_LONG).show()
                            if (result.code == 401) {
                                emotionRepository.clearAuthCache()
                            }
                        }
                        cleanupFile(imageFile)
                    }
                    is ApiResult.Loading -> {
                        Log.d("CameraActivity", "Emotion analysis loading...")
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraActivity", "Exception during emotion analysis", e)
                runOnUiThread {
                    resetPreview()
                    isProcessing = false
                    hideLoadingDialog()
                    Toast.makeText(this@CameraActivity, getString(R.string.general_error, e.message), Toast.LENGTH_LONG).show()
                }
                cleanupFile(imageFile)
            }
        }
    }

    private fun returnEmotionResultToFragment(result: EmotionResult) {
        hideLoadingDialog()
        isProcessing = false

        Log.d("CameraActivity", "Preparing to return result to fragment")
        Log.d("CameraActivity", "Result emotion: ${result.emotion}")
        Log.d("CameraActivity", "Result confidence: ${result.confidence}")
        Log.d("CameraActivity", "Result description: ${result.description}")
        Log.d("CameraActivity", "Result recommendations count: ${result.recommendations.size}")
        result.recommendations.forEachIndexed { index, rec ->
            Log.d("CameraActivity", "Result recommendation $index: $rec")
        }

        val resultIntent = Intent().apply {
            val bundle = Bundle().apply {
                putString("emotion", result.emotion)
                putFloat("confidence", result.confidence)
                putString("description", result.description)

                if (result.recommendations.isNotEmpty()) {
                    putStringArrayList("recommendations", ArrayList(result.recommendations))
                    Log.d("CameraActivity", "Bundle - added ${result.recommendations.size} recommendations")
                } else {
                    Log.w("CameraActivity", "No recommendations to add to bundle")
                    putStringArrayList("recommendations", ArrayList())
                }

                Log.d("CameraActivity", "Bundle verification:")
                Log.d("CameraActivity", "  - emotion: ${getString("emotion")}")
                Log.d("CameraActivity", "  - confidence: ${getFloat("confidence")}")
                Log.d("CameraActivity", "  - description: ${getString("description")}")
                val bundleRecs = getStringArrayList("recommendations")
                Log.d("CameraActivity", "  - recommendations count: ${bundleRecs?.size ?: 0}")
                bundleRecs?.forEachIndexed { index, rec ->
                    Log.d("CameraActivity", "  - recommendation $index: $rec")
                }
            }
            putExtras(bundle)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        emotionStorageManager.saveLastEmotionResult(result)
    }

    private fun cleanupFile(file: File) {
        try {
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("CameraActivity", "File cleanup: ${file.name} - ${if (deleted) "deleted" else "failed to delete"}")
                createdFiles.remove(file)
            }
        } catch (e: Exception) {
            Log.w("CameraActivity", "Failed to cleanup file: ${file.name}", e)
        }
    }

    private fun cleanupAllFiles() {
        createdFiles.toList().forEach { file ->
            cleanupFile(file)
        }
        createdFiles.clear()
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialogFragment.newInstance {
                Log.d("CameraActivity", "Loading dialog dismissed")
            }
        }

        if (!loadingDialog!!.isAdded && !isFinishing) {
            loadingDialog!!.show(supportFragmentManager, LoadingDialogFragment.TAG)
        }
    }

    private fun hideLoadingDialog() {
        loadingDialog?.let { dialog ->
            if (dialog.isAdded && !isFinishing) {
                dialog.dismiss()
            }
        }
        loadingDialog = null
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CameraActivity", "onDestroy called")
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        cleanupAllFiles()
        emotionRepository.clearAnalysisCache()
        emotionRepository.cleanup()
        hideLoadingDialog()
    }

    override fun onPause() {
        super.onPause()
        emotionRepository.clearAnalysisCache()
    }

    override fun onBackPressed() {
        if (isProcessing) {
            Toast.makeText(this, getString(R.string.processing_photo), Toast.LENGTH_SHORT).show()
            resetPreview()
            return
        }

        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }
}