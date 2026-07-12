package laund.laundy.data.remote.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import laund.laundy.data.remote.dto.SongDto

@Serializable
data class DownloadBatchRequest(
    @SerialName("songs")
    val songs: List<SongDto>,

    @SerialName("playlist_url")
    val playlistUrl: String? = null,

    @SerialName("generate_m3u")
    val generateM3U: Boolean = true
)