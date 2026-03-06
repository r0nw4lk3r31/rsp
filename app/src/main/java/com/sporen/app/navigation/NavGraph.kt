package com.sporen.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sporen.app.ui.edit.EditShiftScreen
import com.sporen.app.ui.onboarding.OnboardingScreen
import com.sporen.app.ui.onboarding.OnboardingViewModel
import com.sporen.app.ui.settings.SettingsScreen
import com.sporen.app.ui.shifts.ShiftsScreen
import java.time.YearMonth

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Shifts : Screen("shifts/{yearMonth}") {
        fun createRoute(yearMonth: YearMonth) = "shifts/$yearMonth"
    }
    data object EditShift : Screen("edit/{shiftId}") {
        fun createRoute(shiftId: Long = -1L) = "edit/$shiftId"
    }
    data object Settings : Screen("settings/{yearMonth}") {
        fun createRoute(yearMonth: YearMonth) = "settings/$yearMonth"
    }
}

@Composable
fun SporenNavGraph() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState(initial = null)

    // Wait until preference is loaded before deciding start destination
    val startDestination = when (onboardingComplete) {
        true -> Screen.Shifts.createRoute(YearMonth.now())
        false -> Screen.Onboarding.route
        null -> return // still loading — render nothing
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Shifts.createRoute(YearMonth.now())) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Shifts.route,
            arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
        ) {
            ShiftsScreen(
                onAddShift = { navController.navigate(Screen.EditShift.createRoute()) },
                onEditShift = { id -> navController.navigate(Screen.EditShift.createRoute(id)) },
                onSettings = { month -> navController.navigate(Screen.Settings.createRoute(month)) },
            )
        }

        composable(
            route = Screen.EditShift.route,
            arguments = listOf(navArgument("shiftId") { type = NavType.LongType })
        ) {
            EditShiftScreen(
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

