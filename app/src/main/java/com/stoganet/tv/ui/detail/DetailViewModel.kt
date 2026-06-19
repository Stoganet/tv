package com.stoganet.tv.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stoganet.tv.StoganetApp
import com.stoganet.tv.api.model.LibraryDetail
import com.stoganet.tv.api.model.MediaState
import com.stoganet.tv.data.detail.DetailRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(private val id: String, private val repository: DetailRepository) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.Retry -> {
                if (_state.value is DetailUiState.Loading) return
                loadDetail()
            }
        }
    }

    private fun loadDetail() {
        _state.update { DetailUiState.Loading }
        viewModelScope.launch {
            repository.getDetail(id)
                .onSuccess { detail -> _state.update { detail.toUiState() } }
                .onFailure { _state.update { DetailUiState.Error } }
        }
    }

    private fun LibraryDetail.toUiState() = DetailUiState.Content(
        title = title,
        year = year,
        mediaType = type,
        backdropUrl = backdrop,
        overview = overview,
        genres = genres.toImmutableList(),
        runtime = formatRuntime(runtime),
        cast = cast.map { CastMemberUiState(it.name, it.role) }.toImmutableList(),
        seasons = seasons,
        isPlayable = state == MediaState.PLAYABLE && play != null,
        streamUrl = play?.streamUrl,
    )

    companion object {
        fun formatRuntime(minutes: Int): String {
            if (minutes <= 0) return ""
            val h = minutes / 60
            val m = minutes % 60
            return when {
                h == 0 -> "${m}m"
                m == 0 -> "${h}h"
                else -> "${h}h ${m}m"
            }
        }

        fun factory(id: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StoganetApp
                DetailViewModel(id = id, repository = app.services.detailRepository)
            }
        }
    }
}
