package com.example.tasteindia.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.repository.RecipeRepository
import com.example.tasteindia.domain.model.MealDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val mealId: String,
    private val repository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteIdsFlow().collect { favIds ->
                _uiState.update { state ->
                    val currentDetail = state.mealDetail
                    if (currentDetail != null) {
                        state.copy(mealDetail = currentDetail.copy(isFavorite = favIds.contains(mealId)))
                    } else state
                }
            }
        }
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getMealDetail(mealId)
            val isFav = repository.isFavorite(mealId)

            if (result.isSuccess) {
                val detail = result.getOrNull()?.copy(isFavorite = isFav)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        mealDetail = detail,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load recipe details."
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentDetail = _uiState.value.mealDetail
        viewModelScope.launch {
            try {
                val isFavorite = repository.toggleFavorite(mealId, currentDetail)
                _uiState.update { state ->
                    state.copy(
                        mealDetail = state.mealDetail?.copy(isFavorite = isFavorite)
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Could not save favourite to the cloud: ${error.message ?: "try again."}"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            mealId: String,
            repository: RecipeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(mealId, repository) as T
            }
        }
    }
}
