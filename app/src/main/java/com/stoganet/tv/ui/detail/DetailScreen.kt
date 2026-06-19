package com.stoganet.tv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.stoganet.tv.R
import com.stoganet.tv.api.model.MediaType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val GRADIENT_MID_STOP = 0.55f
private const val GRADIENT_MID_ALPHA = 0.85f
private const val GRADIENT_START_ALPHA = 0.90f
private const val PANEL_WIDTH_FRACTION = 0.52f

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit,
    onNavigateToPlayer: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DetailUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        DetailUiState.Error -> {
            val retryLabel = stringResource(R.string.action_retry)
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.detail_error_message),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { onIntent(DetailIntent.Retry) },
                        modifier = Modifier.semantics { contentDescription = retryLabel },
                    ) { Text(retryLabel) }
                }
            }
        }

        is DetailUiState.Content -> DetailContent(
            state = state,
            onNavigateToPlayer = onNavigateToPlayer,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailContent(
    state: DetailUiState.Content,
    onNavigateToPlayer: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        DetailBackdrop(backdropUrl = state.backdropUrl)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = GRADIENT_START_ALPHA),
                            GRADIENT_MID_STOP to Color.Black.copy(alpha = GRADIENT_MID_ALPHA),
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )
        DetailMetadataPanel(
            state = state,
            focusRequester = focusRequester,
            onNavigateToPlayer = onNavigateToPlayer,
        )
    }
}

@Composable
private fun DetailBackdrop(backdropUrl: String?) {
    if (backdropUrl != null) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Suppress("ForbiddenComment")
@Composable
private fun DetailMetadataPanel(
    state: DetailUiState.Content,
    focusRequester: FocusRequester,
    onNavigateToPlayer: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(PANEL_WIDTH_FRACTION)
            .padding(horizontal = 48.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))

        val metaParts = buildList {
            add(state.year.toString())
            if (state.genres.isNotEmpty()) add(state.genres.joinToString(", "))
            when (state.mediaType) {
                MediaType.MOVIE -> if (state.runtime.isNotEmpty()) add(state.runtime)

                MediaType.TV -> {
                    // TODO: episode browsing — seasons/episodes rows go here
                    if (state.seasons > 0) {
                        add(pluralStringResource(R.plurals.detail_seasons_label, state.seasons, state.seasons))
                    }
                }
            }
        }
        Text(
            text = metaParts.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.overview,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 4,
        )
        Spacer(Modifier.height(24.dp))

        DetailPlayButton(
            title = state.title,
            isPlayable = state.isPlayable,
            focusRequester = focusRequester,
            onNavigateToPlayer = { onNavigateToPlayer(state.streamUrl) },
        )

        if (state.cast.isNotEmpty()) {
            CastSection(cast = state.cast)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailPlayButton(
    title: String,
    isPlayable: Boolean,
    focusRequester: FocusRequester,
    onNavigateToPlayer: () -> Unit,
) {
    val playLabel: String
    val playDesc: String
    if (isPlayable) {
        playLabel = stringResource(R.string.detail_play_button)
        playDesc = stringResource(R.string.detail_play_content_description, title)
    } else {
        playLabel = stringResource(R.string.detail_not_available_button)
        playDesc = stringResource(R.string.detail_not_available_content_description, title)
    }
    Button(
        onClick = { if (isPlayable) onNavigateToPlayer() },
        enabled = isPlayable,
        modifier = Modifier
            .focusRequester(focusRequester)
            .semantics { contentDescription = playDesc },
    ) { Text(playLabel) }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CastSection(cast: ImmutableList<CastMemberUiState>) {
    Column {
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.detail_cast_label),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(cast) { member ->
                Column {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewLoading() {
    DetailScreen(state = DetailUiState.Loading, onIntent = {}, onNavigateToPlayer = {})
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewError() {
    DetailScreen(state = DetailUiState.Error, onIntent = {}, onNavigateToPlayer = {})
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewMovieContent() {
    DetailScreen(
        state = DetailUiState.Content(
            title = "The Matrix",
            year = 1999,
            mediaType = MediaType.MOVIE,
            backdropUrl = null,
            overview = "A computer hacker learns that the world he knows is a simulated reality.",
            genres = persistentListOf("Action", "Sci-Fi"),
            runtime = "2h 16m",
            cast = persistentListOf(
                CastMemberUiState("Keanu Reeves", "Actor"),
                CastMemberUiState("Laurence Fishburne", "Actor"),
            ),
            seasons = 0,
            isPlayable = true,
        ),
        onIntent = {},
        onNavigateToPlayer = {},
    )
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewTvContent() {
    DetailScreen(
        state = DetailUiState.Content(
            title = "Breaking Bad",
            year = 2008,
            mediaType = MediaType.TV,
            backdropUrl = null,
            overview = "A chemistry teacher turns to manufacturing drugs after a cancer diagnosis.",
            genres = persistentListOf("Drama", "Crime"),
            runtime = "",
            cast = persistentListOf(CastMemberUiState("Bryan Cranston", "Actor")),
            seasons = 5,
            isPlayable = false,
        ),
        onIntent = {},
        onNavigateToPlayer = {},
    )
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewNotPlayable() {
    DetailScreen(
        state = DetailUiState.Content(
            title = "The Matrix",
            year = 1999,
            mediaType = MediaType.MOVIE,
            backdropUrl = null,
            overview = "A computer hacker learns the truth.",
            genres = persistentListOf("Action"),
            runtime = "2h 16m",
            cast = persistentListOf(),
            seasons = 0,
            isPlayable = false,
        ),
        onIntent = {},
        onNavigateToPlayer = {},
    )
}
