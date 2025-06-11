package com.hackathon.temantidur.data.chat.model

data class VoiceApiResponse(
    val userText: String?,
    val aiText: String?,
    val audioData: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceApiResponse
        if (userText != other.userText) return false
        if (aiText != other.aiText) return false
        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = userText?.hashCode() ?: 0
        result = 31 * result + (aiText?.hashCode() ?: 0)
        result = 31 * result + (audioData?.contentHashCode() ?: 0)
        return result
    }
}