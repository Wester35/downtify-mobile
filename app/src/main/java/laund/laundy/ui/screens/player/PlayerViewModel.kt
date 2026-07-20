package laund.laundy.ui.screens.player

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

    var downloadingSongs by mutableStateOf<Set<String>>(emptySet())
        private set

    var downloadedSongs by mutableStateOf<Set<String>>(emptySet())
        private set

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

    fun isSongDownloaded(song: LibrarySong): Boolean {
        return downloadManager.isDownloaded(song)
    }

    fun isSongDownloading(song: LibrarySong): Boolean {
        return downloadingSongs.contains(song.path)
    }

    fun downloadSong(song: LibrarySong) {
        if (downloadingSongs.contains(song.path)) return

        downloadingSongs = downloadingSongs + song.path

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.downloadSong(song)
                }

                result.onSuccess {
                    downloadedSongs = downloadedSongs + song.path
                }.onFailure { error ->
                }
            } finally {
                downloadingSongs = downloadingSongs - song.path
            }
        }
    }

    fun deleteSong(song: LibrarySong) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (downloadManager.deleteSong(song)) {
                    downloadedSongs = downloadedSongs - song.path
                }
            }
        }
    }

    fun refreshDownloadedStatus(songs: List<LibrarySong>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val downloaded = songs.filter { downloadManager.isDownloaded(it) }
                downloadedSongs = downloaded.map { it.path }.toSet()
            }
        }
    }
}