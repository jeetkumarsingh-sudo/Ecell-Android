package dcrustm.ecell.mobile.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dcrustm.ecell.mobile.ui.about.AboutUsScreen
import dcrustm.ecell.mobile.ui.directory.MemberDetailScreen
import dcrustm.ecell.mobile.ui.profile.ProfileScreen

@Composable
fun RootApp(
    onSignedOut: () -> Unit = {},
    viewModel: RootAppViewModel = hiltViewModel()
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    LaunchedEffect(isSignedIn) {
        if (!isSignedIn) onSignedOut()
    }

    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = RootScreen.Main.route
    ) {
        composable(route = RootScreen.Main.route) {
            MainScreen(rootNavController = rootNavController)
        }

        composable(
            route = RootScreen.Profile.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(150)
                )
            },
            popEnterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            }
        ) {
            ProfileScreen(onDismiss = { rootNavController.popBackStack() })
        }

        composable(
            route = RootScreen.Info.route,
            enterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300))
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            },
            popEnterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            }
        ) {
            AboutUsScreen(onDismiss = { rootNavController.popBackStack() })
        }

        composable(
            route = RootScreen.MemberDetail.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
            enterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300))
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            },
            popEnterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150))
            }
        ) {
            MemberDetailScreen(onBack = { rootNavController.popBackStack() })
        }
    }
}
