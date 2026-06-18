package com.stoganet.tv.ui.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.stoganet.tv.api.model.CastMember
import com.stoganet.tv.api.model.LibraryDetail
import com.stoganet.tv.api.model.MediaState
import com.stoganet.tv.api.model.MediaType
import com.stoganet.tv.api.model.PlayInfo
import com.stoganet.tv.data.detail.DetailRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerViewModelTest {

    private val repository = mockk<DetailRepository>()
    private val player = mockk<ExoPlayer>(relaxed = true)
    private val mediaSession = mockk<MediaSession>(relaxed = true)

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeDetail(play: PlayInfo? = fakePlay()) = LibraryDetail(
        id = "tmdb:movie:603",
        title = "The Matrix",
        year = 1999,
        type = MediaType.MOVIE,
        poster = "https://img/poster",
        overview = "A computer hacker learns the truth.",
        state = if (play != null) MediaState.PLAYABLE else MediaState.DOWNLOADING,
        genres = listOf("Action"),
        runtime = 136,
        cast = listOf(CastMember(name = "Keanu Reeves", role = "Actor")),
        seasons = 0,
        play = play,
    )

    private fun fakePlay() = PlayInfo(
        jellyfinItemId = "abc123",
        jellyfinBaseUrl = "https://jf.example.com",
        jellyfinAccessToken = "mytoken",
        jellyfinUserId = "user1",
    )

    @Test
    fun `transitions to Ready when play info available`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.success(fakeDetail())
        val vm = PlayerViewModel(id = "id1", repository = repository, player = player, mediaSession = mediaSession)

        assertTrue(vm.state.value is PlayerUiState.Ready)
        verify { player.setMediaItem(any<MediaItem>()) }
        verify { player.prepare() }
    }

    @Test
    fun `transitions to Error on fetch failure`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.failure(RuntimeException("fail"))
        val vm = PlayerViewModel(id = "id1", repository = repository, player = player, mediaSession = mediaSession)

        assertTrue(vm.state.value is PlayerUiState.Error)
    }

    @Test
    fun `transitions to Error when play is null`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.success(fakeDetail(play = null))
        val vm = PlayerViewModel(id = "id1", repository = repository, player = player, mediaSession = mediaSession)

        assertTrue(vm.state.value is PlayerUiState.Error)
    }

    @Test
    fun `buildStreamUrl constructs correct URL`() {
        val url = PlayerViewModel.buildStreamUrl(fakePlay())
        assertEquals(
            "https://jf.example.com/Videos/abc123/stream?api_key=mytoken&UserId=user1&static=true",
            url,
        )
    }
}
