package laund.laundy.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import laund.laundy.domain.model.LibrarySong
import laund.laundy.domain.repository.LibraryRepository
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {

    var songs by mutableStateOf<List<LibrarySong>>(emptyList())
        private set

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            songs = repository.getLibrary()
        }
    }
}