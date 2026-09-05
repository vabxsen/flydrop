package com.flydrop.app.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flydrop.app.BuildConfig
import com.flydrop.app.data.MockData
import com.flydrop.app.data.PickedFile
import com.flydrop.app.data.describePickedFiles
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.about.AboutInfo
import com.flydrop.app.ui.about.AboutScreen
import com.flydrop.app.ui.about.UpdateViewModel
import com.flydrop.app.ui.auth.AuthStatus
import com.flydrop.app.ui.auth.AuthViewModel
import com.flydrop.app.ui.auth.SignInScreen
import com.flydrop.app.ui.components.FloatingBottomNavigation
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.NavTab
import com.flydrop.app.ui.home.HomeScreen
import com.flydrop.app.ui.home.HomeViewModel
import com.flydrop.app.ui.home.InviteContactDialog
import com.flydrop.app.ui.home.sendInvite
import com.flydrop.app.ui.nearby.NearbyScreen
import com.flydrop.app.data.share.ShareOutcome
import com.flydrop.app.data.share.openQuickShareReceive
import com.flydrop.app.data.share.shareFiles
import com.flydrop.app.ui.profile.ProfileScreen
import com.flydrop.app.ui.profile.ProfileViewModel
import com.flydrop.app.ui.settings.SettingsRoute
import com.flydrop.app.ui.theme.FlyDrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object Routes {
    const val HOME = "home"
    const val NEARBY = "nearby"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private val tabRoutes = listOf(Routes.HOME, Routes.NEARBY, Routes.PROFILE)

/**
 * Top-level gate: splash while the Firebase session resolves, then either the
 * sign-in screen or the app itself.
 *
 * Auth is deliberately not a route inside [FlyDropNavHost]. Keeping it outside
 * means a sign-out cannot leave a signed-in screen on the back stack.
 */
@Composable
fun FlyDropApp(modifier: Modifier = Modifier) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val edgePadding = PaddingValues(top = statusBarPadding, bottom = navigationBarPadding)

    val signedIn = authState.status is AuthStatus.SignedIn
    val showApp = signedIn || authState.bypassedSetup

    Crossfade(
        targetState = when {
            authState.status is AuthStatus.Resolving -> Gate.Resolving
            showApp -> Gate.App
            else -> Gate.SignIn
        },
        animationSpec = tween(durationMillis = 260),
        label = "authGate",
        modifier = modifier.fillMaxSize(),
    ) { gate ->
        when (gate) {
            Gate.Resolving -> SplashScreen()

            Gate.SignIn -> SignInScreen(
                state = authState,
                onSignIn = { activity?.let(authViewModel::signInWithGoogle) },
                onContinueAsGuest = authViewModel::continueAsGuest,
                contentPadding = edgePadding,
            )

            Gate.App -> FlyDropNavHost(
                signedInUser = (authState.status as? AuthStatus.SignedIn)?.user,
                onSignOut = authViewModel::signOut,
                statusBarPadding = statusBarPadding,
                navigationBarPadding = navigationBarPadding,
            )
        }
    }
}

private enum class Gate { Resolving, SignIn, App }

/** Held only for the moment Firebase takes to report an existing session. */
@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FlyDrop.colors.heroAqua),
        contentAlignment = Alignment.Center,
    ) {
        FlyDropLogo()
    }
}

/**
 * Hosts the three tab destinations plus Settings and About. The real transfer
 * is delegated to Android's Sharesheet rather than a simulated in-app route.
 *
 * The floating navigation is drawn over the content rather than reserving a
 * slot for it, which is what lets the screens run edge to edge underneath, as
 * they do in the reference.
 */
