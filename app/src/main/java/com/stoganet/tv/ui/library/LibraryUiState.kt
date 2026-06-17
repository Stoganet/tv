package com.stoganet.tv.ui.library

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class LibraryItemUiState(val id: String, val posterUrl: String, val contentDescription: String)

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    @Immutable
    data class Content(
        val items: ImmutableList<LibraryItemUiState>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean,
    ) : LibraryUiState

    data object Error : LibraryUiState
}
