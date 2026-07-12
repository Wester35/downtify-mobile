package laund.laundy.data.repository

import laund.laundy.data.remote.DowntifyApi
import laund.laundy.domain.AppConstants
import laund.laundy.domain.model.LibrarySong
import laund.laundy.domain.repository.LibraryRepository
import java.net.URLEncoder
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val api: DowntifyApi
) : LibraryRepository {
    override suspend fun getLibrary(): List<LibrarySong> {
        return api.getLibrary().map { path ->

            val name = path.substringAfterLast('/').removeSuffix(".mp3")
            val parts = name.split(" - ", limit = 2)

            LibrarySong(
                path = path,
                artist = parts.getOrElse(0) { "Unknown" },
                title = parts.getOrElse(1) { name },
                coverUrl = "${AppConstants.API_URL}cover?file=${URLEncoder.encode(path, "UTF-8")}"
            )
        }
    }
}