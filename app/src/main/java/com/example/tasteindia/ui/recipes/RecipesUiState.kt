package com.example.tasteindia.ui.recipes

import com.example.tasteindia.domain.model.MealSummary

enum class SortOrder {
    NONE,
    A_TO_Z,
    Z_TO_A
}

data class RecipesUiState(
    val isLoading: Boolean = true,
    val isCategoriesLoading: Boolean = true,
    val meals: List<MealSummary> = emptyList(),
    val featuredMeal: MealSummary? = null,
    val categories: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedIngredient: String? = null,
    val showFavoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NONE,
    val favoriteIds: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    val appliedFilterCount: Int
        get() {
            var count = 0
            if (!selectedCategory.isNullOrBlank()) count++
            if (!selectedIngredient.isNullOrBlank()) count++
            if (showFavoritesOnly) count++
            if (searchQuery.isNotBlank()) count++
            if (sortOrder != SortOrder.NONE) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = appliedFilterCount > 0
}
