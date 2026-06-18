package com.stoganet.tv.ui.detail

import androidx.compose.runtime.Immutable
import com.stoganet.tv.api.model.MediaType
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class CastMemberUiState(val name: String, val role: String)

sealed interface DetailUiState {
    @Immutable data object Loading : DetailUiState

    @Immutable data object Error : DetailUiState

    @Immutable
    data class Content(
        val title: String,
        val year: Int,
        val mediaType: MediaType,
        val backdropUrl: String?,
        val overview: String,
        val genres: ImmutableList<String>,
        val runtime: String,
        val cast: ImmutableList<CastMemberUiState>,
        val seasons: Int,
        val isPlayable: Boolean,
    ) : DetailUiState
}
