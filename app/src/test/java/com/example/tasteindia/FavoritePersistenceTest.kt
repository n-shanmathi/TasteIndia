package com.example.tasteindia

import com.example.tasteindia.data.local.FavoriteMealDao
import com.example.tasteindia.data.local.FavoriteMealEntity
import com.example.tasteindia.data.remote.*
import com.example.tasteindia.data.repository.DefaultRecipeRepository
import com.example.tasteindia.domain.model.MealDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FavoritePersistenceTest {

    private class MemoryFavoriteDao : FavoriteMealDao {
        private val storage = mutableMapOf<String, FavoriteMealEntity>()
        private val idsFlow = MutableStateFlow<List<String>>(emptyList())
        private val entitiesFlow = MutableStateFlow<List<FavoriteMealEntity>>(emptyList())

        private fun notifyChanges() {
            idsFlow.value = storage.keys.toList()
            entitiesFlow.value = storage.values.toList()
        }

        override fun getAllFavoriteEntitiesFlow(): Flow<List<FavoriteMealEntity>> = entitiesFlow
        override fun getAllFavoriteIdsFlow(): Flow<List<String>> = idsFlow
        override suspend fun getAllFavoriteIds(): List<String> = storage.keys.toList()
        override suspend fun getFavoriteById(idMeal: String): FavoriteMealEntity? = storage[idMeal]
        override suspend fun isFavorite(idMeal: String): Boolean = storage.containsKey(idMeal)
        override suspend fun insertFavorite(favorite: FavoriteMealEntity) {
            storage[favorite.idMeal] = favorite
            notifyChanges()
        }
        override suspend fun deleteFavoriteById(idMeal: String) {
            storage.remove(idMeal)
            notifyChanges()
        }
    }

    private class FakeApi : TheMealDbApi {
        override suspend fun getIndianMeals(): MealListDto = MealListDto(null)
        override suspend fun getMealDetail(mealId: String): MealDetailResponseDto = MealDetailResponseDto(null)
        override suspend fun getCategories(): CategoryListDto = CategoryListDto(null)
        override suspend fun getIngredients(): IngredientListDto = IngredientListDto(null)
        override suspend fun getMealsByCategory(category: String): MealListDto = MealListDto(null)
        override suspend fun getMealsByIngredient(ingredient: String): MealListDto = MealListDto(null)
        override suspend fun searchMealsByName(query: String): MealDetailResponseDto = MealDetailResponseDto(null)
    }

    @Test
    fun testFavoriteToggleAndOfflinePersistence() = runTest {
        val dao = MemoryFavoriteDao()
        val repository = DefaultRecipeRepository(FakeApi(), dao)

        val mealId = "52807"
        val detail = MealDetail(
            idMeal = mealId,
            strMeal = "Baigan Bharta",
            strCategory = "Vegetarian",
            strArea = "Indian",
            strInstructions = "Roast eggplant on open flame...",
            strMealThumb = "https://example.com/baigan.jpg",
            strTags = listOf("Eggplant", "Healthy"),
            strYoutube = null,
            strSource = null,
            ingredients = emptyList(),
            isFavorite = false
        )

        // 1. Initial check: not favorite
        assertFalse(repository.isFavorite(mealId))

        // 2. Toggle favorite: add
        val isNowFavorite = repository.toggleFavorite(mealId, detail)
        assertTrue(isNowFavorite)
        assertTrue(repository.isFavorite(mealId))

        // 3. Verify detail retrieval from offline DB storage
        val retrievedResult = repository.getMealDetail(mealId)
        assertTrue(retrievedResult.isSuccess)
        val retrievedDetail = retrievedResult.getOrNull()
        assertNotNull(retrievedDetail)
        assertEquals("Baigan Bharta", retrievedDetail?.strMeal)
        assertEquals("Roast eggplant on open flame...", retrievedDetail?.strInstructions)

        // 4. Toggle favorite: remove
        val isRemoved = repository.toggleFavorite(mealId, detail)
        assertFalse(isRemoved)
        assertFalse(repository.isFavorite(mealId))
    }
}
