package laund.laundy.domain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import laund.laundy.MainActivity
import javax.inject.Inject
import androidx.core.graphics.createBitmap

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var imageLoader: ImageLoader

    private var mediaSession: MediaSession? = null
    private var mediaSessionCompat: MediaSessionCompat? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "playback_channel"
        const val NOTIFICATION_ID = 1
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            currentBitmap = null
            updateNotification()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updateNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            updatePlaybackState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MEDIA_SERVICE", "onCreate")

        createNotificationChannel()

        // MediaSession для Media3
        mediaSession = MediaSession.Builder(this, player)
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

        // MediaSessionCompat для Live Notifications
        mediaSessionCompat = MediaSessionCompat(this, "DowntifyPlayer")
        mediaSessionCompat?.isActive = true

        // Устанавливаем начальное состояние
        updatePlaybackState()

        player.addListener(playerListener)

        startForeground(NOTIFICATION_ID, createInitialNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(true)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updatePlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )

        val state = when {
            player.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            player.playbackState == Player.STATE_READY -> PlaybackStateCompat.STATE_PAUSED
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            else -> PlaybackStateCompat.STATE_NONE
        }

        stateBuilder.setState(state, player.currentPosition, 1.0f)
        mediaSessionCompat?.setPlaybackState(stateBuilder.build())
    }

    private fun createInitialNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Laundy")
            .setContentText("Ready to play")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSessionCompat?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        mediaSessionCompat?.release()
        mediaSessionCompat = null
        super.onDestroy()
    }

    private fun updateNotification() {
        val currentItem = player.currentMediaItem

        scope.launch {
            try {
                val notification = if (currentItem != null) {
                    createMediaNotification(currentItem.mediaMetadata)
                } else {
                    createInitialNotification()
                }

                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e("MEDIA_SERVICE", "Error updating notification", e)
            }
        }
    }

    private suspend fun createMediaNotification(metadata: MediaMetadata): android.app.Notification {
        if (currentBitmap == null) {
            metadata.artworkUri?.let { uri ->
                currentBitmap = withContext(Dispatchers.IO) {
                    loadBitmap(uri.toString())
                }
            }
        }

        // Intent для Play/Pause через MediaSessionCompat callback
        val playPauseIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = "TOGGLE_PLAY"
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            100,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent для закрытия
        val closeIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val closePendingIntent = PendingIntent.getService(
            this,
            200,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent для открытия приложения
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .apply {
                setContentTitle(metadata.title ?: "Unknown Title")
                setContentText(metadata.artist ?: "Unknown Artist")
                setContentIntent(contentIntent)

                setSmallIcon(android.R.drawable.ic_media_play)
                if (currentBitmap != null) {
                    setLargeIcon(currentBitmap)
                }

                setOngoing(true)
                setOnlyAlertOnce(true)
                setPriority(NotificationCompat.PRIORITY_LOW)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setSilent(true)

                // Кнопки
                if (player.isPlaying) {
                    addAction(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        playPausePendingIntent
                    )
                } else {
                    addAction(
                        android.R.drawable.ic_media_play,
                        "Play",
                        playPausePendingIntent
                    )
                }

                addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Close",
                    closePendingIntent
                )

                // Главное для Live Notifications - MediaStyle
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat?.sessionToken)
                        .setShowActionsInCompactView(0)
                )
            }
            .build()
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(this)
                .data(url)
                .size(512, 512)
                .build()

            val result = imageLoader.execute(request)
            (result as? SuccessResult)?.drawable?.toBitmap()
        } catch (e: Exception) {
            Log.e("MEDIA_SERVICE", "Error loading bitmap", e)
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MEDIA_SERVICE", "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            "TOGGLE_PLAY" -> {
                if (player.isPlaying) {
                    player.pause()
                    mediaSessionCompat?.setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PAUSED, player.currentPosition, 1.0f)
                            .setActions(
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                        PlaybackStateCompat.ACTION_PLAY or
                                        PlaybackStateCompat.ACTION_PAUSE
                            )
                            .build()
                    )
                } else {
                    player.play()
                    mediaSessionCompat?.setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition, 1.0f)
                            .setActions(
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                        PlaybackStateCompat.ACTION_PLAY or
                                        PlaybackStateCompat.ACTION_PAUSE
                            )
                            .build()
                    )
                }
            }
            "STOP_SERVICE" -> {
                player.stop()
                currentBitmap = null
                mediaSessionCompat?.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                        .build()
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}

fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        return this.bitmap
    }
    val bitmap = createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}