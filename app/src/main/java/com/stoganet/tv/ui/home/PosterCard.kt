package com.stoganet.tv.ui.home

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage

private val PosterWidth = 120.dp
private const val PosterAspectRatio = 2f / 3f

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(item: HomeItemUiState, modifier: Modifier = Modifier) {
    Card(
        onClick = {},
        modifier = modifier
            .width(PosterWidth)
            .aspectRatio(PosterAspectRatio)
            .semantics { contentDescription = item.contentDescription },
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun PreviewPosterCard() {
    PosterCard(
        item = HomeItemUiState(
            id = "tmdb:movie:603",
            posterUrl = "",
            contentDescription = "The Matrix (1999)",
        ),
    )
}
