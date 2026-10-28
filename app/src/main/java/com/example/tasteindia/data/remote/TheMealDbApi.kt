package com.example.tasteindia.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {

    @GET("filter.php?a=Indian")
    suspend fun getIndianMeals(): MealListDto

    @GET("lookup.php")
    suspend fun getMealDetail(@Query("i") mealId: String): MealDetailResponseDto

    @GET("list.php?c=list")
    suspend fun getCategories(): CategoryListDto

    @GET("list.php?i=list")
    suspend fun getIngredients(): IngredientListDto

    @GET("filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): MealListDto

    @GET("filter.php")
    suspend fun getMealsByIngredient(@Query("i") ingredient: String): MealListDto

    @GET("search.php")
    suspend fun searchMealsByName(@Query("s") query: String): MealDetailResponseDto
}
