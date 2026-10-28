package com.example.tasteindia.domain.model

data class MealSummary(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String?,
    val strCategory: String? = null,
    val strArea: String? = null,
    val isFavorite: Boolean = false
)
