package laund.laundy.domain.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import laund.laundy.MainActivity
import javax.inject.Inject


@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(
            "MEDIA_SERVICE",
            "MediaPlaybackService CREATED"
        )
        mediaSession =
            MediaSession.Builder(
                this,
                player
            )
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setId("DowntifyPlayer")
                .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        Log.d(
            "MEDIA_SERVICE",
            "getSession"
        )
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()

        super.onDestroy()
    }
}