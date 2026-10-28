package com.example.tasteindia.ui.cookmode

import com.example.tasteindia.domain.model.CookStep

data class CookModeUiState(
    val isLoading: Boolean = true,
    val mealName: String = "",
    val thumbnailUrl: String? = null,
    val steps: List<CookStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
) {
    val currentStep: CookStep?
        get() = steps.getOrNull(currentStepIndex)

    val progress: Float
        get() = if (steps.isEmpty()) 0f else (currentStepIndex + 1).toFloat() / steps.size.toFloat()
}
