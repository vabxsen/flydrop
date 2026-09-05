package com.flydrop.app.ui.navigation

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.auth.AuthStatus
import com.flydrop.app.ui.auth.AuthViewModel
import com.flydrop.app.ui.auth.SignInScreen
import com.flydrop.app.ui.components.FloatingBottomNavigation
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.NavTab
import com.flydrop.app.ui.home.HomeScreen
import com.flydrop.app.ui.home.HomeViewModel
import com.flydrop.app.ui.nearby.NearbyScreen
import com.flydrop.app.ui.nearby.NearbyViewModel
import com.flydrop.app.ui.profile.ProfileScreen
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.transfer.TransferScreen
import com.flydrop.app.ui.transfer.TransferViewModel

private object Routes {
    const val HOME = "home"
    const val NEARBY = "nearby"
    const val PROFILE = "profile"
    const val TRANSFER = "transfer"
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
                onContinueWithoutFirebase = authViewModel::continueWithoutFirebase,
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
 * Hosts the three tab destinations plus the File Transfer screen.
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy
        ?.firstOrNull { it.route in tabRoutes }?.route
    val showNav = currentRoute != null

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
                HomeScreen(
                    // Fall back to the mock identity when running without Firebase.
                    state = if (signedInUser != null) state.copy(currentUser = signedInUser) else state,
                    onSendFile = { navController.navigate(Routes.NEARBY) { launchSingleTop = true } },
                    onReceiveFile = { navController.navigate(Routes.NEARBY) { launchSingleTop = true } },
                    onOpenActivity = { navController.navigate(Routes.TRANSFER) },
                    onOpenFriend = { navController.navigate(Routes.TRANSFER) },
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
                    onAddFriend = {},
                    contentPadding = screenPadding,
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    user = signedInUser ?: MockData.currentUser,
                    signedIn = signedInUser != null,
                    onSignOut = onSignOut,
                    contentPadding = screenPadding,
                )
            }

            composable(
                route = Routes.TRANSFER,
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
                onSelect = { index ->
                    navController.navigate(tabRoutes[index]) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
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
