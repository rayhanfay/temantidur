package com.hackathon.temantidur.presentation.sidemenu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.temantidur.R

class AvatarAdapter(
    private val avatars: List<Int>,
    private val onAvatarSelected: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImageView: ImageView = itemView.findViewById(R.id.iv_avatar_item)
        val selectedBorder: View = itemView.findViewById(R.id.view_selected_border_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatarResId = avatars[position]
        holder.avatarImageView.setImageResource(avatarResId)

        if (position == selectedPosition) {
            holder.selectedBorder.visibility = View.VISIBLE
        } else {
            holder.selectedBorder.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(selectedPosition)
            onAvatarSelected(avatarResId)
        }
    }

    override fun getItemCount(): Int = avatars.size

    fun setSelectedAvatar(resId: Int) {
        val index = avatars.indexOf(resId)
        if (index != -1 && index != selectedPosition) {
            val previousSelectedPosition = selectedPosition
            selectedPosition = index
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}