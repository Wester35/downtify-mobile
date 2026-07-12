package laund.laundy.ui.screens.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import laund.laundy.ui.components.SongCard
import laund.laundy.ui.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    LazyColumn {
        items(viewModel.songs) { song ->
            SongCard(
                song = song
            )
        }
    }
}