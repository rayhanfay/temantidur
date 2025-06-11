package com.hackathon.temantidur.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff // Diperlukan untuk PorterDuff.Mode.SRC_IN jika menggunakan setColorFilter
import android.net.Uri
import android.os.Build // Diperlukan jika menggunakan Build.VERSION.SDK_INT
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.emotion.EmotionStorageManager
import com.hackathon.temantidur.presentation.mainmenu.MainActivity
import java.util.ArrayList

class MyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.app_widget_layout
            )

            val emotionStorageManager = EmotionStorageManager(context)
            val lastEmotionResult = emotionStorageManager.getLastEmotionResult()

            val faceEmojiDrawableRes = when (lastEmotionResult?.emotion?.lowercase()) {
                "happy" -> R.drawable.emotion_happy
                "sad" -> R.drawable.emotion_sad
                "angry" -> R.drawable.emotion_angry
                "surprised" -> R.drawable.emotion_surprised
                "neutral" -> R.drawable.emotion_neutral
                "disgust" -> R.drawable.emotion_disgust
                "sickened" -> R.drawable.emotion_sickened
                else -> R.drawable.ic_face_home
            }
            views.setImageViewResource(R.id.face_emoji, faceEmojiDrawableRes)
            val darkblueColor = ContextCompat.getColor(context, R.color.darkblue)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { 
                views.setInt(R.id.face_emoji, "setColorFilter", darkblueColor)
            } else {
                views.setInt(R.id.face_emoji, "setColorFilter", darkblueColor)
            }

            val softMagentaColor = ContextCompat.getColor(context, R.color.softmagenta)

            views.setInt(R.id.widget_chat_icon, "setColorFilter", softMagentaColor)
            views.setTextColor(R.id.widget_chat_text, softMagentaColor)
            views.setInt(R.id.widget_emotion_icon, "setColorFilter", softMagentaColor)
            views.setTextColor(R.id.widget_emotion_text, softMagentaColor)
            views.setInt(R.id.widget_recommendation_icon, "setColorFilter", softMagentaColor)
            views.setTextColor(R.id.widget_recommendation_text, softMagentaColor)

            val faceEmojiIntent = Intent(context, MainActivity::class.java).apply {
                if (lastEmotionResult != null) {
                    putExtra("start_destination", "emotion_result_from_widget")
                    val bundle = Bundle().apply {
                        putString("emotion", lastEmotionResult.emotion)
                        putFloat("confidence", lastEmotionResult.confidence)
                        putString("description", lastEmotionResult.description)
                        putStringArrayList("recommendations", ArrayList(lastEmotionResult.recommendations))
                    }
                    putExtra("emotion_data_bundle", bundle)
                } else {
                    putExtra("start_destination", "emotion_check")
                }
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME) + "face_emoji_click_$appWidgetId")
            }
            val faceEmojiPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 0,
                faceEmojiIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_face_emoji_container, faceEmojiPendingIntent)

            val chatIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("start_destination", "chat")
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME) + "chat_click_$appWidgetId")
            }
            val chatPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 1,
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_chat_container, chatPendingIntent)

            val emotionCheckIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("start_destination", "emotion_check")
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME) + "emotion_click_$appWidgetId")
            }
            val emotionCheckPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 2,
                emotionCheckIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_emotion_container, emotionCheckPendingIntent)

            val recommendationIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("start_destination", "recommendation")
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME) + "recommendation_click_$appWidgetId")
            }
            val recommendationPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 3,
                recommendationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_recommendation_container, recommendationPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}