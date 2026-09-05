package com.flydrop.app.ui.navigation

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flydrop.app.BuildConfig
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.TransferDirection
import com.flydrop.app.ui.about.AboutInfo
import com.flydrop.app.ui.about.AboutScreen
import com.flydrop.app.ui.auth.AuthStatus
import com.flydrop.app.ui.auth.AuthViewModel
import com.flydrop.app.ui.auth.SignInScreen
import com.flydrop.app.ui.components.FloatingBottomNavigation
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.NavTab
import com.flydrop.app.ui.home.HomeScreen
import com.flydrop.app.ui.home.HomeViewModel
import com.flydrop.app.ui.home.ContactsAccess
import com.flydrop.app.ui.nearby.NearbyScreen
import com.flydrop.app.ui.nearby.NearbyViewModel
import com.flydrop.app.ui.profile.ProfileScreen
import com.flydrop.app.ui.profile.ProfileViewModel
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.transfer.TransferScreen
import com.flydrop.app.ui.transfer.TransferViewModel

private object Routes {
    const val HOME = "home"
    const val NEARBY = "nearby"
    const val PROFILE = "profile"
    const val ABOUT = "about"
    const val TRANSFER = "transfer/{peerId}/{peerName}/{direction}"

    fun transfer(peer: FlyUser, direction: TransferDirection): String =
        "transfer/${Uri.encode(peer.id)}/${Uri.encode(peer.name)}/${direction.name}"
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
 * Hosts the three tab destinations plus the File Transfer and About screens.
 * Neither of those is a tab route, so the floating navigation hides on both.
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

    // Read once: what the build is cannot change while it is running.
    val aboutInfo = remember {
        AboutInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            packageName = BuildConfig.APPLICATION_ID,
            debugBuild = BuildConfig.DEBUG,
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
                val contactsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                    viewModel::onContactsPermissionResult,
                )
                LaunchedEffect(state.contactsAccess) {
                    if (state.contactsAccess == ContactsAccess.PermissionRequired) {
                        viewModel.markContactsPermissionRequestStarted()
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }
                HomeScreen(
                    // Fall back to the mock identity when running without Firebase.
                    state = if (identity != null) state.copy(currentUser = identity) else state,
                    onSendFile = { navigateToTab(Routes.NEARBY) },
                    onReceiveFile = { navigateToTab(Routes.NEARBY) },
                    onNotificationsClick = viewModel::clearNotifications,
                    onScan = { navigateToTab(Routes.NEARBY) },
                    onOpenFriend = { friend ->
                        navController.navigate(Routes.transfer(friend, TransferDirection.Outgoing))
                    },
                    onToggleFavourite = viewModel::toggleFavourite,
                    onRequestContactsPermission = viewModel::requestContactsPermission,
                    onRetryContacts = viewModel::retryContacts,
                    contentPadding = screenPadding,
                )
            }

            composable(Routes.NEARBY) {
                val viewModel: NearbyViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                NearbyScreen(
                    state = state,
                    onDiscoverableChange = viewModel::setDiscoverable,
                    onSelectUser = viewModel::selectUser,
                    onAddFriend = viewModel::addFriend,
                    contentPadding = screenPadding,
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    user = identity ?: MockData.currentUser,
                    signedIn = signedInUser != null,
                    flyIdState = flyIdState,
                    onEditFlyId = profileViewModel::openEditor,
                    onFlyIdInputChange = profileViewModel::onInputChange,
                    onConfirmFlyId = profileViewModel::confirmEdit,
                    onDismissFlyIdEditor = profileViewModel::dismissEditor,
                    onDismissFlyIdMessage = profileViewModel::dismissMessage,
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onSignOut = onSignOut,
                    contentPadding = screenPadding,
                )
            }

            composable(
                route = Routes.ABOUT,
                enterTransition = { slideInVertically(tween(300)) { it / 6 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(260)) },
            ) {
                AboutScreen(
                    info = aboutInfo,
                    onBack = { navController.popBackStack() },
                    contentPadding = PaddingValues(
                        top = statusBarPadding,
                        bottom = navigationBarPadding,
                    ),
                )
            }

            composable(
                route = Routes.TRANSFER,
                arguments = listOf(
                    navArgument("peerId") { type = NavType.StringType },
                    navArgument("peerName") { type = NavType.StringType },
                    navArgument("direction") { type = NavType.StringType },
                ),
                enterTransition = { slideInVertically(tween(300)) { it / 6 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(260)) },
            ) {
                val viewModel: TransferViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                TransferScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
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
