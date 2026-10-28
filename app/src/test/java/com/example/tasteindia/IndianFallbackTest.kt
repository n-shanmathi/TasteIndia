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
import java.io.IOException

class IndianFallbackTest {

    private class FakeDao : FavoriteMealDao {
        override fun getAllFavoriteEntitiesFlow(): Flow<List<FavoriteMealEntity>> = flowOf(emptyList())
        override fun getAllFavoriteIdsFlow(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getAllFavoriteIds(): List<String> = emptyList()
        override suspend fun getFavoriteById(idMeal: String): FavoriteMealEntity? = null
        override suspend fun isFavorite(idMeal: String): Boolean = false
        override suspend fun insertFavorite(favorite: FavoriteMealEntity) {}
        override suspend fun deleteFavoriteById(idMeal: String) {}
    }

    private class PrimarySuccessApi : TheMealDbApi {
        override suspend fun getIndianMeals(): MealListDto {
            return MealListDto(listOf(MealSummaryDto("52807", "Baigan Bharta", "thumb")))
        }
        override suspend fun getMealDetail(mealId: String): MealDetailResponseDto = MealDetailResponseDto(null)
        override suspend fun getCategories(): CategoryListDto = CategoryListDto(null)
        override suspend fun getIngredients(): IngredientListDto = IngredientListDto(null)
        override suspend fun getMealsByCategory(category: String): MealListDto = MealListDto(null)
        override suspend fun getMealsByIngredient(ingredient: String): MealListDto = MealListDto(null)
        override suspend fun searchMealsByName(query: String): MealDetailResponseDto = MealDetailResponseDto(null)
    }

    private class GlitchApiWithFallback : TheMealDbApi {
        override suspend fun getIndianMeals(): MealListDto {
            // Legitimate null returned by server for a=Indian
            return MealListDto(null)
        }

        override suspend fun searchMealsByName(query: String): MealDetailResponseDto {
            return when (query) {
                "curry" -> MealDetailResponseDto(
                    listOf(
                        MealDetailDto("52868", "Kidney Bean Curry", "Vegetarian", "India", "Instructions", "thumb", null, null, null),
                        MealDetailDto("52814", "Thai Green Curry", "Chicken", "Thai", "Instructions", "thumb", null, null, null) // Unrelated cuisine!
                    )
                )
                "chicken" -> MealDetailResponseDto(
                    listOf(
                        MealDetailDto("52795", "Chicken Handi", "Chicken", "India", "Instructions", "thumb", null, null, null),
                        MealDetailDto("52868", "Kidney Bean Curry", "Vegetarian", "India", "Instructions", "thumb", null, null, null) // Duplicate!
                    )
                )
                "mandi" -> MealDetailResponseDto(
                    listOf(
                        MealDetailDto("53359", "Beef Mandi", "Beef", "India", "Instructions", "thumb", null, null, null)
                    )
                )
                "omelette" -> MealDetailResponseDto(
                    listOf(
                        MealDetailDto("53076", "Bread omelette", "Breakfast", "India", "Instructions", "thumb", null, null, null)
                    )
                )
                else -> MealDetailResponseDto(null)
            }
        }

        override suspend fun getMealDetail(mealId: String): MealDetailResponseDto = MealDetailResponseDto(null)
        override suspend fun getCategories(): CategoryListDto = CategoryListDto(listOf(CategoryDto("Pasta"), CategoryDto("Chicken")))
        override suspend fun getIngredients(): IngredientListDto = IngredientListDto(null)
        override suspend fun getMealsByCategory(category: String): MealListDto = MealListDto(null)
        override suspend fun getMealsByIngredient(ingredient: String): MealListDto = MealListDto(null)
    }

