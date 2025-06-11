package com.hackathon.temantidur.presentation.recommendation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.FragmentRecommendationBinding

class RecommendationFragment : Fragment() {

    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!
    private lateinit var recommendationAdapter: RecommendationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emotion = arguments?.getString("emotion") ?: "neutral"
        val recommendations = arguments?.getStringArrayList("recommendations") ?: arrayListOf(
            getString(R.string.default_recommendation_1),
            getString(R.string.default_recommendation_2),
            getString(R.string.default_recommendation_3)
        )

        Log.d("RecommendationFragment", "Emotion: $emotion")
        Log.d("RecommendationFragment", "Recommendations: $recommendations")

        binding.apply {
            sectionTitle.text = getEmotionBasedTitle(emotion)
            setupRecommendationRecyclerView(recommendations)
            apiRecommendationCard.visibility = View.GONE
        }
    }

    private fun setupRecommendationRecyclerView(recommendations: List<String>) {
        if (recommendations.isNotEmpty()) {
            recommendationAdapter = RecommendationAdapter(recommendations)
            binding.recommendationsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = recommendationAdapter
                setHasFixedSize(true)
            }

            Log.d("RecommendationFragment", "RecyclerView setup completed with ${recommendations.size} items")
        } else {
            Log.d("RecommendationFragment", "No recommendations to display")
            binding.recommendationsRecyclerView.visibility = View.GONE
        }
    }

    private fun getEmotionBasedTitle(emotion: String): String {
        val ctx = requireContext()
        return when (emotion.lowercase()) {
            "happy"      -> ctx.getString(R.string.recommendation_activity_happy)
            "sad"        -> ctx.getString(R.string.recommendation_activity_sad)
            "angry"      -> ctx.getString(R.string.recommendation_activity_angry)
            "anxious", "fear" -> ctx.getString(R.string.recommendation_activity_anxious)
            "stressed"   -> ctx.getString(R.string.recommendation_activity_stressed)
            "tired"      -> ctx.getString(R.string.recommendation_activity_tired)
            "lonely"     -> ctx.getString(R.string.recommendation_activity_lonely)
            "frustrated" -> ctx.getString(R.string.recommendation_activity_frustrated)
            else         -> ctx.getString(R.string.recommendation_activity_neutral)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}