package com.example.tasteindia.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.repository.RecipeRepository
import com.example.tasteindia.domain.model.MealSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavouritesViewModel(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouritesUiState())
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        observeFavoriteErrors()
    }

    private fun observeFavoriteErrors() {
        viewModelScope.launch {
            repository.favoriteErrorFlow.collect { error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Cloud favourites failed: ${error.message ?: "try again."}"
                        )
                    }
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteMealsFlow().collect { favorites ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        favoriteMeals = favorites,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun removeFavorite(mealSummary: MealSummary) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(mealSummary.idMeal)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not delete cloud favourite: ${error.message ?: "try again."}"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: RecipeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FavouritesViewModel(repository) as T
                }
            }
    }
}
