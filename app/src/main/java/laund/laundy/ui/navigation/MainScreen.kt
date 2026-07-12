package laund.laundy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import laund.laundy.ui.viewmodels.LibraryViewModel
import laund.laundy.ui.screens.library.LibraryScreen

@Composable
fun MainScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
){
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController,
                startDestination = "library",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("library") {
                    val libraryViewModel: LibraryViewModel = hiltViewModel()

                    LibraryScreen(
                        viewModel = libraryViewModel
                    )
                }
            }
        }
    }
}