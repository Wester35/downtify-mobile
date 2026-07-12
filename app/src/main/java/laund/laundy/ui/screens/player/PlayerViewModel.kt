package laund.laundy.ui.screens.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import laund.laundy.domain.model.LibrarySong
import laund.laundy.domain.service.PlayerService
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: PlayerService
) : ViewModel()
{
    var currentSong by mutableStateOf<LibrarySong?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    fun play(song: LibrarySong){
        currentSong = song

        player.play(
            song.streamUrl
        )

        isPlaying = true
    }

    fun pause(){
        player.pause()

        isPlaying = false
    }

    fun resume(){
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
}