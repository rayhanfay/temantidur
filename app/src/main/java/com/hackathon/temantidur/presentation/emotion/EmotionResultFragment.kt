package com.hackathon.temantidur.presentation.emotion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.FragmentEmotionResultBinding
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import com.hackathon.temantidur.presentation.recommendation.RecommendationFragment
import com.hackathon.temantidur.data.emotion.EmotionStorageManager
import com.hackathon.temantidur.data.emotion.model.EmotionResult

class EmotionResultFragment : Fragment() {

    private var _binding: FragmentEmotionResultBinding? = null
    private val binding get() = _binding!!
    private var emotion: String = "Unknown"
    private var confidence: Float = 0f
    private var description: String = ""
    private var recommendations: ArrayList<String> = ArrayList()
    private lateinit var emotionStorageManager: EmotionStorageManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmotionResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgumentsData()
        setupUI()
        displayEmotionResult()
        emotionStorageManager = EmotionStorageManager(requireContext())
        saveCurrentEmotionResult()
    }

    private fun getArgumentsData() {
        arguments?.let {
            emotion = it.getString("emotion", "Unknown")
            confidence = it.getFloat("confidence", 0f)
            description = it.getString("description", getString(R.string.no_ai_description))
            recommendations = it.getStringArrayList("recommendations") ?: ArrayList()
        }
    }

    private fun setupUI() {
        binding.btnRecommendation.setOnClickListener {
            navigateToRecommendation()
        }
    }

    private fun displayEmotionResult() {
        binding.tvEmotionTitle.text = getEmotionTitle(emotion)
        binding.tvEmotionDescription.text = description
        binding.ivEmotionResult.setImageResource(getEmotionDrawable(emotion))
    }

    private fun saveCurrentEmotionResult() {
        val emotionResult = EmotionResult(
            emotion = emotion,
            confidence = confidence,
            description = description,
            recommendations = recommendations,
            imageFile = null
        )
        emotionStorageManager.saveLastEmotionResult(emotionResult)
    }

    private fun getEmotionTitle(emotion: String): String {
        return when (emotion.lowercase()) {
            "happy" -> getString(R.string.emotion_happy_title)
            "sad" -> getString(R.string.emotion_sad_title)
            "angry" -> getString(R.string.emotion_angry_title)
            "surprised" -> getString(R.string.emotion_surprised_title)
            "neutral" -> getString(R.string.emotion_neutral_title)
            else -> getString(R.string.emotion_unknown_title)
        }
    }

    private fun getEmotionDrawable(emotion: String): Int {
        return when (emotion.lowercase()) {
            "happy" -> R.drawable.emotion_happy
            "sad" -> R.drawable.emotion_sad
            "angry" -> R.drawable.emotion_angry
            "surprised" -> R.drawable.emotion_surprised
            "sickened", "disgust" -> R.drawable.emotion_disgust
            "fear" -> R.drawable.emotion_sad
            "neutral" -> R.drawable.emotion_neutral
            "galau", "lonely" -> R.drawable.emotion_sad
            "anxious", "stressed", "frustrated" -> R.drawable.emotion_angry
            "excited" -> R.drawable.emotion_happy
            "tired", "calm" -> R.drawable.emotion_neutral
            else -> R.drawable.emotion_neutral
        }
    }

    private fun navigateToRecommendation() {
        val bundle = Bundle().apply {
            putString("emotion", emotion)
            putStringArrayList("recommendations", recommendations)
        }
        val fragment = RecommendationFragment()
        fragment.arguments = bundle
        (activity as? MainActivity)?.replaceFragment(
            fragment,
            showToolbar = false,
            tag = "RecommendationFragment"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}