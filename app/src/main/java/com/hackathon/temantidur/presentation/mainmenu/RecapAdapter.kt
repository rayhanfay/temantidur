package com.hackathon.temantidur.presentation.mainmenu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.temantidur.data.chat.model.DailyRecap
import com.hackathon.temantidur.databinding.ItemRecapBinding

class RecapAdapter(private val recaps: List<DailyRecap>) :
    RecyclerView.Adapter<RecapAdapter.RecapViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecapViewHolder {
        val binding = ItemRecapBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecapViewHolder, position: Int) {
        holder.bind(recaps[position])
    }

    override fun getItemCount() = recaps.size

    inner class RecapViewHolder(private val binding: ItemRecapBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recap: DailyRecap) {
            binding.tvDate.text = "${recap.dayLabel}, ${recap.date}"
            binding.tvSummary.text = recap.summary
        }
    }
}