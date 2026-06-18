package com.stoganet.tv.ui.detail

import com.stoganet.tv.api.model.CastMember
import com.stoganet.tv.api.model.LibraryDetail
import com.stoganet.tv.api.model.MediaState
import com.stoganet.tv.api.model.MediaType
import com.stoganet.tv.api.model.PlayInfo
import com.stoganet.tv.data.detail.DetailRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val repository = mockk<DetailRepository>()

    @BeforeEach fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeDetail(
        play: PlayInfo? = PlayInfo(
            jellyfinItemId = "jf-uuid",
            jellyfinBaseUrl = "https://jf.example.com",
            jellyfinAccessToken = "tok",
            jellyfinUserId = "uid",
        ),
    ) = LibraryDetail(
        id = "tmdb:movie:603",
        title = "The Matrix",
        year = 1999,
        type = MediaType.MOVIE,
        poster = "https://img/poster",
        backdrop = "https://img/backdrop",
        overview = "A computer hacker learns the truth.",
        state = if (play != null) MediaState.PLAYABLE else MediaState.DOWNLOADING,
        genres = listOf("Action", "Sci-Fi"),
        runtime = 136,
        cast = listOf(CastMember(name = "Keanu Reeves", role = "Actor")),
        seasons = 0,
        play = play,
    )

    @Test
    fun `loads Content on success`() = runTest {
        coEvery { repository.getDetail("id1") } returns Result.success(fakeDetail())
        val vm = DetailViewModel(id = "id1", repository = repository)

        val state = vm.state.value
        assertInstanceOf(DetailUiState.Content::class.java, state)
        state as DetailUiState.Content
        assertEquals("The Matrix", state.title)
        assertEquals(1999, state.year)
        assertEquals("https://img/backdrop", state.backdropUrl)
        assertEquals(listOf("Action", "Sci-Fi"), state.genres.toList())
        assertEquals("2h 16m", state.runtime)
        assertEquals(1, state.cast.size)
        assertEquals("Keanu Reeves", state.cast[0].name)
        assertTrue(state.isPlayable)
    }

    @Test
    fun `shows Error on failure`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.failure(RuntimeException("fail"))
        val vm = DetailViewModel(id = "id1", repository = repository)

        assertInstanceOf(DetailUiState.Error::class.java, vm.state.value)
    }

    @Test
    fun `Retry reloads from scratch`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.failure(RuntimeException("fail"))
        val vm = DetailViewModel(id = "id1", repository = repository)

        coEvery { repository.getDetail(any()) } returns Result.success(fakeDetail())
        vm.onIntent(DetailIntent.Retry)

        assertInstanceOf(DetailUiState.Content::class.java, vm.state.value)
        coVerify(exactly = 2) { repository.getDetail("id1") }
    }

    @Test
    fun `Retry no-ops when already Loading`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.success(fakeDetail())
        val vm = DetailViewModel(id = "id1", repository = repository)
        vm.onIntent(DetailIntent.Retry)
        coVerify(atLeast = 1) { repository.getDetail("id1") }
    }

    @Test
    fun `isPlayable is false when play is null`() = runTest {
        coEvery { repository.getDetail(any()) } returns Result.success(fakeDetail(play = null))
        val vm = DetailViewModel(id = "id1", repository = repository)

        val state = vm.state.value as DetailUiState.Content
        assertFalse(state.isPlayable)
    }

    @Test
    fun `formatRuntime formats hours and minutes`() {
        assertEquals("2h 16m", DetailViewModel.formatRuntime(136))
    }

    @Test
    fun `formatRuntime formats minutes only`() {
        assertEquals("45m", DetailViewModel.formatRuntime(45))
    }

    @Test
    fun `formatRuntime formats whole hours`() {
        assertEquals("2h", DetailViewModel.formatRuntime(120))
    }

    @Test
    fun `formatRuntime returns empty string for zero`() {
        assertEquals("", DetailViewModel.formatRuntime(0))
    }
}
