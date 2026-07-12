package laund.laundy.domain.service

interface PlayerService {
    fun play(url: String)

    fun pause()

    fun resume()

    fun stop()

    fun isPlaying(): Boolean
}