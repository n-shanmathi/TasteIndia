package com.example.tasteindia.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tasteindia.di.AppContainer
import com.example.tasteindia.ui.auth.AuthGate
import com.example.tasteindia.ui.auth.AuthViewModel
import com.example.tasteindia.ui.cookmode.CookModeScreen
import com.example.tasteindia.ui.cookmode.CookModeViewModel
import com.example.tasteindia.ui.details.DetailScreen
import com.example.tasteindia.ui.details.DetailViewModel
import com.example.tasteindia.ui.favourites.FavouritesScreen
import com.example.tasteindia.ui.favourites.FavouritesViewModel
import com.example.tasteindia.ui.recipes.RecipesScreen
import com.example.tasteindia.ui.recipes.RecipesViewModel

@Composable
fun TasteIndiaApp(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.provideFactory(appContainer.authRepository)
    )
    AuthGate(viewModel = authViewModel) {
        Scaffold(
            bottomBar = { BottomNavigationBar(navController = navController) }
        ) {
            innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Recipes.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Recipes.route) {
                    val viewModel: RecipesViewModel = viewModel(
                        factory = RecipesViewModel.provideFactory(appContainer.recipeRepository)
                    )
                    RecipesScreen(
                        viewModel = viewModel,
                        onRecipeClick = { mealId ->
                            navController.navigate(Screen.Detail.createRoute(mealId))
                        },
                        onSignOut = authViewModel::signOut
                    )
                }

                composable(Screen.Favourites.route) {
                    val viewModel: FavouritesViewModel = viewModel(
                        factory = FavouritesViewModel.provideFactory(appContainer.recipeRepository)
                    )
                    FavouritesScreen(
                        viewModel = viewModel,
                        onRecipeClick = { mealId ->
                            navController.navigate(Screen.Detail.createRoute(mealId))
                        },
                        onExploreClick = {
                            navController.navigate(Screen.Recipes.route) {
                                popUpTo(Screen.Recipes.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("mealId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val mealId = backStackEntry.arguments?.getString("mealId") ?: ""
                    val viewModel: DetailViewModel = viewModel(
                        factory = DetailViewModel.provideFactory(mealId, appContainer.recipeRepository)
                    )
                    DetailScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onStartCookingClick = { id ->
                            navController.navigate(Screen.CookMode.createRoute(id))
                        }
                    )
                }

                composable(
                    route = Screen.CookMode.route,
                    arguments = listOf(navArgument("mealId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val mealId = backStackEntry.arguments?.getString("mealId") ?: ""
                    val viewModel: CookModeViewModel = viewModel(
                        factory = CookModeViewModel.provideFactory(mealId, appContainer.recipeRepository)
                    )
                    CookModeScreen(
                        viewModel = viewModel,
                        onCloseClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
