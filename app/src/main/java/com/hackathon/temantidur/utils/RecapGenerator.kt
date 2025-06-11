package com.hackathon.temantidur.utils

import android.content.Context
import com.hackathon.temantidur.R
import com.hackathon.temantidur.data.chat.model.ChatMessage
import com.hackathon.temantidur.data.chat.model.DailyRecap
import java.text.SimpleDateFormat
import java.util.*

class RecapGenerator(private val context: Context) {

    fun generateDailyRecap(messages: List<ChatMessage>): DailyRecap {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val todayDate = dateFormat.format(Date())

        // Filter pesan hanya untuk hari ini
        val todayMessages = messages.filter {
            dateFormat.format(Date(it.timestamp)) == todayDate
        }

        val summary = if (todayMessages.isEmpty()) {
            context.getString(R.string.recap_no_activity)
        } else {
            generateSummaryFromConversation(todayMessages)
        }

        return DailyRecap(
            date = todayDate,
            dayLabel = getDayLabel(todayDate),
            summary = summary
        )
    }

    private fun getDayLabel(dateString: String): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return dateString

        val calendar = Calendar.getInstance().apply {
            time = date
        }

        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> dateString
        }
    }

    private fun generateSummaryFromConversation(messages: List<ChatMessage>): String {
        // Hitung statistik dasar
        val userMessages = messages.filter { !it.isIncoming }
        val aiMessages = messages.filter { it.isIncoming }
        val userMessageCount = userMessages.size
        val aiMessageCount = aiMessages.size
        val totalMessages = messages.size
        val firstMessageTime = messages.firstOrNull()?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
        } ?: "-"
        val lastMessageTime = messages.lastOrNull()?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
        } ?: "-"

        // Analisis konten
        val conversationText = messages.joinToString(" ") { it.message }
        val topics = identifyMainTopics(conversationText)
        val sentiment = analyzeSentiment(conversationText)
        val wordCount = conversationText.split("\\s+".toRegex()).size
        val avgMessageLength = if (totalMessages > 0) wordCount / totalMessages else 0

        // Deteksi emosi khusus
        val emotionIcons = detectEmotionIcons(conversationText)

        // Temukan pesan terpanjang
        val longestMessage = messages.maxByOrNull { it.message.length }?.message?.let {
            if (it.length > 50) it.substring(0, 47) + "..." else it
        } ?: "-"

        // Bangun summary dengan semua parameter yang tersedia
        return buildString {
            append("📊 Rekap Percakapan\n\n")
            append("🕒 Waktu: $firstMessageTime - $lastMessageTime\n")
            append("🧮 Statistik:\n")
            append(" • Total pesan: $totalMessages (Anda: $userMessageCount, AI: $aiMessageCount)\n")
            append(" • Kata: $wordCount (Rata-rata: $avgMessageLength kata/pesan)\n\n")

            if (topics.isNotEmpty()) {
                append("🏷 Topik Utama:\n")
                append(" • ${topics.joinToString(", ")}\n\n")
            }

            append("😊 Sentimen: $sentiment\n")

            if (emotionIcons.isNotEmpty()) {
                append("💖 Emosi Terdeteksi: ${emotionIcons.joinToString(" ")}\n\n")
            }

            append("💬 Pesan Penting:\n")
            append(" • Pesan terpanjang: \"$longestMessage\"\n")

            val questionMessages = messages.filter { it.message.contains("?") }
            if (questionMessages.isNotEmpty()) {
                append(" • Pertanyaan diajukan: ${questionMessages.size}\n")
            }

            val longMessages = messages.filter { it.message.length > 50 }
            if (longMessages.isNotEmpty()) {
                append(" • Pesan detail: ${longMessages.size}\n")
            }

            // Contoh percakapan penting
            val importantMessages = messages.filter {
                it.message.length > 30 ||
                        it.message.contains("!") ||
                        it.message.contains("?")
            }

            if (importantMessages.isNotEmpty()) {
                append("\n🔖 Contoh Percakapan:\n")
                importantMessages.take(3).forEach { msg ->
                    val prefix = if (msg.isIncoming) "AI" else "Anda"
                    val shortMsg = if (msg.message.length > 40)
                        msg.message.take(37) + "..."
                    else msg.message
                    append(" • [$prefix] $shortMsg\n")
                }
            }

            append("\n💡 Tips: ${getRandomTip()}")
        }
    }

    private fun detectEmotionIcons(text: String): List<String> {
        val emotionMap = mapOf(
            "senang" to "😊",
            "bahagia" to "😄",
            "sedih" to "😢",
            "marah" to "😠",
            "lelah" to "😩",
            "stres" to "😰",
            "bingung" to "😕",
            "takjub" to "😲",
            "cinta" to "❤️",
            "terima kasih" to "🙏",
            "baik hati" to "😇"
        )

        return emotionMap.filter { (word, _) ->
            text.contains(word, ignoreCase = true)
        }.values.toList()
    }

    private fun getRandomTip(): String {
        val tips = listOf(
            "Jaga pola tidur yang teratur untuk kesehatan yang lebih baik",
            "Luangkan waktu untuk relaksasi setiap hari",
            "Bicarakan perasaan Anda dengan orang terdekat",
            "Catat pencapaian kecil Anda setiap hari",
            "Minum air yang cukup untuk menjaga konsentrasi",
            "Lakukan istirahat singkat setiap 90 menit bekerja"
        )
        return tips.random()
    }

    private fun identifyMainTopics(text: String): List<String> {
        val keywords = listOf(
            "tidur", "istirahat", "lelah", "capek", "stres", "tekanan",
            "kerja", "pekerjaan", "kantor", "sekolah", "kuliah",
            "keluarga", "teman", "hubungan", "rencana", "target"
        )
        return keywords.filter { keyword ->
            text.contains(keyword, ignoreCase = true)
        }.distinct()
    }

    private fun analyzeSentiment(text: String): String {
        val positiveWords = context.resources.getStringArray(R.array.positive_emotions).toList()
        val negativeWords = context.resources.getStringArray(R.array.negative_emotions).toList()

        val positiveCount = positiveWords.count { word ->
            text.contains(word, ignoreCase = true)
        }

        val negativeCount = negativeWords.count { word ->
            text.contains(word, ignoreCase = true)
        }

        return when {
            positiveCount > negativeCount * 1.5 -> "Positif 😊"
            negativeCount > positiveCount * 1.5 -> "Negatif 😔"
            positiveCount > 0 && negativeCount > 0 -> "Campuran 😐"
            positiveCount > 0 -> "Cenderung positif 🙂"
            negativeCount > 0 -> "Cenderung negatif 🙁"
            else -> "Netral 😐"
        }
    }
}