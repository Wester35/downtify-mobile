package laund.laundy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    @SerialName("id")
    val id: String,

    @SerialName("title")
    val title: String,

    @SerialName("artist")
    val artist: String,

    @SerialName("album")
    val album: String? = null,

    @SerialName("duration")
    val duration: Int? = null,

    @SerialName("thumbnail")
    val thumbnail: String? = null,

    @SerialName("spotify_url")
    val spotifyUrl: String? = null,

    @SerialName("youtube_url")
    val youtubeUrl: String? = null
)