@Composable
private fun FlyDropNavHost(
    signedInUser: FlyUser?,
    onSignOut: () -> Unit,
    statusBarPadding: Dp,
    navigationBarPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    // Hoisted above the routes: Send File picks on Home, and the chosen files
    // are shown on Nearby, where a recipient would be chosen.
    var pickedFiles by remember { mutableStateOf<List<PickedFile>>(emptyList()) }

    // Hoisted above the routes because the chosen FlyDrop ID appears on Home as
    // well as on Profile, and both must show the same one the moment it changes.
    // Guest mode has no account to hang an id on, so it binds null.
    val profileViewModel: ProfileViewModel = viewModel()
    val flyIdState by profileViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(signedInUser?.id) { profileViewModel.bind(signedInUser?.id) }
    val identity = signedInUser?.let { user ->
        flyIdState.flyId?.let { user.copy(flyId = it) } ?: user
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy
        ?.firstOrNull { it.route in tabRoutes }?.route
    val showNav = currentRoute != null

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            pickedFiles = withContext(Dispatchers.IO) {
                context.contentResolver.describePickedFiles(uris)
            }
            navigateToTab(Routes.NEARBY)
        }
    }

    val dimens = FlyDrop.dimens

    // Room for the floating bar so list content can scroll clear of it.
    val screenPadding = remember(statusBarPadding, navigationBarPadding, showNav) {
        PaddingValues(
            top = statusBarPadding,
            bottom = navigationBarPadding + if (showNav) {
                dimens.navHeight + dimens.navBottomInset + 8.dp
            } else {
                0.dp
            },
        )
    }

    // Read once: neither the build nor the device changes while it is running.
    val aboutInfo = remember {
        AboutInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            packageName = BuildConfig.APPLICATION_ID,
            debugBuild = BuildConfig.DEBUG,
            sourceUrl = BuildConfig.SOURCE_URL,
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
    }

    val tabs = remember {
        listOf(
            NavTab("Home", FlyDropIcons.Home),
            NavTab("Nearby", FlyDropIcons.Globe, activeIcon = FlyDropIcons.Radar),
            NavTab("Profile", FlyDropIcons.Person),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var invitee by remember { mutableStateOf<FlyUser?>(null) }
                var inviteError by remember { mutableStateOf<String?>(null) }

                val contactsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                    viewModel::onContactsPermissionResult,
                )
                HomeScreen(
                    state = state.copy(currentUser = identity ?: MockData.guestUser),
                    onSendFile = { filePicker.launch(arrayOf("*/*")) },
                    onReceiveFile = { navigateToTab(Routes.NEARBY) },
                    onNotificationsClick = viewModel::clearNotifications,
                    onScan = { navigateToTab(Routes.NEARBY) },
                    onToggleFavourite = viewModel::toggleFavourite,
                    onRequestContactsPermission = {
                        viewModel.markContactsPermissionRequestStarted()
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onRetryContacts = viewModel::retryContacts,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearch = viewModel::searchFlyId,
                    onClearSearch = viewModel::clearSearch,
                    onContactClick = {
                        inviteError = null
                        invitee = it
                    },
                    searchEnabled = signedInUser != null,
                    avatar = flyIdState.avatar,
                    contentPadding = screenPadding,
                )

                invitee?.let { contact ->
                    InviteContactDialog(
                        contact = contact,
                        onInvite = {
                            if (sendInvite(context, contact, BuildConfig.DOWNLOAD_URL)) {
                                invitee = null
                                inviteError = null
                            } else {
                                inviteError = "No messaging app is available on this phone."
                            }
                        },
                        onDismiss = {
                            invitee = null
                            inviteError = null
                        },
                        errorMessage = inviteError,
                    )
                }
            }

            composable(Routes.NEARBY) {
                var shareError by remember { mutableStateOf<String?>(null) }

                NearbyScreen(
                    onPickFiles = { filePicker.launch(arrayOf("*/*")) },
                    pickedFiles = pickedFiles,
                    onClearPickedFiles = { pickedFiles = emptyList() },
                    onSendWithQuickShare = {
                        shareError = when (val outcome = shareFiles(context, pickedFiles)) {
                            is ShareOutcome.Launched -> null
                            is ShareOutcome.NothingSelected -> "Pick a file to send first."
                            is ShareOutcome.Failed -> outcome.message
                        }
                    },
                    onReceiveWithQuickShare = {
                        shareError = if (openQuickShareReceive(context)) {
                            null
                        } else {
                            "Quick Share settings could not be opened. Open Quick Share from " +
                                "Android Settings or the notification shade."
                        }
                    },
                    shareError = shareError,
                    onDismissShareError = { shareError = null },
                    contentPadding = screenPadding,
                )
            }

            composable(Routes.PROFILE) {
                // The system photo picker: it hands back one image with no
                // storage permission at all, so the app never asks for access
                // to the whole gallery to set one avatar.
                val photoPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia(),
                    profileViewModel::onAvatarPicked,
                )
                ProfileScreen(
                    user = identity ?: MockData.guestUser,
                    signedIn = signedInUser != null,
                    flyIdState = flyIdState,
                    onEditFlyId = profileViewModel::openEditor,
                    onFlyIdInputChange = profileViewModel::onInputChange,
                    onConfirmFlyId = profileViewModel::confirmEdit,
                    onDismissFlyIdEditor = profileViewModel::dismissEditor,
                    onDismissFlyIdMessage = profileViewModel::dismissMessage,
                    onEditAvatar = profileViewModel::openAvatarSheet,
                    onChooseAvatar = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onRemoveAvatar = profileViewModel::removeAvatar,
                    onDismissAvatarSheet = profileViewModel::dismissAvatarSheet,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onSignOut = onSignOut,
                    contentPadding = screenPadding,
                )
            }

            composable(
                route = Routes.SETTINGS,
                enterTransition = { slideInVertically(tween(300)) { it / 6 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(260)) },
            ) {
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    contentPadding = PaddingValues(
                        top = statusBarPadding,
                        bottom = navigationBarPadding,
                    ),
                )
            }

            composable(
                route = Routes.ABOUT,
                enterTransition = { slideInVertically(tween(300)) { it / 6 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(260)) },
            ) {
                val updateViewModel: UpdateViewModel = viewModel()
                val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
                AboutScreen(
                    info = aboutInfo,
                    onBack = { navController.popBackStack() },
                    updateState = updateState,
                    onCheckForUpdate = {
                        updateViewModel.check(
                            apiUrl = BuildConfig.RELEASES_API_URL,
                            installedVersion = aboutInfo.versionName,
                        )
                    },
                    onDownloadUpdate = updateViewModel::downloadAndInstall,
                    onInstallUpdate = updateViewModel::install,
                    onGrantInstallPermission = updateViewModel::openInstallPermissionSettings,
                    contentPadding = PaddingValues(
                        top = statusBarPadding,
                        bottom = navigationBarPadding,
                    ),
                )
            }

        }

        if (showNav) {
            FloatingBottomNavigation(
                tabs = tabs,
                selectedIndex = tabRoutes.indexOf(currentRoute).coerceAtLeast(0),
                onSelect = { index -> navigateToTab(tabRoutes[index]) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = dimens.navHorizontalMargin,
                        end = dimens.navHorizontalMargin,
                        bottom = navigationBarPadding + dimens.navBottomInset,
                    ),
            )
        }
    }
}