    private class NetworkErrorApi : TheMealDbApi {
        override suspend fun getIndianMeals(): MealListDto {
            throw IOException("Network timeout")
        }
        override suspend fun getMealDetail(mealId: String): MealDetailResponseDto = MealDetailResponseDto(null)
        override suspend fun getCategories(): CategoryListDto = CategoryListDto(null)
        override suspend fun getIngredients(): IngredientListDto = IngredientListDto(null)
        override suspend fun getMealsByCategory(category: String): MealListDto = MealListDto(null)
        override suspend fun getMealsByIngredient(ingredient: String): MealListDto = MealListDto(null)
        override suspend fun searchMealsByName(query: String): MealDetailResponseDto = MealDetailResponseDto(null)
    }

    @Test
    fun testPrimaryEndpointSuccess() = runTest {
        val repository = DefaultRecipeRepository(PrimarySuccessApi(), FakeDao())
        val result = repository.getIndianMeals()

        assertTrue(result.isSuccess)
        val meals = result.getOrNull() ?: emptyList()
        assertEquals(1, meals.size)
        assertEquals("Baigan Bharta", meals[0].strMeal)
    }

    @Test
    fun testFallbackFiltersUnrelatedCuisinesAndDeduplicates() = runTest {
        val repository = DefaultRecipeRepository(GlitchApiWithFallback(), FakeDao())
        val result = repository.getIndianMeals()

        assertTrue(result.isSuccess)
        val meals = result.getOrNull() ?: emptyList()

        // Should return Kidney Bean Curry (52868), Chicken Handi (52795), Beef Mandi (53359), Bread omelette (53076)
        // Should EXCLUDE Thai Green Curry (strArea = Thai)
        // Should DEDUPLICATE Kidney Bean Curry (returned in multiple searches)
        assertEquals(4, meals.size)

        val mealIds = meals.map { it.idMeal }.toSet()
        assertTrue(mealIds.contains("52868"))
        assertTrue(mealIds.contains("52795"))
        assertTrue(mealIds.contains("53359"))
        assertTrue(mealIds.contains("53076"))
        assertFalse(mealIds.contains("52814")) // Thai Green Curry excluded
    }

    @Test
    fun testDerivedCategoryListFromIndianDataset() = runTest {
        val repository = DefaultRecipeRepository(GlitchApiWithFallback(), FakeDao())
        val result = repository.getCategories()

        assertTrue(result.isSuccess)
        val categories = result.getOrNull() ?: emptyList()

        // Should return ONLY categories represented in discovered Indian meals: Beef, Breakfast, Chicken, Vegetarian
        // Should NOT return Pasta (global category with no Indian recipes)
        assertEquals(4, categories.size)
        assertTrue(categories.contains("Beef"))
        assertTrue(categories.contains("Breakfast"))
        assertTrue(categories.contains("Chicken"))
        assertTrue(categories.contains("Vegetarian"))
        assertFalse(categories.contains("Pasta"))
    }

    @Test
    fun testCategoryFilteringWithMatchesAndNoMatches() = runTest {
        val repository = DefaultRecipeRepository(GlitchApiWithFallback(), FakeDao())

        // Test Category with Matches: Beef
        val beefResult = repository.getFilteredMeals("Beef", null, null)
        assertTrue(beefResult.isSuccess)
        val beefMeals = beefResult.getOrNull() ?: emptyList()
        assertEquals(1, beefMeals.size)
        assertEquals("Beef Mandi", beefMeals[0].strMeal)

        // Test Category with Matches: Breakfast
        val breakfastResult = repository.getFilteredMeals("Breakfast", null, null)
        assertTrue(breakfastResult.isSuccess)
        val breakfastMeals = breakfastResult.getOrNull() ?: emptyList()
        assertEquals(1, breakfastMeals.size)
        assertEquals("Bread omelette", breakfastMeals[0].strMeal)

        // Test Category with NO Matches: Pasta
        val pastaResult = repository.getFilteredMeals("Pasta", null, null)
        assertTrue(pastaResult.isSuccess)
        val pastaMeals = pastaResult.getOrNull() ?: emptyList()
        assertEquals(0, pastaMeals.size)
    }

    @Test
    fun testNetworkErrorHandledSeparately() = runTest {
        val repository = DefaultRecipeRepository(NetworkErrorApi(), FakeDao())
        val result = repository.getIndianMeals()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
