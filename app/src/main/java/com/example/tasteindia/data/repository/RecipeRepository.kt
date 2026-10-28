package com.example.tasteindia.data.repository

import com.example.tasteindia.data.local.FavoriteMealDao
import com.example.tasteindia.data.local.FavoriteMealEntity
import com.example.tasteindia.data.favorite.FavoriteStore
import com.example.tasteindia.data.favorite.LocalFavoriteStore
import com.example.tasteindia.data.remote.TheMealDbApi
import com.example.tasteindia.domain.model.MealDetail
import com.example.tasteindia.domain.model.MealSummary
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

interface RecipeRepository {
    fun setAuthenticatedUser(userId: String?)
    fun getFavoriteIdsFlow(): Flow<Set<String>>
    fun getFavoriteMealsFlow(): Flow<List<MealSummary>>
    val favoriteErrorFlow: Flow<Throwable?>

    suspend fun getIndianMeals(): Result<List<MealSummary>>
    suspend fun getMealDetail(mealId: String): Result<MealDetail>

    suspend fun getCategories(): Result<List<String>>
    suspend fun getIngredients(): Result<List<String>>

    suspend fun getFilteredMeals(
        categoryFilter: String?,
        ingredientFilter: String?,
        searchQuery: String?
    ): Result<List<MealSummary>>

    suspend fun toggleFavorite(
        mealId: String,
        detail: MealDetail? = null,
        summary: MealSummary? = null
    ): Boolean
    suspend fun isFavorite(mealId: String): Boolean
}

