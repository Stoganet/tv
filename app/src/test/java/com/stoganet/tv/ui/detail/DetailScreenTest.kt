package com.stoganet.tv.ui.detail

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.stoganet.tv.R
import com.stoganet.tv.api.model.MediaType
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DetailScreenTest {

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun strPlural(@StringRes id: Int, count: Int): String =
        ApplicationProvider.getApplicationContext<Context>().resources.getQuantityString(id, count, count)

    private fun fakeMovieContent(isPlayable: Boolean = true) = DetailUiState.Content(
        title = "The Matrix",
        year = 1999,
        mediaType = MediaType.MOVIE,
        backdropUrl = null,
        overview = "A computer hacker learns the truth.",
        genres = persistentListOf("Action", "Sci-Fi"),
        runtime = "2h 16m",
        cast = persistentListOf(CastMemberUiState("Keanu Reeves", "Actor")),
        seasons = 0,
        isPlayable = isPlayable,
    )

    private fun fakeTvContent() = DetailUiState.Content(
        title = "Breaking Bad",
        year = 2008,
        mediaType = MediaType.TV,
        backdropUrl = null,
        overview = "A chemistry teacher turns to crime.",
        genres = persistentListOf("Drama", "Crime"),
        runtime = "",
        cast = persistentListOf(CastMemberUiState("Bryan Cranston", "Actor")),
        seasons = 3,
        isPlayable = false,
    )

    @Test
    fun loadingState_showsProgressIndicator() = runComposeUiTest {
        setContent { DetailScreen(state = DetailUiState.Loading, onIntent = {}, onNavigateToPlayer = {}) }

        onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate),
        ).assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() = runComposeUiTest {
        setContent { DetailScreen(state = DetailUiState.Error, onIntent = {}, onNavigateToPlayer = {}) }

        onNodeWithContentDescription(str(R.string.action_retry)).assertIsDisplayed()
    }

    @Test
    fun retryButton_triggersRetryIntent() = runComposeUiTest {
        var intentFired = false
        setContent {
            DetailScreen(
                state = DetailUiState.Error,
                onIntent = { if (it == DetailIntent.Retry) intentFired = true },
                onNavigateToPlayer = {},
            )
        }

        onNodeWithContentDescription(str(R.string.action_retry)).requestFocus()
        onNodeWithContentDescription(str(R.string.action_retry)).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(intentFired)
    }

    @Test
    fun contentState_movie_showsTitle() = runComposeUiTest {
        setContent { DetailScreen(state = fakeMovieContent(), onIntent = {}, onNavigateToPlayer = {}) }

        onNodeWithText("The Matrix").assertIsDisplayed()
    }

    @Test
    fun contentState_movie_showsPlayButton() = runComposeUiTest {
        setContent { DetailScreen(state = fakeMovieContent(isPlayable = true), onIntent = {}, onNavigateToPlayer = {}) }

        onNodeWithContentDescription(str(R.string.detail_play_content_description, "The Matrix")).assertIsDisplayed()
    }

    @Test
    fun contentState_movie_playButton_invokesCallback() = runComposeUiTest {
        var played = false
        setContent {
            DetailScreen(
                state = fakeMovieContent(isPlayable = true),
                onIntent = {},
                onNavigateToPlayer = { played = true },
            )
        }

        val playDesc = str(R.string.detail_play_content_description, "The Matrix")
        onNodeWithContentDescription(playDesc).requestFocus()
        onNodeWithContentDescription(playDesc).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(played)
    }

    @Test
    fun contentState_tv_showsSeasonsLabel() = runComposeUiTest {
        setContent { DetailScreen(state = fakeTvContent(), onIntent = {}, onNavigateToPlayer = {}) }

        onNodeWithText(strPlural(R.plurals.detail_seasons_label, 3), substring = true).assertIsDisplayed()
    }

    @Test
    fun contentState_showsCastMemberName() = runComposeUiTest {
        setContent { DetailScreen(state = fakeMovieContent(), onIntent = {}, onNavigateToPlayer = {}) }

        onNodeWithText("Keanu Reeves").assertIsDisplayed()
    }

    @Test
    fun contentState_movie_notPlayable_showsNotAvailableButton() = runComposeUiTest {
        setContent {
            DetailScreen(
                state = fakeMovieContent(isPlayable = false),
                onIntent = {},
                onNavigateToPlayer = {},
            )
        }

        onNodeWithContentDescription(
            str(R.string.detail_not_available_content_description, "The Matrix"),
        ).assertIsDisplayed()
    }

    @Test
    fun contentState_movie_playButton_passesStreamUrlToCallback() = runComposeUiTest {
        var receivedUrl: String? = null
        val expectedUrl = "https://api.stoganet.com/stream/abc"
        setContent {
            DetailScreen(
                state = fakeMovieContent(isPlayable = true).copy(streamUrl = expectedUrl),
                onIntent = {},
                onNavigateToPlayer = { url -> receivedUrl = url },
            )
        }

        val playDesc = str(R.string.detail_play_content_description, "The Matrix")
        onNodeWithContentDescription(playDesc).requestFocus()
        onNodeWithContentDescription(playDesc).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertEquals(expectedUrl, receivedUrl)
    }
}
