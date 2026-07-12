package laund.laundy.domain.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import laund.laundy.domain.model.LibrarySong
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class ExoPlayerService @Inject constructor(
    private val player: ExoPlayer,
    @param:ApplicationContext private val context: Context
) : PlayerService {
    override fun play(
        url: String,
        song: LibrarySong?
    ) {

        val metadata =
            MediaMetadata.Builder()
                .setTitle(song?.title ?: "Unknown")
                .setArtist(song?.artist ?: "Unknown")
                .setArtworkUri(
                    song?.coverUrl?.toUri()
                )
                .setMediaType(
                    MediaMetadata.MEDIA_TYPE_MUSIC
                )
                .build()


        val mediaItem =
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()


        player.setMediaItem(
            mediaItem
        )

        player.prepare()


        val intent =
            Intent(
                context,
                MediaPlaybackService::class.java
            )


        ContextCompat.startForegroundService(
            context,
            intent
        )


        player.play()
    }
//    override fun play(url: String, song: LibrarySong?) {
//        Log.d("ExoPlayerService", "play called with song: ${song?.title} - ${song?.artist}")
//
//        // Запускаем сервис
//        val intent = Intent(context, MediaPlaybackService::class.java)
//        ContextCompat.startForegroundService(context, intent)
//
//        val metadata = MediaMetadata.Builder().apply {
//            if (song != null) {
//                setTitle(song.title)
//                setArtist(song.artist)
//                song.coverUrl?.let { coverUrl ->
//                    try {
//                        setArtworkUri(coverUrl.toUri())
//                    } catch (e: Exception) {
//                        Log.e("ExoPlayerService", "Invalid cover URL", e)
//                    }
//                }
//            } else {
//                setTitle("Unknown Title")
//                setArtist("Unknown Artist")
//            }
//            setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
//            setIsPlayable(true)
//        }.build()
//
//        val mediaItem = MediaItem.Builder()
//            .setUri(url)
//            .setMediaMetadata(metadata)
//            .build()
//
//        player.setMediaItem(mediaItem)
//        player.prepare()
//        player.play()
//    }

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