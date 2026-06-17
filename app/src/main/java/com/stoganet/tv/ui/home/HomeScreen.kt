package com.stoganet.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.stoganet.tv.R
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    when (state) {
        HomeUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        HomeUiState.Error -> {
            val retryLabel = stringResource(R.string.action_retry)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.home_error_message),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onIntent(HomeIntent.Retry) },
                        modifier = Modifier.semantics { contentDescription = retryLabel },
                    ) {
                        Text(text = retryLabel)
                    }
                }
            }
        }

        is HomeUiState.Content -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(state.sections, key = { it.id }) { section ->
                    SectionRow(section = section)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionRow(section: HomeSectionUiState) {
    Column {
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items, key = { it.id }) { item ->
                PosterCard(item = item)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewLoading() {
    HomeScreen(state = HomeUiState.Loading, onIntent = {})
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewError() {
    HomeScreen(state = HomeUiState.Error, onIntent = {})
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewContent() {
    val items = persistentListOf(
        HomeItemUiState("1", "", "Movie One (2020)"),
        HomeItemUiState("2", "", "Movie Two (2021)"),
        HomeItemUiState("3", "", "Movie Three (2022)"),
    )
    HomeScreen(
        state = HomeUiState.Content(
            sections = persistentListOf(
                HomeSectionUiState(
                    "recently_added_movies",
                    R.string.home_section_recently_added_movies,
                    items,
                    hasMore = true,
                ),
                HomeSectionUiState("all_tv", R.string.home_section_all_tv, items, hasMore = false),
            ),
        ),
        onIntent = {},
    )
}
