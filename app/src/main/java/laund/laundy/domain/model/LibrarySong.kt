package laund.laundy.domain.model

data class LibrarySong(
    val path: String,
    val title: String,
    val streamUrl: String,
    val artist: String,
    val coverUrl: String? = null
)