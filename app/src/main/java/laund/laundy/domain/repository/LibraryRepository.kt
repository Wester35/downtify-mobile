package laund.laundy.domain.repository

import laund.laundy.domain.model.LibrarySong

interface LibraryRepository {
    suspend fun getLibrary(): List<LibrarySong>
}