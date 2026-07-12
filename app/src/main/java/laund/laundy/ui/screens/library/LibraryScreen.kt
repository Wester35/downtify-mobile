package laund.laundy.ui.screens.library

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import laund.laundy.ui.components.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    LazyColumn {
        items(viewModel.songs) { song ->
            Text(
                text = song.path,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}