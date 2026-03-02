package com.splittrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.splittrip.ui.Screen
import com.splittrip.ui.screens.addtrip.CreateTripScreen
import com.splittrip.ui.screens.addtrip.ManageMembersScreen
import com.splittrip.ui.screens.dashboard.DashboardScreen
import com.splittrip.ui.screens.expense.AddExpenseScreen
import com.splittrip.ui.screens.expense.TripDetailScreen
import com.splittrip.ui.screens.report.ReportScreen
import com.splittrip.ui.theme.SplitTripTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplitTripTheme {
                SplitTripApp()
            }
        }
    }
}

@Composable
fun SplitTripApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onTripClick = { tripId -> navController.navigate(Screen.TripDetail.createRoute(tripId)) },
                onCreateTrip = { navController.navigate(Screen.CreateTrip.route) }
            )
        }

        composable(Screen.CreateTrip.route) {
            CreateTripScreen(
                onNavigateBack = { navController.popBackStack() },
                onTripCreated = { tripId ->
                    navController.navigate(Screen.TripDetail.createRoute(tripId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        composable(Screen.TripDetail.route) {
            TripDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = { tripId -> navController.navigate(Screen.AddExpense.createRoute(tripId)) },
                onEditExpense = { tripId, expenseId -> navController.navigate(Screen.AddExpense.createRoute(tripId, expenseId)) },
                onViewReport = { tripId -> navController.navigate(Screen.Report.createRoute(tripId)) },
                onManageMembers = { tripId -> navController.navigate(Screen.ManageMembers.createRoute(tripId)) }
            )
        }

        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Report.route) {
            ReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageMembers.route) {
            ManageMembersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
