package com.splittrip.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object CreateTrip : Screen("create_trip")
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
    object AddExpense : Screen("add_expense/{tripId}?expenseId={expenseId}") {
        fun createRoute(tripId: String, expenseId: String? = null): String {
            return if (expenseId != null) "add_expense/$tripId?expenseId=$expenseId"
            else "add_expense/$tripId?expenseId="
        }
    }
    object Report : Screen("report/{tripId}") {
        fun createRoute(tripId: String) = "report/$tripId"
    }
    object ManageMembers : Screen("manage_members/{tripId}") {
        fun createRoute(tripId: String) = "manage_members/$tripId"
    }
}
