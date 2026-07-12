package laund.laundy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadJobDto(
    @SerialName("song")
    val song: SongDto,

    @SerialName("progress")
    val progress: Float,

    @SerialName("message")
    val message: String,

    @SerialName("status")
    val status: String,

    @SerialName("filename")
    val filename: String? = null
)