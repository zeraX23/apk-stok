package com.zerax23.stokbarcode.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zerax23.stokbarcode.presentation.addproduct.AddEditProductScreen
import com.zerax23.stokbarcode.presentation.generate.GenerateBarcodeScreen
import com.zerax23.stokbarcode.presentation.history.HistoryScreen
import com.zerax23.stokbarcode.presentation.home.HomeScreen
import com.zerax23.stokbarcode.presentation.report.DailyReportScreen
import com.zerax23.stokbarcode.presentation.scan.ScanToSellScreen

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Beranda", Icons.Default.Home)
    object Generate : Screen(
        "generate_barcode", "Generate", Icons.Default.QrCode2
    )
    object Scan : Screen(
        "scan_to_sell", "Scan", Icons.Default.QrCodeScanner
    )
    object History : Screen("history", "Riwayat", Icons.Default.History)
    object Report : Screen(
        "daily_report", "Laporan", Icons.Default.BarChart
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Generate,
    Screen.Scan,
    Screen.History,
    Screen.Report
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Screens yang tampilkan bottom nav
    val showBottomNav = bottomNavItems.any {
        currentDestination?.hierarchy?.any { dest ->
            dest.route == it.route
        } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(screen.icon, screen.label)
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(
                                        navController.graph
                                            .findStartDestination().id
                                    ) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddProduct = {
                        navController.navigate("add_product")
                    },
                    onEditProduct = { productId ->
                        navController.navigate("edit_product/$productId")
                    },
                    onNavigateToReport = {
                        navController.navigate(Screen.Report.route)
                    }
                )
            }

            // Tambah Produk
            composable("add_product") {
                AddEditProductScreen(
                    productId = null,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Edit Produk
            composable(
                route = "edit_product/{productId}",
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.IntType
                    }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments
                    ?.getInt("productId")
                AddEditProductScreen(
                    productId = productId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Generate Barcode
            composable(Screen.Generate.route) {
                GenerateBarcodeScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Scan to Sell
            composable(Screen.Scan.route) {
                ScanToSellScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // History
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Daily Report
            composable(Screen.Report.route) {
                DailyReportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
