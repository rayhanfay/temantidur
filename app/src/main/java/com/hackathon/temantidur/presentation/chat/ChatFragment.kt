package com.hackathon.temantidur.presentation.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hackathon.temantidur.databinding.FragmentChatBinding
import com.hackathon.temantidur.presentation.voicechat.VoiceChatHandlerFragment
import com.hackathon.temantidur.presentation.voicechat.VoicePlayerManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.Rect
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var voiceChatHandler: VoiceChatHandlerFragment

    private var voiceMode = false

    private var isInputFocused = false
    private var isKeyboardVisible = false
    private var programmaticFocusChange = false

    private var recapGenerationJob: Job? = null

    companion object {
        private const val TAG = "ChatFragment"
        private const val AUDIO_PERMISSION_REQUEST_CODE = 1001
    }

    private fun debounceRecapGeneration() {
        recapGenerationJob?.cancel()
        recapGenerationJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(10000)
            viewModel.generateAndSaveRecapForToday()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupVoiceHandler()
        setupObservers()
        setupClickListeners()
        setupInputFocusHandler()
        setupKeyboardDetection(view)
        updateButtonState()

        arguments?.let {
            voiceMode = it.getBoolean("voice_mode", false)
            val shouldShowKeyboard = it.getBoolean("show_keyboard", true)

            val receivedVoiceFilePath = it.getString("voice_file_path")
            if (receivedVoiceFilePath != null) {
                Log.d(TAG, "ChatFragment received voice_file_path: $receivedVoiceFilePath")

                val alreadyProcessed = it.getBoolean("voice_already_processed", false)

                if (!alreadyProcessed) {
                    it.putBoolean("voice_already_processed", true)

                    view.post {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(100)

                            val audioFile = File(receivedVoiceFilePath)
                            if (audioFile.exists()) {
                                Log.d(TAG, "Processing voice file: ${audioFile.absolutePath}")
                                viewModel.sendVoiceMessage(audioFile)

                                delay(500)
                                scrollToBottom(smooth = true)
                            } else {
                                Log.w(TAG, "Voice file not found: $receivedVoiceFilePath")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Voice file already processed, just scrolling to bottom")
                    view.post {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(300)
                            scrollToBottom(smooth = true)
                        }
                    }
                }
            } else if (!voiceMode && shouldShowKeyboard) {
                view.post {
                    showKeyboardAndFocusInput()
                }
            }
        } ?: run {
            view.post {
                showKeyboardAndFocusInput()
            }
        }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    VoicePlayerManager.stop()
                }
            }
        })
    }

    private fun setupKeyboardDetection(view: View) {
        view.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            view.getWindowVisibleDisplayFrame(r)
            val screenHeight = view.rootView.height
            val keyboardHeight = screenHeight - r.bottom
            val wasKeyboardVisible = isKeyboardVisible
            isKeyboardVisible = keyboardHeight > screenHeight * 0.15

            if (wasKeyboardVisible && !isKeyboardVisible && binding.etChatMessage.hasFocus()) {
                if (!programmaticFocusChange) {
                    clearFocusAndUpdateState()
                }
            }
        }
    }

    private fun clearFocusAndUpdateState() {
        programmaticFocusChange = true
        binding.etChatMessage.clearFocus()
        isInputFocused = false
        updateButtonState()
        programmaticFocusChange = false
    }

    private fun showKeyboardAndFocusInput() {
        programmaticFocusChange = true
        binding.etChatMessage.requestFocus()

        binding.etChatMessage.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etChatMessage, InputMethodManager.SHOW_IMPLICIT)
        }

        isInputFocused = true
        updateButtonState()
        programmaticFocusChange = false
    }

    private fun hideKeyboardOnly() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etChatMessage.windowToken, 0)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
    }

    private fun setupVoiceHandler() {
        voiceChatHandler = VoiceChatHandlerFragment(binding, requireContext(), object : VoiceChatHandlerFragment.VoiceChatListener {
            override fun onVoiceMessageSent(audioFile: File) {
                Log.d(TAG, "VoiceChatHandlerFragment: onVoiceMessageSent, sending to ViewModel. Path: ${audioFile.name}")
                viewModel.sendVoiceMessage(audioFile)
            }

            override fun onVoiceError(error: String) {
                showErrorMessage(error)
            }

            override fun onPermissionRequired() {
                requestAudioPermission()
            }
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Starting to observe chatMessages")
                viewModel.chatMessages.collectLatest { messages ->
                    Log.d(TAG, "ChatMessages updated. New size: ${messages.size}. Previous size: ${chatAdapter.itemCount}")

                    val wasAtBottom = isAtBottom()
                    val previousCount = chatAdapter.itemCount

                    view?.post {
                        chatAdapter.submitChatMessages(messages)

                        binding.chatRecyclerView.layoutManager?.let { layoutManager ->
                            if (layoutManager is LinearLayoutManager) {
                                layoutManager.requestLayout()
                            }
                        }

                        if (messages.isNotEmpty() && (wasAtBottom || messages.size > previousCount)) {
                            Log.d(TAG, "Scrolling to bottom. Was at bottom: $wasAtBottom, has new message: ${messages.size > previousCount}")
                            binding.chatRecyclerView.post {
                                binding.chatRecyclerView.post {
                                    scrollToBottom(smooth = true)
                                }
                            }
                        } else {
                            Log.d(TAG, "Not scrolling. No new messages or not at bottom. Current size: ${messages.size}, previous: $previousCount")
                        }
                    }

                    debounceRecapGeneration()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Starting to observe voiceResponse")
                viewModel.voiceResponse.collect { voiceResponse ->
                    Log.d(TAG, "Voice response received: ${voiceResponse != null}")
                    voiceResponse?.audioData?.let { audioData ->
                        Log.d(TAG, "Voice response audio data size: ${audioData.size}")
                        saveAndPlayAiAudio(audioData)
                        viewModel.voiceResponseHandled()

                        view?.post {
//                            if (isAtBottom()) {
                                binding.chatRecyclerView.post {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        delay(300)
                                        scrollToBottom(smooth = true)
                                    }
//                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isAtBottom(): Boolean {
        val layoutManager = binding.chatRecyclerView.layoutManager as LinearLayoutManager
        if (chatAdapter.itemCount == 0) return true
        val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
        return lastVisiblePosition >= chatAdapter.itemCount - 1
    }

    private fun saveAndPlayAiAudio(audioBytes: ByteArray) {
        try {
            val tempAudioFile =
                File.createTempFile("ai_response_", ".wav", requireContext().cacheDir)
            FileOutputStream(tempAudioFile).use { it.write(audioBytes) }
            Log.d(TAG, "AI audio saved to: ${tempAudioFile.absolutePath}")
            VoicePlayerManager.playOrPause(tempAudioFile.absolutePath)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save AI audio", e)
            showErrorMessage("Gagal memutar audio AI")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInputFocusHandler() {
        binding.etChatMessage.setOnFocusChangeListener { _, hasFocus ->
            if (!programmaticFocusChange) {
                isInputFocused = hasFocus
                updateButtonState()
            }
        }

        binding.etChatMessage.setOnClickListener {
            if (!isInputFocused && !isKeyboardVisible) {
                showKeyboardAndFocusInput()
            }
        }

        binding.etChatMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chatAndInputContainer.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isKeyboardVisible) {
                val outRect = Rect()
                binding.etChatMessage.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    hideKeyboardOnly()
                    clearFocusAndUpdateState()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun updateButtonState() {
        val hasText = binding.etChatMessage.text.toString().trim().isNotEmpty()
        val shouldShowSendButton = isInputFocused || hasText

        if (shouldShowSendButton) {
            binding.cardSend.apply {
                visibility = View.VISIBLE
                isEnabled = true
                isClickable = true
                bringToFront()
            }

            binding.cardMic.apply {
                visibility = View.GONE
                isEnabled = false
                isClickable = false
            }

        } else {
            binding.cardMic.apply {
                visibility = View.VISIBLE
                isEnabled = true
                isClickable = true
                bringToFront()
            }

            binding.cardSend.apply {
                visibility = View.GONE
                isEnabled = false
                isClickable = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        binding.btnSendMessage.setOnClickListener {
            Log.d("ChatFragment", "Send button clicked")
            sendTextMessage()
        }

        binding.btnMic.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Mic button ACTION_DOWN")
                    if (!voiceChatHandler.isCurrentlyRecording()) {
                        handleVoiceRecording(true)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "Mic button ACTION_UP/CANCEL")
                    if (voiceChatHandler.isCurrentlyRecording()) {
                        handleVoiceRecording(false)
                    }
                    true
                }
                else -> false
            }
        }

        binding.btnBack.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            navigateBack()
        }
    }

    private fun sendTextMessage() {
        val message = binding.etChatMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            viewModel.sendMessage(message)
            binding.etChatMessage.setText("")

            binding.chatRecyclerView.post {
                binding.chatRecyclerView.post {
                    scrollToBottom(smooth = true)
                }
            }
        }
    }

    private fun handleVoiceRecording(start: Boolean) {
        if (start) {
            if (!voiceChatHandler.isCurrentlyRecording()) {
                voiceChatHandler.startRecording()
            }
        } else {
            if (voiceChatHandler.isCurrentlyRecording()) {
                voiceChatHandler.stopRecording()
            }
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun scrollToBottom(smooth: Boolean = true) {
        if (chatAdapter.itemCount > 0) {
            val lastItemPosition = chatAdapter.itemCount - 1
            if (smooth) {
                binding.chatRecyclerView.smoothScrollToPosition(lastItemPosition)
            } else {
                binding.chatRecyclerView.scrollToPosition(lastItemPosition)
            }
        }
    }

    private fun navigateBack() {
        hideKeyboardOnly()
        (activity as? MainActivity)?.onBackPressedDispatcher?.onBackPressed()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            voiceChatHandler.handlePermissionResult(grantResults)
        }
    }

    override fun onStop() {
        super.onStop()
        VoicePlayerManager.stop()
        if (::voiceChatHandler.isInitialized) {
            voiceChatHandler.stopRecording()
        }
        clearFocusAndUpdateState()
        hideKeyboardOnly()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboardOnly()

        VoicePlayerManager.stop()
        if (::voiceChatHandler.isInitialized) {
            voiceChatHandler.cleanup()
        }


        binding.chatRecyclerView.adapter = null

        _binding = null
    }
}