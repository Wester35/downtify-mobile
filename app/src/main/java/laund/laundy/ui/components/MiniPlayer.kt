package laund.laundy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import laund.laundy.domain.model.LibrarySong

@Composable
fun MiniPlayer(
    song: LibrarySong?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit
){
    if(song == null)
        return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ){
            Text(song.title)
            Text(song.artist)
        }

        IconButton(
            onClick = onTogglePlayPause
        ){
            Icon(
                imageVector =
                    if(isPlaying)
                        Icons.Default.Pause
                    else
                        Icons.Default.PlayArrow,
                null
            )
        }
    }
}