package laund.laundy.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueueClearResponse(
    @SerialName("cleared")
    val cleared: Boolean? = null,

    @SerialName("removed")
    val removed: Boolean? = null
)