package laund.laundy.ui.screens.player

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import laund.laundy.domain.model.LibrarySong
import laund.laundy.domain.service.DownloadManager
import laund.laundy.domain.service.PlayerService
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: PlayerService,
    private val downloadManager: DownloadManager
) : ViewModel() {
    var currentSong by mutableStateOf<LibrarySong?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var downloadingSongs by mutableStateOf(emptySet<String>())
        private set

    // Кэш статуса скачивания: path -> true/false
    private var downloadStatusCache by mutableStateOf(mapOf<String, Boolean>())

    init {
        refreshAllDownloadStatus()
    }

    fun play(song: LibrarySong) {
        currentSong = song
        player.play(song.streamUrl, song)
        isPlaying = true
    }

    fun pause() {
        player.pause()
        isPlaying = false
    }

    fun resume() {
        player.resume()
        isPlaying = true
    }

    fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun isSongDownloading(song: LibrarySong): Boolean {
        return downloadingSongs.contains(song.path)
    }

    fun isSongDownloaded(song: LibrarySong): Boolean {
        // Сначала проверяем кэш
        val cached = downloadStatusCache[song.path]
        if (cached != null) {
            return cached
        }

        // Если нет в кэше, проверяем напрямую
        val isDownloaded = downloadManager.isDownloaded(song)
        downloadStatusCache = downloadStatusCache + (song.path to isDownloaded)

        Log.d("PlayerViewModel", "🎵 ${song.title}: ${if (isDownloaded) "✅ СКАЧАНА" else "❌ НЕ скачана"}")
        return isDownloaded
    }

    fun refreshAllDownloadStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val files = downloadManager.getDownloadedSongs()
                Log.d("PlayerViewModel", "📁 Файлы в Downloads: $files")
            }
            // Сбрасываем кэш, чтобы он обновился при следующей проверке
            downloadStatusCache = emptyMap()
            Log.d("PlayerViewModel", "🔄 Кэш статуса скачивания сброшен")
        }
    }

    fun downloadSong(song: LibrarySong) {
        if (downloadingSongs.contains(song.path)) {
            Log.d("PlayerViewModel", "⏳ Песня уже скачивается: ${song.title}")
            return
        }

        downloadingSongs = downloadingSongs + song.path

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.downloadSong(song)
                }

                result.onSuccess {
                    Log.d("PlayerViewModel", "✅ Скачано успешно: ${song.title}")
                    // Обновляем кэш
                    downloadStatusCache = downloadStatusCache + (song.path to true)
                    // Обновляем весь список для синхронизации
                    refreshAllDownloadStatus()
                }

                result.onFailure { error ->
                    Log.e("PlayerViewModel", "❌ Ошибка скачивания ${song.title}: ${error.message}")
                }
            } finally {
                downloadingSongs = downloadingSongs - song.path
            }
        }
    }

    fun deleteSong(song: LibrarySong) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                downloadManager.deleteSong(song)
            }

            if (deleted) {
                // Обновляем кэш
                downloadStatusCache = downloadStatusCache + (song.path to false)
                Log.d("PlayerViewModel", "🗑️ Удалено: ${song.title}")
                // Обновляем весь список
                refreshAllDownloadStatus()
            } else {
                Log.d("PlayerViewModel", "❌ Не удалось удалить: ${song.title}")
            }
        }
    }
}