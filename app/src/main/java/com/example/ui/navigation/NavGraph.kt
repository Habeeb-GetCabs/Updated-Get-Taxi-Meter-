package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DriverProfileScreen
import com.example.ui.screens.HomeMeterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TripReceiptScreen
import com.example.ui.screens.TripSummaryScreen
import com.example.ui.screens.VendorManagementScreen
import com.example.util.AppLanguage
import com.example.util.AppLanguageManager
import com.example.util.LocalAppStrings
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.SettingsViewModel

@Composable
fun TaxiMeterNavGraph(
    meterViewModel: MeterViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AppLanguageManager.init(context)
    }
    val currencySymbol by settingsViewModel.currency.collectAsStateWithLifecycle()
    val appLanguageCode by settingsViewModel.appLanguage.collectAsStateWithLifecycle()
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsStateWithLifecycle()

    val resolvedLanguage = if (appLanguageCode == AppLanguage.TAMIL.code || currentLanguage == AppLanguage.TAMIL) {
        if (currentLanguage == AppLanguage.TAMIL && appLanguageCode == AppLanguage.TAMIL.code) AppLanguage.TAMIL
        else currentLanguage
    } else {
        currentLanguage
    }
    val activeStrings = AppLanguageManager.stringsFor(resolvedLanguage)

    CompositionLocalProvider(LocalAppStrings provides activeStrings) {
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            composable("splash") {
                SplashScreen(onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            composable("home") {
                HomeMeterScreen(
                    viewModel = meterViewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToProfile = { navController.navigate("driver_profile") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToVendorManagement = { navController.navigate("vendor_management") },
                    onNavigateToReceipt = { tripId -> navController.navigate("receipt/$tripId") },
                    onNavigateToSummary = { tripId -> navController.navigate("summary/$tripId") }
                )
            }

            composable("vendor_management") {
                VendorManagementScreen(
                    viewModel = meterViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("driver_profile") {
                DriverProfileScreen(
                    viewModel = meterViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("app_details") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDriverProfile = { navController.navigate("driver_profile") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToVendorManagement = { navController.navigate("vendor_management") }
                )
            }

            composable("receipt/{tripId}") { backStackEntry ->
                val tripIdStr = backStackEntry.arguments?.getString("tripId")
                val tripId = tripIdStr?.toIntOrNull() ?: 0

                TripReceiptScreen(
                    viewModel = meterViewModel,
                    tripId = tripId,
                    currencySymbol = currencySymbol,
                    onNavigateBack = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            composable("summary/{tripId}") { backStackEntry ->
                val tripIdStr = backStackEntry.arguments?.getString("tripId")
                val tripId = tripIdStr?.toIntOrNull() ?: 0

                TripSummaryScreen(
                    tripId = tripId,
                    viewModel = meterViewModel,
                    currencySymbol = currencySymbol,
                    onNavigateBack = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onNavigateToInvoice = { id ->
                        navController.navigate("receipt/$id")
                    }
                )
            }
        }
    }
}
