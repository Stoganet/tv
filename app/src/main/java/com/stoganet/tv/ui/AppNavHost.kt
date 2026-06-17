package com.stoganet.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stoganet.tv.api.model.MediaType
import com.stoganet.tv.ui.home.HomeScreen
import com.stoganet.tv.ui.home.HomeViewModel
import com.stoganet.tv.ui.library.LibraryScreen
import com.stoganet.tv.ui.library.LibraryViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            val state by vm.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onIntent = vm::onIntent,
                onNavigateTo = { route -> navController.navigate(route) },
            )
        }
        composable("library/movies") {
            val vm: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(MediaType.MOVIE))
            val state by vm.state.collectAsStateWithLifecycle()
            LibraryScreen(state = state, onIntent = vm::onIntent)
        }
        composable("library/tv") {
            val vm: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(MediaType.TV))
            val state by vm.state.collectAsStateWithLifecycle()
            LibraryScreen(state = state, onIntent = vm::onIntent)
        }
    }
}
