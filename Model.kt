package com.saveclip.app

import androidx.compose.ui.graphics.Color

enum class Mode { VIDEO, AUDIO }

enum class Quality(val label: String, val format: String) {
    BEST("Лучшее", "bv*[ext=mp4]+ba[ext=m4a]/bv*+ba/b"),
    P1080("1080p", "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/bv*[height<=1080]+ba/b[height<=1080]/b"),
    P720("720p", "bv*[height<=720][ext=mp4]+ba[ext=m4a]/bv*[height<=720]+ba/b[height<=720]/b"),
    P480("480p", "bv*[height<=480][ext=mp4]+ba[ext=m4a]/bv*[height<=480]+ba/b[height<=480]/b"),
}

enum class Phase { IDLE, PREPARING, DOWNLOADING, SAVING }

enum class Platform(val label: String, val color: Color) {
    YOUTUBE("YouTube", Color(0xFFFF5A52)),
    TIKTOK("TikTok", Color(0xFF2DE6E0)),
    INSTAGRAM("Instagram", Color(0xFFF06BA8)),
}

fun detectPlatform(url: String): Platform? {
    val u = url.lowercase()
    return when {
        "youtube.com" in u || "youtu.be" in u -> Platform.YOUTUBE
        "tiktok.com" in u -> Platform.TIKTOK
        "instagram.com" in u || "instagr.am" in u -> Platform.INSTAGRAM
        else -> null
    }
}

/** Достаёт первую ссылку из текста (приложения часто делятся «Смотри это: https://…»). */
fun extractUrl(text: String): String? =
    Regex("""https?://\S+""").find(text)?.value?.trimEnd('.', ',', ')', '"', '\'')

data class HistoryItem(
    val title: String,
    val uri: String,
    val audio: Boolean,
    val time: Long,
)

data class UiState(
    val url: String = "",
    val mode: Mode = Mode.VIDEO,
    val quality: Quality = Quality.BEST,
    val phase: Phase = Phase.IDLE,
    val progress: Float = 0f,
    val eta: Long = 0,
    val history: List<HistoryItem> = emptyList(),
)

sealed interface EngineState {
    data object Loading : EngineState
    data object Ready : EngineState
    data class Failed(val message: String) : EngineState
}
