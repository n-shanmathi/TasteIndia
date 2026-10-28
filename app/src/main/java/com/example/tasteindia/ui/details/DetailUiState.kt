package com.example.tasteindia.ui.details

import com.example.tasteindia.domain.model.MealDetail

data class DetailUiState(
    val isLoading: Boolean = true,
    val mealDetail: MealDetail? = null,
    val errorMessage: String? = null
)
