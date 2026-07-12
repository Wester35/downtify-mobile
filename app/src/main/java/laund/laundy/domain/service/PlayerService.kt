package laund.laundy.domain.service

import laund.laundy.domain.model.LibrarySong

interface PlayerService {
    fun play(url: String, song: LibrarySong? = null)
    fun pause()
    fun resume()
    fun stop()
    fun isPlaying(): Boolean
}