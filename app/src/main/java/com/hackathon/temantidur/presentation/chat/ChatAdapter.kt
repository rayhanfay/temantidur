package com.hackathon.temantidur.presentation.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.chat.model.ChatMessage
import com.hackathon.temantidur.presentation.voicechat.VoicePlayerManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class ChatItem {
    data class MessageItem(val message: ChatMessage) : ChatItem()
    data class DateSeparatorItem(val date: String, val formattedDate: String) : ChatItem()
}

class ChatAdapter(private val context: Context) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback()) {

    private var lastPosition = -1 // Untuk animasi
    companion object {
        private const val VIEW_TYPE_TEXT_INCOMING = 1
        private const val VIEW_TYPE_TEXT_OUTGOING = 2
        private const val VIEW_TYPE_VOICE_INCOMING = 3
        private const val VIEW_TYPE_VOICE_OUTGOING = 4
        private const val VIEW_TYPE_DATE_SEPARATOR = 5
    }

    fun submitChatMessages(messages: List<ChatMessage>) {
        val chatItems = mutableListOf<ChatItem>()

        messages.forEachIndexed { index, message ->
            val previousMessage = if (index > 0) messages[index - 1] else null

            if (shouldShowDateSeparator(message, previousMessage)) {
                val dateString = getDateSeparatorText(message)
                val formattedDate = message.getFormattedDate()
                chatItems.add(ChatItem.DateSeparatorItem(dateString, formattedDate))
            }

            chatItems.add(ChatItem.MessageItem(message))
        }
        Log.d("ChatAdapter", "Submitting new list. Total items: ${chatItems.size}")
        submitList(chatItems)
    }

    private fun shouldShowDateSeparator(currentMessage: ChatMessage, previousMessage: ChatMessage?): Boolean {
        if (previousMessage == null) return true

        val currentDate = Calendar.getInstance().apply {
            timeInMillis = currentMessage.timestamp
        }
        val previousDate = Calendar.getInstance().apply {
            timeInMillis = previousMessage.timestamp
        }

        return currentDate.get(Calendar.DAY_OF_YEAR) != previousDate.get(Calendar.DAY_OF_YEAR) ||
                currentDate.get(Calendar.YEAR) != previousDate.get(Calendar.YEAR)
    }

    private fun getDateSeparatorText(message: ChatMessage): String {
        return when {
            message.isToday() -> context.getString(R.string.date_today)
            message.isYesterday() -> context.getString(R.string.date_yesterday)
            else -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = message.timestamp }
                val dayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                dayFormat.format(calendar.time)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.DateSeparatorItem -> VIEW_TYPE_DATE_SEPARATOR
            is ChatItem.MessageItem -> {
                val message = item.message
                if (!message.isIncoming) {
                    if (message.isVoiceMessage) VIEW_TYPE_VOICE_OUTGOING else VIEW_TYPE_TEXT_OUTGOING
                } else {
                    if (message.isVoiceMessage) VIEW_TYPE_VOICE_INCOMING else VIEW_TYPE_TEXT_INCOMING
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT_OUTGOING -> TextMessageViewHolder(inflater.inflate(R.layout.item_chat_outgoing, parent, false))
            VIEW_TYPE_TEXT_INCOMING -> TextMessageViewHolder(inflater.inflate(R.layout.item_chat_incoming, parent, false))
            VIEW_TYPE_VOICE_OUTGOING -> VoiceMessageViewHolder(inflater.inflate(R.layout.item_chat_voice_outgoing, parent, false))
            VIEW_TYPE_VOICE_INCOMING -> VoiceMessageViewHolder(inflater.inflate(R.layout.item_chat_voice_incoming, parent, false))
            VIEW_TYPE_DATE_SEPARATOR -> DateSeparatorViewHolder(inflater.inflate(R.layout.item_date_separator, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TextMessageViewHolder -> {
                if (item is ChatItem.MessageItem) {
                    holder.bind(item.message)
                }
            }
            is VoiceMessageViewHolder -> {
                if (item is ChatItem.MessageItem) {
                    holder.bind(item.message)
                }
            }
            is DateSeparatorViewHolder -> {
                if (item is ChatItem.DateSeparatorItem) {
                    holder.bind(item.date)
                }
            }
        }
        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.item_animation_from_bottom)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    class DateSeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.date_text)

        fun bind(dateString: String) {
            dateText.text = dateString
        }
    }

    class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)

