package com.velocity.speedometer.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.velocity.speedometer.ui.screens.about.AboutScreen
import com.velocity.speedometer.ui.screens.detail.TripDetailScreen
import com.velocity.speedometer.ui.screens.history.HistoryScreen
import com.velocity.speedometer.ui.screens.hud.HudScreen
import com.velocity.speedometer.ui.screens.permission.PermissionScreen
import com.velocity.speedometer.ui.screens.settings.SettingsScreen
import com.velocity.speedometer.ui.screens.speedometer.SpeedometerScreen

sealed class VelocityRoute(val route: String) {
    data object Permission : VelocityRoute("permission")
    data object Speedometer : VelocityRoute("speedometer")
    data object Hud : VelocityRoute("hud")
    data object History : VelocityRoute("history")
    data object TripDetail : VelocityRoute("trip_detail/{tripId}") {
        fun createRoute(tripId: Long): String = "trip_detail/$tripId"
    }
    data object Settings : VelocityRoute("settings")
    data object About : VelocityRoute("about")
}

private const val TRANSITION_MS = 250

@Composable
fun VelocityNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(TRANSITION_MS)) +
                slideInHorizontally(
                    animationSpec = tween(TRANSITION_MS),
                    initialOffsetX = { fullWidth -> fullWidth / 12 }
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(TRANSITION_MS)) +
                slideOutHorizontally(
                    animationSpec = tween(TRANSITION_MS),
                    targetOffsetX = { fullWidth -> -fullWidth / 12 }
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(TRANSITION_MS)) +
                slideInHorizontally(
                    animationSpec = tween(TRANSITION_MS),
                    initialOffsetX = { fullWidth -> -fullWidth / 12 }
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(TRANSITION_MS)) +
                slideOutHorizontally(
                    animationSpec = tween(TRANSITION_MS),
                    targetOffsetX = { fullWidth -> fullWidth / 12 }
                )
        }
    ) {
        composable(VelocityRoute.Permission.route) {
            PermissionScreen(
                onContinueToSpeedometer = {
                    navController.navigate(VelocityRoute.Speedometer.route) {
                        popUpTo(VelocityRoute.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(VelocityRoute.Speedometer.route) {
            SpeedometerScreen(
                onOpenHud = { navController.navigate(VelocityRoute.Hud.route) },
                onOpenHistory = { navController.navigate(VelocityRoute.History.route) },
                onOpenSettings = { navController.navigate(VelocityRoute.Settings.route) }
            )
        }

        composable(VelocityRoute.Hud.route) {
            HudScreen(
                onExitHud = { navController.popBackStack() }
            )
        }

        composable(VelocityRoute.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onTripClick = { tripId ->
                    navController.navigate(VelocityRoute.TripDetail.createRoute(tripId))
                }
            )
        }

        composable(
            route = VelocityRoute.TripDetail.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType }
            )
        ) {
            TripDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(VelocityRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(VelocityRoute.About.route) }
            )
        }

        composable(VelocityRoute.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
