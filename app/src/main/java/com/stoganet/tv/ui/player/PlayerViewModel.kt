package com.stoganet.tv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.stoganet.tv.StoganetApp
import com.stoganet.tv.api.model.PlayInfo
import com.stoganet.tv.data.detail.DetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val id: String,
    private val repository: DetailRepository,
    val player: ExoPlayer,
    private val mediaSession: MediaSession,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        loadAndPrepare()
    }

    private fun loadAndPrepare() {
        viewModelScope.launch {
            repository.getDetail(id)
                .onSuccess { detail ->
                    val play = detail.play
                    if (play == null) {
                        _state.update { PlayerUiState.Error }
                        return@onSuccess
                    }
                    player.setMediaItem(MediaItem.fromUri(buildStreamUrl(play)))
                    player.prepare()
                    _state.update { PlayerUiState.Ready }
                }
                .onFailure { _state.update { PlayerUiState.Error } }
        }
    }

    override fun onCleared() {
        mediaSession.release()
        player.release()
    }

    companion object {
        fun buildStreamUrl(play: PlayInfo): String = "${play.jellyfinBaseUrl}/Videos/${play.jellyfinItemId}/stream" +
            "?api_key=${play.jellyfinAccessToken}" +
            "&UserId=${play.jellyfinUserId}" +
            "&static=true"

        fun factory(id: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StoganetApp
                val player = ExoPlayer.Builder(app).build()
                val mediaSession = MediaSession.Builder(app, player).build()
                PlayerViewModel(
                    id = id,
                    repository = app.services.detailRepository,
                    player = player,
                    mediaSession = mediaSession,
                )
            }
        }
    }
}
