package com.hackathon.temantidur.presentation.mainmenu

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.FragmentHomeBinding
import com.hackathon.temantidur.presentation.chat.ChatViewModel
import com.hackathon.temantidur.presentation.recommendation.RecommendationFragment
import com.hackathon.temantidur.data.emotion.EmotionStorageManager
import androidx.lifecycle.lifecycleScope
import com.hackathon.temantidur.common.ApiResult
import com.hackathon.temantidur.utils.dialogs.FailedDialogFragment
import com.hackathon.temantidur.utils.dialogs.LoadingDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModelProvider
import com.hackathon.temantidur.presentation.emotion.EmotionResultFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var emotionStorageManager: EmotionStorageManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setupUI()
        setupUsernameText()
        setupMainContentButtons()
        displayLastEmotionEmoji()
        setupRecapObserver()
    }

    override fun onResume() {
        super.onResume()
        binding.etStory?.setText("")
        setupUsernameText()
        displayLastEmotionEmoji()
    }

    private fun setupUI() {
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        emotionStorageManager = EmotionStorageManager(requireContext())
    }

    private fun displayLastEmotionEmoji() {
        val lastEmotionResult = emotionStorageManager.getLastEmotionResult()
        val emotionDrawable = when (lastEmotionResult?.emotion?.lowercase()) {
            "happy" -> R.drawable.emotion_happy
            "sad" -> R.drawable.emotion_sad
            "angry" -> R.drawable.emotion_angry
            "surprised" -> R.drawable.emotion_surprised
            "neutral" -> R.drawable.emotion_neutral
            else -> R.drawable.ic_face_home
        }

        binding.faceEmoji.setImageResource(emotionDrawable)
    }

    private fun setupUsernameText() {
        val sharedPref =
            activity?.getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
        val username = sharedPref?.getString("username", null)
            ?: auth.currentUser?.displayName
            ?: "User"
        val fullText = getString(R.string.hows_your_day, username)
        val spannableString = SpannableString(fullText)
        spannableString.setSpan(UnderlineSpan(), 0, fullText.length, 0)
        binding.howsYourDayText?.text = spannableString
    }

    private fun setupMainContentButtons() {
        binding.btnRecap?.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(
                RecapFragment(),
                showToolbar = false,
                tag = "RecapFragment"
            )
        }
        binding.btnRecommendation?.setOnClickListener {
            navigateToRecommendationFragment()
        }
        binding.etStory.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                (activity as? MainActivity)?.navigateToChatFragment()
            }
        }

        binding.faceEmojiContainer.setOnClickListener {
            navigateToLastEmotionResult()
        }
    }

    private fun setupRecapObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            chatViewModel.recapGenerationState.collectLatest { result ->
                when (result) {
                    is ApiResult.Loading -> {
                    }
                    is ApiResult.Success -> {
                        (activity as? MainActivity)?.replaceFragment(
                            RecapFragment(),
                            showToolbar = false,
                            tag = "RecapFragment"
                        )
                    }
                    is ApiResult.Error -> {
                        FailedDialogFragment.newInstance(
                            getString(R.string.failed_title),
                            result.message
                        ).show(parentFragmentManager, FailedDialogFragment.TAG)
                    }
                }
            }
        }
    }

    private fun navigateToLastEmotionResult() {
        val lastEmotionResult = emotionStorageManager.getLastEmotionResult()

        if (lastEmotionResult != null) {
            val bundle = Bundle().apply {
                putString("emotion", lastEmotionResult.emotion)
                putFloat("confidence", lastEmotionResult.confidence)
                putString("description", lastEmotionResult.description)
                putStringArrayList("recommendations", ArrayList(lastEmotionResult.recommendations))
            }

            val emotionResultFragment = EmotionResultFragment()
            emotionResultFragment.arguments = bundle

            (activity as? MainActivity)?.replaceFragment(
                emotionResultFragment,
                showToolbar = false,
                tag = "EmotionResultFragment"
            )
        } else {
            Toast.makeText(context, getString(R.string.no_emotion_data), Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.setActiveNavigation(R.id.nav_emotion)
            (activity as? MainActivity)?.navigateToEmotionCheck()
        }
    }

    private fun navigateToRecommendationFragment() {
        val lastEmotionResult = emotionStorageManager.getLastEmotionResult()

        val bundle = Bundle().apply {
            if (lastEmotionResult != null) {
                putString("emotion", lastEmotionResult.emotion)
                putStringArrayList("recommendations", ArrayList(lastEmotionResult.recommendations))
            } else {
                putString("emotion", "neutral")
                putStringArrayList("recommendations", arrayListOf(
                    getString(R.string.default_recommendation_1),
                    getString(R.string.default_recommendation_2),
                    getString(R.string.default_recommendation_3)
                ))            }
        }

        val recommendationFragment = RecommendationFragment()
        recommendationFragment.arguments = bundle
        (activity as? MainActivity)?.replaceFragment(
            recommendationFragment,
            showToolbar = true,
            tag = "RecommendationFragment"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::chatViewModel.isInitialized) {
            chatViewModel.cleanup()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}