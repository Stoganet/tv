package com.stoganet.tv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.stoganet.tv.R
import com.stoganet.tv.api.model.MediaType
import com.stoganet.tv.ui.home.HomeScreen
import com.stoganet.tv.ui.home.HomeViewModel
import com.stoganet.tv.ui.library.LibraryScreen
import com.stoganet.tv.ui.library.LibraryViewModel

@Suppress("LongMethod")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavigationDrawer(
        drawerContent = {
            val homeLabel = stringResource(R.string.nav_home)
            val moviesLabel = stringResource(R.string.nav_movies)
            val tvLabel = stringResource(R.string.nav_tv_shows)

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
            ) {
                NavigationDrawerItem(
                    selected = currentRoute == "home",
                    onClick = { navigateTo("home") },
                    leadingContent = { Icon(Icons.Filled.Home, contentDescription = null) },
                    modifier = Modifier.semantics { contentDescription = homeLabel },
                ) {
                    Text(homeLabel)
                }
                NavigationDrawerItem(
                    selected = currentRoute == "library/movies",
                    onClick = { navigateTo("library/movies") },
                    leadingContent = { Icon(Icons.Filled.Movie, contentDescription = null) },
                    modifier = Modifier.semantics { contentDescription = moviesLabel },
                ) {
                    Text(moviesLabel)
                }
                NavigationDrawerItem(
                    selected = currentRoute == "library/tv",
                    onClick = { navigateTo("library/tv") },
                    leadingContent = { Icon(Icons.Filled.Tv, contentDescription = null) },
                    modifier = Modifier.semantics { contentDescription = tvLabel },
                ) {
                    Text(tvLabel)
                }
            }
        },
    ) {
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
                val vm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.factory(MediaType.MOVIE),
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LibraryScreen(state = state, onIntent = vm::onIntent)
            }
            composable("library/tv") {
                val vm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.factory(MediaType.TV),
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LibraryScreen(state = state, onIntent = vm::onIntent)
            }
        }
    }
}
