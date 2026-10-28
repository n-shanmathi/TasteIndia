package com.example.tasteindia.domain.model

data class MealDetail(
    val idMeal: String,
    val strMeal: String,
    val strCategory: String?,
    val strArea: String?,
    val strInstructions: String?,
    val strMealThumb: String?,
    val strTags: List<String>,
    val strYoutube: String?,
    val strSource: String?,
    val ingredients: List<IngredientAmount>,
    val isFavorite: Boolean = false
)