class DefaultRecipeRepository(
    private val api: TheMealDbApi,
    private val favoriteDao: FavoriteMealDao,
    private val favoriteStore: FavoriteStore = LocalFavoriteStore(favoriteDao)
) : RecipeRepository {

    override fun setAuthenticatedUser(userId: String?) {
        favoriteDao.setUserId(userId)
        favoriteStore.setUserId(userId)
        detailCache.clear()
    }

    override val favoriteErrorFlow: Flow<Throwable?>
        get() = favoriteStore.errorFlow

    // Cache of authoritative Indian meal summaries and IDs
    @Volatile
    private var cachedIndianSummaries: List<MealSummary>? = null
    private val authoritativeIndianIds = ConcurrentHashMap.newKeySet<String>()

    // Bounded and deterministic fallback search terms for Indian cuisine
    private val fallbackSearchQueries = listOf("chicken", "curry", "dal", "paneer", "biryani", "mandi", "fish", "masala", "omelette", "rogan", "bharta")

    // In-memory detail cache & in-flight request deduplication map
    private val detailCache = ConcurrentHashMap<String, MealDetail>()
    private val inFlightDetails = ConcurrentHashMap<String, Deferred<Result<MealDetail>>>()

    override fun getFavoriteIdsFlow(): Flow<Set<String>> {
        return favoriteStore.getFavoriteIdsFlow()
    }

    override fun getFavoriteMealsFlow(): Flow<List<MealSummary>> {
        return favoriteStore.getFavoriteMealsFlow()
    }

    override suspend fun getIndianMeals(): Result<List<MealSummary>> {
        val cached = cachedIndianSummaries
        if (!cached.isNullOrEmpty()) {
            return Result.success(cached)
        }

        // 1. Try primary endpoint filter.php?a=Indian
        val primaryResult = try {
            val dto = api.getIndianMeals()
            val list = dto.meals?.mapNotNull { it.toDomain() } ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }

        if (primaryResult.isSuccess) {
            val primaryList = primaryResult.getOrNull() ?: emptyList()
            if (primaryList.isNotEmpty()) {
                cachedIndianSummaries = primaryList
                authoritativeIndianIds.clear()
                primaryList.forEach { authoritativeIndianIds.add(it.idMeal) }
                return Result.success(primaryList)
            }
        } else {
            // Network error on primary call -> return failure rather than silently running fallback
            return primaryResult
        }

        // 2. Primary call returned legitimate empty/null (TheMealDB server-side filter glitch for a=Indian).
        // Execute bounded, deterministic fallback search strategy.
        return try {
            val fallbackMeals = fetchIndianMealsFallback()
            if (fallbackMeals.isNotEmpty()) {
                cachedIndianSummaries = fallbackMeals
                authoritativeIndianIds.clear()
                fallbackMeals.forEach { authoritativeIndianIds.add(it.idMeal) }
            }
            Result.success(fallbackMeals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchIndianMealsFallback(): List<MealSummary> = coroutineScope {
        val deferredResults = fallbackSearchQueries.map { query ->
            async {
                try {
                    val response = api.searchMealsByName(query)
                    response.meals?.filter { meal ->
                        val area = meal.strArea?.trim()
                        area?.equals("India", ignoreCase = true) == true ||
                                area?.equals("Indian", ignoreCase = true) == true
                    }?.mapNotNull { it.toMealSummary() } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

        val allResults = deferredResults.flatMap { it.await() }
        // Strict deduplication by idMeal
        allResults.distinctBy { it.idMeal }
    }

    override suspend fun getMealDetail(mealId: String): Result<MealDetail> {
        // 1. Check in-memory cache
        detailCache[mealId]?.let { return Result.success(it) }

        // 2. Check local DB (for offline favorites)
        val localEntity = favoriteDao.getFavoriteById(mealId)
        if (localEntity != null && !localEntity.strInstructions.isNullOrBlank()) {
            val detail = localEntity.toMealDetail()
            detailCache[mealId] = detail
            return Result.success(detail)
        }

        // 3. Deduplicate network request if already in flight
        val existingJob = inFlightDetails[mealId]
        if (existingJob != null) {
            return existingJob.await()
        }

        return coroutineScope {
            val newJob = async {
                try {
                    val dto = api.getMealDetail(mealId)
                    val detailDto = dto.meals?.firstOrNull()
                    val isFav = favoriteStore.isFavorite(mealId)
                    val domainDetail = detailDto?.toDomain(isFavorite = isFav)

                    if (domainDetail != null) {
                        detailCache[mealId] = domainDetail
                        // If it's a favorite, update Room DB with full details
                        Result.success(domainDetail)
                    } else {
                        // If offline favorite entity exists with partial data
                        if (localEntity != null) {
                            Result.success(localEntity.toMealDetail())
                        } else {
                            Result.failure(NoSuchElementException("Meal detail not found for ID: $mealId"))
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to local DB if network fails
                    if (localEntity != null) {
                        Result.success(localEntity.toMealDetail())
                    } else {
                        Result.failure(e)
                    }
                } finally {
                    inFlightDetails.remove(mealId)
                }
            }

            inFlightDetails[mealId] = newJob
            newJob.await()
        }
    }

    override suspend fun getCategories(): Result<List<String>> {
        val indianResult = getIndianMeals()
        if (indianResult.isSuccess) {
            val meals = indianResult.getOrNull() ?: emptyList()
            val derivedCategories = meals.mapNotNull { it.strCategory?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            if (derivedCategories.isNotEmpty()) {
                return Result.success(derivedCategories)
            }
        }

        return try {
            val response = api.getCategories()
            val categories = response.meals?.mapNotNull { it.strCategory?.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIngredients(): Result<List<String>> {
        return try {
            val response = api.getIngredients()
            val ingredients = response.meals?.mapNotNull { it.strIngredient?.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            Result.success(ingredients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFilteredMeals(
        categoryFilter: String?,
        ingredientFilter: String?,
        searchQuery: String?
    ): Result<List<MealSummary>> {
        // Ensure authoritative Indian ID set is loaded
        val indianResult = getIndianMeals()
        if (indianResult.isFailure && cachedIndianSummaries.isNullOrEmpty()) {
            return indianResult
        }

        val indianSummaries = cachedIndianSummaries ?: emptyList()
        val indianIds = authoritativeIndianIds.toSet()

        return try {
            var candidateIds: Set<String>? = null

            // 1. Filter by Category if provided
            if (!categoryFilter.isNullOrBlank()) {
                val matchingFromLoaded = indianSummaries.filter {
                    it.strCategory?.equals(categoryFilter, ignoreCase = true) == true
                }.map { it.idMeal }.toSet()

                val catIds = if (matchingFromLoaded.isNotEmpty()) {
                    matchingFromLoaded
                } else {
                    val catResponse = api.getMealsByCategory(categoryFilter)
                    catResponse.meals?.mapNotNull { it.idMeal }?.toSet() ?: emptySet()
                }

                candidateIds = intersectSets(candidateIds, catIds)
            }

            // 2. Filter by Main Ingredient if provided
            if (!ingredientFilter.isNullOrBlank()) {
                val ingResponse = api.getMealsByIngredient(ingredientFilter)
                val ingIds = ingResponse.meals?.mapNotNull { it.idMeal }?.toSet() ?: emptySet()
                candidateIds = intersectSets(candidateIds, ingIds)
            }

            // Always intersect with authoritative Indian IDs!
            val validIndianIds = intersectSets(candidateIds, indianIds)

            // Filter our Indian summaries list
            var results = indianSummaries.filter { validIndianIds.contains(it.idMeal) }

            // 3. Apply search query filtering if provided
            if (!searchQuery.isNullOrBlank()) {
                val query = searchQuery.trim().lowercase()
                results = results.filter { it.strMeal.lowercase().contains(query) }
            }

            Result.success(results)
        } catch (e: Exception) {
            // Fallback: local filtering on loaded Indian list
            var results = indianSummaries
            if (!categoryFilter.isNullOrBlank()) {
                results = results.filter { it.strCategory?.equals(categoryFilter, ignoreCase = true) == true }
            }
            if (!searchQuery.isNullOrBlank()) {
                val query = searchQuery.trim().lowercase()
                results = results.filter { it.strMeal.lowercase().contains(query) }
            }
            Result.success(results)
        }
    }

    override suspend fun toggleFavorite(
        mealId: String,
        detail: MealDetail?,
        summary: MealSummary?
    ): Boolean {
        val summaryToSave = summary ?: cachedIndianSummaries?.find { it.idMeal == mealId }
        val isFavorite = favoriteStore.toggleFavorite(mealId, detail, summaryToSave)
        if (!isFavorite) {
            // Update in-memory cache
            detailCache[mealId]?.let {
                detailCache[mealId] = it.copy(isFavorite = false)
            }
            return false
        } else {
            val detailToSave = detail ?: detailCache[mealId] ?: getMealDetail(mealId).getOrNull()
            if (detailToSave != null) {
                detailCache[mealId] = detailToSave.copy(isFavorite = true)
            }
            return true
        }
    }

    override suspend fun isFavorite(mealId: String): Boolean {
        return favoriteStore.isFavorite(mealId)
    }

    private fun intersectSets(current: Set<String>?, newSet: Set<String>): Set<String> {
        return if (current == null) newSet else current.intersect(newSet)
    }
}
