package com.stoganet.tv.ui.library

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
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.stoganet.tv.R
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryScreenTest {

    private fun str(@StringRes id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    @Test
    fun loadingState_showsProgressIndicator() = runComposeUiTest {
        setContent { LibraryScreen(state = LibraryUiState.Loading, onIntent = {}) }

        onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() = runComposeUiTest {
        setContent { LibraryScreen(state = LibraryUiState.Error, onIntent = {}) }

        onNodeWithText(str(R.string.library_error_message)).assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() = runComposeUiTest {
        setContent { LibraryScreen(state = LibraryUiState.Error, onIntent = {}) }

        onNodeWithContentDescription(str(R.string.action_retry)).assertIsDisplayed()
    }

    @Test
    fun retryButton_triggersRetryIntent() = runComposeUiTest {
        var intentFired = false
        setContent {
            LibraryScreen(
                state = LibraryUiState.Error,
                onIntent = { if (it == LibraryIntent.Retry) intentFired = true },
            )
        }

        onNodeWithContentDescription(str(R.string.action_retry)).requestFocus()
        onNodeWithContentDescription(str(R.string.action_retry)).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(intentFired)
    }

    @Test
    fun contentState_showsItemByContentDescription() = runComposeUiTest {
        setContent {
            LibraryScreen(
                state = LibraryUiState.Content(
                    items = persistentListOf(
                        LibraryItemUiState(
                            id = "1",
                            posterUrl = "",
                            contentDescription = "Inception (2010)",
                        ),
                    ),
                    hasMore = false,
                    isLoadingMore = false,
                ),
                onIntent = {},
            )
        }

        onNodeWithContentDescription("Inception (2010)").assertIsDisplayed()
    }
}
