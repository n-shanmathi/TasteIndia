package com.example.tasteindia

import com.example.tasteindia.data.local.FavoriteMealDao
import com.example.tasteindia.data.local.FavoriteMealEntity
import com.example.tasteindia.data.remote.*
import com.example.tasteindia.data.repository.DefaultRecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class IndianIdIntersectionTest {

    private class FakeApi : TheMealDbApi {
        override suspend fun getIndianMeals(): MealListDto {
            return MealListDto(
                meals = listOf(
                    MealSummaryDto("52807", "Baigan Bharta", "thumb1"),
                    MealSummaryDto("52818", "Chicken Handi", "thumb2"), // Indian & Chicken
                    MealSummaryDto("52772", "Dal Dhamaka", "thumb3")
                )
            )
        }

        override suspend fun getMealDetail(mealId: String): MealDetailResponseDto {
            return MealDetailResponseDto(meals = emptyList())
        }

        override suspend fun getCategories(): CategoryListDto = CategoryListDto(emptyList())

        override suspend fun getIngredients(): IngredientListDto = IngredientListDto(emptyList())

        override suspend fun getMealsByCategory(category: String): MealListDto {
            return MealListDto(
                meals = listOf(
                    MealSummaryDto("52818", "Chicken Handi", "thumb2"), // Indian
                    MealSummaryDto("52796", "Chicken Alfredo", "thumb4") // Non-Indian!
                )
            )
        }

        override suspend fun getMealsByIngredient(ingredient: String): MealListDto {
            return MealListDto(
                meals = listOf(
                    MealSummaryDto("52818", "Chicken Handi", "thumb2"), // Indian
                    MealSummaryDto("52820", "Chicken Marengo", "thumb5") // Non-Indian!
                )
            )
        }

        override suspend fun searchMealsByName(query: String): MealDetailResponseDto {
            return MealDetailResponseDto(meals = emptyList())
        }
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

    @Test
    fun testCategoryFilterStrictlyIntersectsWithAuthoritativeIndianIds() = runTest {
        val repository = DefaultRecipeRepository(FakeApi(), FakeFavoriteDao())

        val result = repository.getFilteredMeals(
            categoryFilter = "Chicken",
            ingredientFilter = null,
            searchQuery = null
        )

        assertTrue(result.isSuccess)
        val filteredList = result.getOrNull() ?: emptyList()

        // Should contain ONLY Chicken Handi (52818) and exclude Chicken Alfredo (52796)
        assertEquals(1, filteredList.size)
        assertEquals("52818", filteredList[0].idMeal)
        assertEquals("Chicken Handi", filteredList[0].strMeal)
    }
}