        @SuppressLint("SetTextI18n")
        fun bind(message: ChatMessage) {
            messageText.text = message.message
            timestampText.text = message.getFormattedTime()
        }
    }

    class VoiceMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playPauseButton: ImageButton = itemView.findViewById(R.id.play_pause_button)
        private val seekBar: SeekBar = itemView.findViewById(R.id.seek_bar)
        private val durationText: TextView = itemView.findViewById(R.id.duration_text)
        private val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)
        private var isSeekBarTracking = false
        private var currentAudioPath: String? = null

        private val TAG = "VoiceMessageViewHolder"

        fun bind(message: ChatMessage) {
            val audioPath = message.voiceFilePath ?: return
            currentAudioPath = audioPath
            Log.d(TAG, "Binding item for audio: ${audioPath.takeLast(15)}")

            timestampText.text = message.getFormattedTime()

            val messageDuration = message.voiceDuration
            durationText.text = formatDuration(messageDuration)
            seekBar.max = messageDuration.toInt().let { if (it > 0) it else 100 }
            seekBar.progress = 0

            playPauseButton.setImageResource(R.drawable.ic_play)

            playPauseButton.setOnClickListener {
                Log.d(TAG, ">>> Play button clicked for: ${audioPath.takeLast(15)}")
                VoicePlayerManager.playOrPause(audioPath)
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        durationText.text = formatDuration(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarTracking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { progress ->
                        VoicePlayerManager.seekTo(progress)
                    }
                    isSeekBarTracking = false
                }
            })

            itemView.post {
                val lifecycleOwner = itemView.findViewTreeLifecycleOwner()
                if (lifecycleOwner == null) {
                    Log.e(TAG, "LifecycleOwner is NULL after post(). UI will not update for: ${audioPath.takeLast(15)}")
                    return@post
                }

                lifecycleOwner.lifecycleScope.launch {
                    Log.d(TAG, "Setting up state observers for: ${audioPath.takeLast(15)}")

                    launch {
                        combine(
                            VoicePlayerManager.currentPlayingAudioUrl,
                            VoicePlayerManager.playbackState
                        ) { currentUrl, state ->
                            Pair(currentUrl, state)
                        }.collect { (currentUrl, playbackState) ->
                            val isThisMessagePlaying = currentUrl == audioPath
                            Log.d(TAG, "State changed for [${audioPath.takeLast(15)}]: " +
                                    "isPlaying=$isThisMessagePlaying, state=$playbackState")

                            updatePlayPauseButton(isThisMessagePlaying, playbackState)

                            if (!isThisMessagePlaying) {
                                seekBar.progress = 0
                                durationText.text = formatDuration(messageDuration)
                            }
                        }
                    }

                    launch {
                        combine(
                            VoicePlayerManager.currentPlayingAudioUrl,
                            VoicePlayerManager.currentProgress,
                            VoicePlayerManager.currentDuration
                        ) { currentUrl, progress, duration ->
                            Triple(currentUrl, progress, duration)
                        }.collect { (currentUrl, currentProgress, totalDuration) ->
                            val isThisMessagePlaying = currentUrl == audioPath

                            if (isThisMessagePlaying) {
                                Log.v(TAG, "Progress update for [${audioPath.takeLast(15)}]: " +
                                        "progress=$currentProgress, duration=$totalDuration")

                                if (totalDuration > 0 && seekBar.max != totalDuration) {
                                    seekBar.max = totalDuration
                                }

                                if (!isSeekBarTracking) {
                                    seekBar.progress = currentProgress
                                    durationText.text = formatDuration(currentProgress.toLong())
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun updatePlayPauseButton(
            isThisMessagePlaying: Boolean,
            playbackState: VoicePlayerManager.PlaybackState
        ) {
            if (isThisMessagePlaying) {
                when (playbackState) {
                    is VoicePlayerManager.PlaybackState.Playing -> {
                        playPauseButton.setImageResource(R.drawable.ic_pause)
                        Log.d(TAG, "Button set to PAUSE")
                    }
                    is VoicePlayerManager.PlaybackState.Paused -> {
                        playPauseButton.setImageResource(R.drawable.ic_play)
                        Log.d(TAG, "Button set to PLAY (paused)")
                    }
                    is VoicePlayerManager.PlaybackState.Stopped -> {
                        playPauseButton.setImageResource(R.drawable.ic_play)
                        Log.d(TAG, "Button set to PLAY (stopped)")
                    }
                    is VoicePlayerManager.PlaybackState.Error -> {
                        playPauseButton.setImageResource(R.drawable.ic_play)
                        Log.e(TAG, "Playback error: ${playbackState.message}")
                    }
                }
            } else {
                playPauseButton.setImageResource(R.drawable.ic_play)
            }
        }

        private fun formatDuration(millis: Long): String {
            if (millis <= 0) return "00:00"
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    class ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            val areSame = when {
                oldItem is ChatItem.MessageItem && newItem is ChatItem.MessageItem -> {
                    oldItem.message.id == newItem.message.id
                }
                oldItem is ChatItem.DateSeparatorItem && newItem is ChatItem.DateSeparatorItem ->
                    oldItem.date == newItem.date
                else -> false
            }
            Log.d("DiffUtilCallback", "areItemsTheSame: $areSame for old: $oldItem, new: $newItem")
            return areSame
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            val areSame = oldItem == newItem
            Log.d("DiffUtilCallback", "areContentsTheSame: $areSame for old: $oldItem, new: $newItem")
            return areSame
        }
    }
}