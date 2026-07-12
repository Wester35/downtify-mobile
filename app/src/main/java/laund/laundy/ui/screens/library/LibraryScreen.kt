package laund.laundy.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import laund.laundy.ui.components.MiniPlayer
import laund.laundy.ui.components.SongCard
import laund.laundy.ui.screens.player.PlayerViewModel

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    Column {
        MiniPlayer(
            song = playerViewModel.currentSong,
            isPlaying = playerViewModel.isPlaying,
            onTogglePlayPause = {
                playerViewModel.togglePlayPause()
            }
        )

        LazyColumn {
            items(libraryViewModel.songs) { song ->
                SongCard(
                    song = song,
                    onClick = {
                        playerViewModel.play(song)
                    }
                )
            }
        }
    }
}