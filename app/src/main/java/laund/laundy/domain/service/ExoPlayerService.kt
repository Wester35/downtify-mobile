package laund.laundy.domain.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerService @Inject constructor(
    @ApplicationContext context: Context
) : PlayerService {
    private val player =
        ExoPlayer.Builder(context)
            .build()

    override fun play(url: String) {
        val item = MediaItem.fromUri(url)

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