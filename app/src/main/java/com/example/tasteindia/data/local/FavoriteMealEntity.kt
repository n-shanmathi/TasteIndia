package com.example.tasteindia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tasteindia.domain.model.IngredientAmount
import com.example.tasteindia.domain.model.MealDetail
import com.example.tasteindia.domain.model.MealSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "favorite_meals")
data class FavoriteMealEntity(
    @PrimaryKey
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String?,
    val strCategory: String?,
    val strArea: String?,
    val strInstructions: String?,
    val strTags: String?,
    val strYoutube: String?,
    val strSource: String?,
    val ingredientsJson: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMealSummary(): MealSummary {
        return MealSummary(
            idMeal = idMeal,
            strMeal = strMeal,
            strMealThumb = strMealThumb,
            strCategory = strCategory,
            strArea = strArea,
            isFavorite = true
        )
    }

    fun toMealDetail(): MealDetail {
        val gson = Gson()
        val type = object : TypeToken<List<IngredientAmount>>() {}.type
        val parsedIngredients: List<IngredientAmount> = try {
            if (!ingredientsJson.isNullOrEmpty()) {
                gson.fromJson(ingredientsJson, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val tagsList = strTags?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        return MealDetail(
            idMeal = idMeal,
            strMeal = strMeal,
            strCategory = strCategory,
            strArea = strArea,
            strInstructions = strInstructions,
            strMealThumb = strMealThumb,
            strTags = tagsList,
            strYoutube = strYoutube,
            strSource = strSource,
            ingredients = parsedIngredients,
            isFavorite = true
        )
    }

    companion object {
        fun fromMealDetail(detail: MealDetail): FavoriteMealEntity {
            val gson = Gson()
            val ingredientsJsonStr = gson.toJson(detail.ingredients)
            val tagsStr = detail.strTags.joinToString(",")

            return FavoriteMealEntity(
                idMeal = detail.idMeal,
                strMeal = detail.strMeal,
                strMealThumb = detail.strMealThumb,
                strCategory = detail.strCategory,
                strArea = detail.strArea,
                strInstructions = detail.strInstructions,
                strTags = tagsStr,
                strYoutube = detail.strYoutube,
                strSource = detail.strSource,
                ingredientsJson = ingredientsJsonStr
            )
        }

        fun fromMealSummary(summary: MealSummary): FavoriteMealEntity {
            return FavoriteMealEntity(
                idMeal = summary.idMeal,
                strMeal = summary.strMeal,
                strMealThumb = summary.strMealThumb,
                strCategory = summary.strCategory,
                strArea = summary.strArea,
                strInstructions = null,
                strTags = null,
                strYoutube = null,
                strSource = null,
                ingredientsJson = null
            )
        }
    }
}
