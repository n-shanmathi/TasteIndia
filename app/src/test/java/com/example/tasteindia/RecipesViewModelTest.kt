package com.example.tasteindia

import com.example.tasteindia.data.local.FavoriteMealDao
import com.example.tasteindia.data.local.FavoriteMealEntity
import com.example.tasteindia.data.remote.*
import com.example.tasteindia.data.repository.DefaultRecipeRepository
import com.example.tasteindia.ui.recipes.RecipesViewModel
import com.example.tasteindia.ui.recipes.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeApi : TheMealDbApi {
        override suspend fun getIndianMeals(): MealListDto {
            return MealListDto(
                meals = listOf(
                    MealSummaryDto("1", "Paneer Butter Masala", "thumb1"),
                    MealSummaryDto("2", "Aloo Gobi", "thumb2"),
                    MealSummaryDto("3", "Biryani", "thumb3")
                )
            )
        }

        override suspend fun getMealDetail(mealId: String): MealDetailResponseDto = MealDetailResponseDto(null)
        override suspend fun getCategories(): CategoryListDto = CategoryListDto(null)
        override suspend fun getIngredients(): IngredientListDto = IngredientListDto(null)
        override suspend fun getMealsByCategory(category: String): MealListDto = MealListDto(null)
        override suspend fun getMealsByIngredient(ingredient: String): MealListDto = MealListDto(null)
        override suspend fun searchMealsByName(query: String): MealDetailResponseDto = MealDetailResponseDto(null)
    }

    private class FakeFavoriteDao : FavoriteMealDao {
        override fun getAllFavoriteEntitiesFlow(): Flow<List<FavoriteMealEntity>> = flowOf(emptyList())
        override fun getAllFavoriteIdsFlow(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getAllFavoriteIds(): List<String> = emptyList()
        override suspend fun getFavoriteById(idMeal: String): FavoriteMealEntity? = null
        override suspend fun isFavorite(idMeal: String): Boolean = false
        override suspend fun insertFavorite(favorite: FavoriteMealEntity) {}
        override suspend fun deleteFavoriteById(idMeal: String) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSortingAndSearchQueryState() = runTest {
        val repository = DefaultRecipeRepository(FakeApi(), FakeFavoriteDao())
        val viewModel = RecipesViewModel(repository)

        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(3, state.meals.size)

        // Test A to Z sorting
        viewModel.onSortOrderChange(SortOrder.A_TO_Z)
        state = viewModel.uiState.value
        assertEquals("Aloo Gobi", state.meals[0].strMeal)
        assertEquals("Biryani", state.meals[1].strMeal)
        assertEquals("Paneer Butter Masala", state.meals[2].strMeal)

        // Test search query
        viewModel.onSearchQueryChange("Paneer")
        advanceTimeBy(400) // Debounce delay
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(1, state.meals.size)
        assertEquals("Paneer Butter Masala", state.meals[0].strMeal)
    }
}
