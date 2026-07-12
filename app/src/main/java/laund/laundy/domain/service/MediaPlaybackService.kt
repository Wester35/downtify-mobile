package laund.laundy.domain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
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

@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var imageLoader: ImageLoader

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Кэш для обложки
    private var currentBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "playback_channel"
        const val NOTIFICATION_ID = 1
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            updateNotification()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MEDIA_SERVICE", "onCreate")

        createNotificationChannel()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        player.addListener(playerListener)

        // Показываем начальное уведомление
        startForeground(NOTIFICATION_ID, createInitialNotification())
    }

    private fun createNotificationChannel() {
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

    private fun createInitialNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Laundy")
            .setContentText("Ready to play")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
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
        super.onDestroy()
    }

    private fun updateNotification() {
        val currentItem = player.currentMediaItem
        if (currentItem == null) {
            // Если нет трека, показываем начальное уведомление
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, createInitialNotification())
            return
        }

        scope.launch {
            try {
                val notification = createNotification()
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e("MEDIA_SERVICE", "Error updating notification", e)
            }
        }
    }

    private suspend fun createNotification(): android.app.Notification {
        val metadata = player.currentMediaItem?.mediaMetadata

        // Загружаем обложку если еще не загружена
        if (currentBitmap == null) {
            metadata?.artworkUri?.let { uri ->
                currentBitmap = withContext(Dispatchers.IO) {
                    loadBitmap(uri.toString())
                }
            }
        }

        // Создаем Intent для кнопки play/pause
        val toggleIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = "TOGGLE_PLAY_PAUSE"
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Создаем Intent для закрытия
        val closeIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = "CLOSE"
        }
        val closePendingIntent = PendingIntent.getService(
            this,
            1,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .apply {
                // Заголовок и исполнитель
                setContentTitle(metadata?.title ?: "Unknown Title")
                setContentText(metadata?.artist ?: "Unknown Artist")

                // Иконка и обложка
                setSmallIcon(android.R.drawable.ic_media_play)
                currentBitmap?.let { setLargeIcon(it) }

                // Стиль уведомления
                setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(currentBitmap)
                        .bigLargeIcon(null as Bitmap?)
                        .setSummaryText(metadata?.artist?.toString())
                )

                // Настройки
                setOngoing(true)
                setOnlyAlertOnce(true)
                setPriority(NotificationCompat.PRIORITY_LOW)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Кнопка Play/Pause с правильной иконкой
                if (player.isPlaying) {
                    addAction(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        togglePendingIntent
                    )
                } else {
                    addAction(
                        android.R.drawable.ic_media_play,
                        "Play",
                        togglePendingIntent
                    )
                }

                // Кнопка закрытия
                addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Close",
                    closePendingIntent
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
        when (intent?.action) {
            "TOGGLE_PLAY_PAUSE" -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
            "CLOSE" -> {
                player.stop()
                currentBitmap = null
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