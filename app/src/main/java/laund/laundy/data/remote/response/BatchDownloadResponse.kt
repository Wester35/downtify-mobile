package laund.laundy.data.remote.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BatchDownloadResponse(
    @SerialName("job_ids")
    val jobIds: List<String>,

    @SerialName("count")
    val count: Int
)