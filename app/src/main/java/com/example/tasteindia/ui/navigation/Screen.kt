package com.example.tasteindia.ui.navigation

sealed class Screen(val route: String) {
    object Recipes : Screen("recipes")
    object Favourites : Screen("favourites")
    object Detail : Screen("detail/{mealId}") {
        fun createRoute(mealId: String) = "detail/$mealId"
    }
    object CookMode : Screen("cookmode/{mealId}") {
        fun createRoute(mealId: String) = "cookmode/$mealId"
    }
}
