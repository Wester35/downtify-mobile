package laund.laundy.domain.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerService @Inject constructor(
    private val player: ExoPlayer
) : PlayerService {
    override fun play(url: String) {
        val item =
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Название")
                        .setArtist("Исполнитель")
                        .build()
                )
                .build()

        player.setMediaItem(item)

        player.prepare()

        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun stop() {
        player.stop()
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }
}