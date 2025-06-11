package com.hackathon.temantidur.presentation.recommendation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.temantidur.R
import com.hackathon.temantidur.databinding.ItemRecommendationBinding

class RecommendationAdapter(
    private val recommendations: List<String>
) : RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder>() {

    inner class RecommendationViewHolder(
        private val binding: ItemRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recommendation: String, position: Int) {
            binding.apply {
                titleTextView.text = binding.root.context
                    .getString(R.string.recommendation_title, position + 1)
                descTextView.text = recommendation
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecommendationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(recommendations[position], position)
    }

    override fun getItemCount(): Int = recommendations.size
}