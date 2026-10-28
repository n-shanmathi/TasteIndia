package com.example.tasteindia.ui.cookmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.repository.RecipeRepository
import com.example.tasteindia.domain.model.CookStep
import com.example.tasteindia.util.IngredientParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CookModeViewModel(
    private val mealId: String,
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookModeUiState())
    val uiState: StateFlow<CookModeUiState> = _uiState.asStateFlow()

    init {
        loadCookSteps()
    }

    fun loadCookSteps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getMealDetail(mealId)

            if (result.isSuccess) {
                val detail = result.getOrNull()
                val parsedTextSteps = IngredientParser.parseInstructionsToSteps(detail?.strInstructions)
                val cookSteps = parsedTextSteps.mapIndexed { index, stepText ->
                    CookStep(
                        stepNumber = index + 1,
                        totalSteps = parsedTextSteps.size,
                        instructionText = stepText,
                        mealName = detail?.strMeal ?: "Recipe",
                        thumbnailUrl = detail?.strMealThumb
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        mealName = detail?.strMeal ?: "Recipe",
                        thumbnailUrl = detail?.strMealThumb,
                        steps = cookSteps,
                        currentStepIndex = 0,
                        isCompleted = false,
                        errorMessage = if (cookSteps.isEmpty()) "No cooking steps found for this recipe." else null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load cooking steps."
                    )
                }
            }
        }
    }

    fun nextStep() {
        val state = _uiState.value
        if (state.currentStepIndex < state.steps.size - 1) {
            _uiState.update { it.copy(currentStepIndex = it.currentStepIndex + 1) }
        } else if (state.currentStepIndex == state.steps.size - 1) {
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.isCompleted) {
            _uiState.update { it.copy(isCompleted = false) }
        } else if (state.currentStepIndex > 0) {
            _uiState.update { it.copy(currentStepIndex = it.currentStepIndex - 1) }
        }
    }

    fun restart() {
        _uiState.update { it.copy(currentStepIndex = 0, isCompleted = false) }
    }

    companion object {
        fun provideFactory(
            mealId: String,
            repository: RecipeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CookModeViewModel(mealId, repository) as T
            }
        }
    }
}
