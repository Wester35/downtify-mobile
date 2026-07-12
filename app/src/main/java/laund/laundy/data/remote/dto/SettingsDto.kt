package laund.laundy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    @SerialName("audio_providers")
    val audioProviders: List<String>,

    @SerialName("lyrics_providers")
    val lyricsProviders: List<String>,

    @SerialName("download_lyrics")
    val downloadLyrics: Boolean,

    @SerialName("format")
    val format: String,

    @SerialName("bitrate")
    val bitrate: String,

    @SerialName("output")
    val output: String,

    @SerialName("generate_m3u")
    val generateM3U: Boolean,

    @SerialName("organize_by_artist")
    val organizeByArtist: Boolean
)