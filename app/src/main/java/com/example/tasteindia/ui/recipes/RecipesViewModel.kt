package com.example.tasteindia.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.repository.RecipeRepository
import com.example.tasteindia.domain.model.MealSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipesViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var rawFetchedMeals: List<MealSummary> = emptyList()

    init {
        observeFavorites()
        observeFavoriteErrors()
        loadInitialData()
    }

    private fun observeFavoriteErrors() {
        viewModelScope.launch {
            repository.favoriteErrorFlow.collect { error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(errorMessage = "Cloud favourites failed: ${error.message ?: "try again."}")
                    }
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteIdsFlow().collect { favIds ->
                _uiState.update { state ->
                    val updatedMeals = state.meals.map { meal ->
                        meal.copy(isFavorite = favIds.contains(meal.idMeal))
                    }
                    val updatedFeatured = state.featuredMeal?.copy(
                        isFavorite = favIds.contains(state.featuredMeal.idMeal)
                    )
                    state.copy(
                        favoriteIds = favIds,
                        meals = updatedMeals,
                        featuredMeal = updatedFeatured
                    )
                }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load categories and ingredients asynchronously
            launch { loadFilterOptions() }

            val result = repository.getIndianMeals()
            if (result.isSuccess) {
                val meals = result.getOrDefault(emptyList())
                rawFetchedMeals = meals
                val favIds = _uiState.value.favoriteIds
                val domainMeals = meals.map { it.copy(isFavorite = favIds.contains(it.idMeal)) }
                val featured = domainMeals.firstOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        meals = applySorting(domainMeals, it.sortOrder),
                        featuredMeal = featured,
                        errorMessage = null
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to load Indian recipes."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error
                    )
                }
            }
        }
    }

    private suspend fun loadFilterOptions() {
        _uiState.update { it.copy(isCategoriesLoading = true) }
        val catResult = repository.getCategories()
        val ingResult = repository.getIngredients()

        _uiState.update { state ->
            state.copy(
                isCategoriesLoading = false,
                categories = catResult.getOrDefault(emptyList()),
                ingredients = ingResult.getOrDefault(emptyList())
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            applyFiltersAndSearch()
        }
    }

    fun onCategorySelect(category: String?) {
        val newCategory = if (_uiState.value.selectedCategory == category) null else category
        _uiState.update { it.copy(selectedCategory = newCategory) }
        applyFiltersAndSearch()
    }

    fun onIngredientSelect(ingredient: String?) {
        val newIngredient = if (_uiState.value.selectedIngredient == ingredient) null else ingredient
        _uiState.update { it.copy(selectedIngredient = newIngredient) }
        applyFiltersAndSearch()
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = sortOrder,
                meals = applySorting(state.meals, sortOrder)
            )
        }
    }

    fun toggleFavoritesOnlyFilter() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        applyFiltersAndSearch()
    }

    fun clearAllFilters() {
        _uiState.update {
            it.copy(
                selectedCategory = null,
                selectedIngredient = null,
                showFavoritesOnly = false,
                searchQuery = "",
                sortOrder = SortOrder.NONE
            )
        }
        applyFiltersAndSearch()
    }

    private fun applyFiltersAndSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value

            val result = repository.getFilteredMeals(
                categoryFilter = state.selectedCategory,
                ingredientFilter = state.selectedIngredient,
                searchQuery = state.searchQuery
            )

            if (result.isSuccess) {
                val favIds = state.favoriteIds
                var filtered = result.getOrDefault(emptyList()).map {
                    it.copy(isFavorite = favIds.contains(it.idMeal))
                }
                if (state.showFavoritesOnly) {
                    filtered = filtered.filter { it.isFavorite }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        meals = applySorting(filtered, it.sortOrder),
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Filtering failed."
                    )
                }
            }
        }
    }

    private fun applySorting(meals: List<MealSummary>, sortOrder: SortOrder): List<MealSummary> {
        return when (sortOrder) {
            SortOrder.NONE -> meals
            SortOrder.A_TO_Z -> meals.sortedBy { it.strMeal }
            SortOrder.Z_TO_A -> meals.sortedByDescending { it.strMeal }
        }
    }

    fun toggleFavorite(mealSummary: MealSummary) {
        viewModelScope.launch {
            try {
                val isFavorite = repository.toggleFavorite(
                    mealId = mealSummary.idMeal,
                    summary = mealSummary
                )
                _uiState.update { state ->
                    val updatedFavoriteIds = if (isFavorite) {
                        state.favoriteIds + mealSummary.idMeal
                    } else {
                        state.favoriteIds - mealSummary.idMeal
                    }
                    state.copy(
                        favoriteIds = updatedFavoriteIds,
                        meals = state.meals.map { meal ->
                            if (meal.idMeal == mealSummary.idMeal) {
                                meal.copy(isFavorite = isFavorite)
                            } else {
                                meal
                            }
                        },
                        featuredMeal = state.featuredMeal?.let { featured ->
                            if (featured.idMeal == mealSummary.idMeal) {
                                featured.copy(isFavorite = isFavorite)
                            } else {
                                featured
                            }
                        }
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Could not save favourite to the cloud: ${error.message ?: "try again."}")
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: RecipeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecipesViewModel(repository) as T
                }
            }
    }
}
