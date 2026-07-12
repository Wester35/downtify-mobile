package laund.laundy.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteFileResponse(
    @SerialName("deleted")
    val deleted: Boolean,

    @SerialName("error")
    val error: String? = null
)