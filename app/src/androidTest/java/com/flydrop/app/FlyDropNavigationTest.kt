package com.flydrop.app

import android.Manifest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end smoke coverage for the primary guest navigation and actions. */
@RunWith(AndroidJUnit4::class)
class FlyDropNavigationTest {

    private val contactsPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(contactsPermissionRule)
        .around(composeRule)

    @Test
    fun guestFlowExercisesAllScreensAndActions() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Continue as guest")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Continue with Google")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithText("Continue as guest").performClick()

        composeRule.onNodeWithText("Favourite Friends").assertIsDisplayed()
        composeRule.onNodeWithText("No favourite friends yet").assertIsDisplayed()
        composeRule.onNodeWithText("Contacts").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in to find people by FlyDrop ID.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Unread notifications", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Notifications").performClick()
        composeRule.onNodeWithContentDescription("Unread notifications", useUnmergedTree = true)
            .assertDoesNotExist()

        // Send File opens the system document picker, which leaves the app and
        // so cannot be followed from a Compose test. Its presence is asserted
        // here; the picker and the resulting Nearby banner are covered by hand.
        composeRule.onNodeWithText("Send File").assertIsDisplayed().assertHasClickAction()

        navigateTo("Nearby")
        assertNearbySharing()
        navigateTo("Home")
        composeRule.onNodeWithText("Favourite Friends").assertIsDisplayed()

        composeRule.onNodeWithText("Receive File").performClick()
        composeRule.onNodeWithText("Nearby sharing").assertIsDisplayed()
        navigateTo("Home")

        composeRule.onNodeWithContentDescription("Scan a FlyDrop code").performClick()
        composeRule.onNodeWithText("Nearby sharing").assertIsDisplayed()
        navigateTo("Home")

        navigateTo("Profile")
        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
        openAndDismissAvatarSheet()
        openSettingsAndCheckPermissions()
        openAboutAndSwitchTabs()
        composeRule.onNodeWithText("Sign in").performClick()
        composeRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
    }

    /** Settings exposes every permission FlyDrop currently needs. */
    private fun openSettingsAndCheckPermissions() {
        composeRule.onNodeWithText("Settings").performClick()

        composeRule.onNodeWithText("Permissions").assertIsDisplayed()
        composeRule.onNodeWithText("Contacts").assertIsDisplayed()
        composeRule.onNodeWithText("Nearby Wi-Fi").assertIsDisplayed()
        composeRule.onNodeWithText("Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Internet access").assertIsDisplayed()
        composeRule.onNodeWithText("Install updates").assertIsDisplayed()
        composeRule.onNodeWithText("Open Android settings").assertHasClickAction()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
    }

    /**
     * The avatar badge opens the photo sheet and Cancel closes it. The pick
     * itself launches the system photo picker, which is outside the app and so
     * outside what this test can drive.
     */
    private fun openAndDismissAvatarSheet() {
        composeRule.onNodeWithContentDescription("Change profile photo").performClick()
        composeRule.onNodeWithText("Choose a photo").assertIsDisplayed()
        // Nothing to remove until a photo is set.
        composeRule.onNodeWithText("Use my generated avatar").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Choose a photo").assertDoesNotExist()
    }

    /** About opens from Profile, both tabs render, and Back returns to Profile. */
    private fun openAboutAndSwitchTabs() {
        composeRule.onNodeWithText("About").performClick()

        // Version is the tab About opens on. "Build type" and "Package" appear
        // only in that pane, so they stand in for it.
        composeRule.onNodeWithText("Build type").assertIsDisplayed()
        composeRule.onNodeWithText("Package").assertIsDisplayed()

        // The two ways out to the project. They are not tapped: both hand off
        // to a browser, which would leave the app and end the test.
        composeRule.onNodeWithText("Source code").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Report a bug").assertIsDisplayed().assertHasClickAction()

        // The update card starts idle and tappable. It is not tapped: doing so
        // would reach GitHub, making the test depend on the network and on what
        // happens to be released.
        composeRule.onNodeWithText("Check for update").assertIsDisplayed()
        composeRule.onNodeWithText("See if a newer build is on GitHub").assertIsDisplayed()

        aboutTab("Credits").assertIsNotSelected().performClick()
        aboutTab("Credits").assertIsSelected()
        composeRule.onNodeWithText("Designed & developed by Vaibhav Sen").assertIsDisplayed()
        composeRule.onNodeWithText("Made with ❤️ in India 🇮🇳").assertIsDisplayed()
        composeRule.onNodeWithText("Open Source").assertIsDisplayed()
        composeRule.onNodeWithText("Build type").assertDoesNotExist()

        aboutTab("Version").performClick()
        composeRule.onNodeWithText("Build type").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Not signed in").assertIsDisplayed()
    }

    /**
     * The tab, not the "Version" row label that shares its text: only the tab
     * carries the Tab role.
     */
    private fun aboutTab(label: String) = composeRule.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    private fun assertNearbySharing() {
        composeRule.onNodeWithText("Nearby sharing").assertIsDisplayed()
        composeRule.onNodeWithText("Share through Android").assertIsDisplayed()
        composeRule.onNodeWithText("Choose files to send")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Receive with Quick Share")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Ashley Nelson").assertDoesNotExist()
        composeRule.onNodeWithText("Gofar Badman").assertDoesNotExist()
    }

    private fun navigateTo(destination: String) {
        composeRule.onNode(
            hasContentDescription(destination) and hasClickAction(),
        ).performClick()
    }
}
