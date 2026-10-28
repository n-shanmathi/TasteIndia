package com.example.tasteindia.ui.favourites

import com.example.tasteindia.domain.model.MealSummary

data class FavouritesUiState(
    val isLoading: Boolean = true,
    val favoriteMeals: List<MealSummary> = emptyList(),
    val errorMessage: String? = null
)
