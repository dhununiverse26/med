package com.example.util

import java.util.regex.Pattern

fun isYouTubeUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    return lower.contains("youtube.com") || lower.contains("youtu.be")
}

fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F|shorts\\/)[^#\\&\\?\\n]*"
    val compiledPattern = Pattern.compile(pattern)
    val matcher = compiledPattern.matcher(url)
    return if (matcher.find()) {
        matcher.group()
    } else {
        null
    }
}

object YouTubeHelper {
    fun extractVideoId(url: String): String? = extractYouTubeVideoId(url)
}
