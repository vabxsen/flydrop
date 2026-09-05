package com.flydrop.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.theme.FlyDropTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectionFailureKeepsTheIdAndOffersRetry() {
        val message =
            "Could not reach FlyDrop. Check your internet connection, then tap Save to retry."
        composeRule.setContent {
            FlyDropTheme {
                ProfileScreen(
                    user = FlyUser(
                        id = "test-user",
                        name = "Test User",
                        flyId = "fly#oldname",
                        avatarSeed = 1,
                    ),
                    signedIn = true,
                    flyIdState = ProfileUiState(
                        flyId = "fly#oldname",
                        canEdit = true,
                        editorOpen = true,
                        input = "newname",
                        submissionError = message,
                    ),
                )
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Save").assertDoesNotExist()
        composeRule.onNodeWithText("newname").assertIsDisplayed()
    }

    @Test
    fun signOutAsksBeforeEndingTheSession() {
        var signedOut = 0
        composeRule.setContent {
            FlyDropTheme {
                ProfileScreen(
                    user = signedInUser,
                    signedIn = true,
                    onSignOut = { signedOut++ },
                )
            }
        }

        composeRule.onNodeWithText("Sign out").performClick()

        // The prompt is up and nothing has happened yet.
        composeRule.onNodeWithText("Sign out of FlyDrop?").assertIsDisplayed()
        assertEquals(0, signedOut)

        // Backing out leaves the session alone.
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Sign out of FlyDrop?").assertDoesNotExist()
        assertEquals(0, signedOut)

        // Confirming is what actually signs out.
        composeRule.onNodeWithText("Sign out").performClick()
        composeRule.onAllNodesWithText("Sign out")[1].performClick()
        assertEquals(1, signedOut)
    }

    /** Signing in is reversible, so it is not worth a confirmation. */
    @Test
    fun signInActsImmediately() {
        var pressed = 0
        composeRule.setContent {
            FlyDropTheme {
                ProfileScreen(
                    user = signedInUser,
                    signedIn = false,
                    onSignOut = { pressed++ },
                )
            }
        }

        composeRule.onNodeWithText("Sign in").performClick()

        assertEquals(1, pressed)
        composeRule.onNodeWithText("Sign out of FlyDrop?").assertDoesNotExist()
    }

    private val signedInUser = FlyUser(
        id = "test-user",
        name = "Test User",
        flyId = "fly#tester",
        avatarSeed = 1,
    )
}
