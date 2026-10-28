package com.example.tasteindia.data.remote

import com.example.tasteindia.domain.model.IngredientAmount
import com.example.tasteindia.domain.model.MealDetail
import com.example.tasteindia.domain.model.MealSummary
import com.example.tasteindia.util.IngredientParser
import com.google.gson.annotations.SerializedName

data class MealListDto(
    @SerializedName("meals")
    val meals: List<MealSummaryDto>?
)

data class MealSummaryDto(
    @SerializedName("idMeal")
    val idMeal: String?,
    @SerializedName("strMeal")
    val strMeal: String?,
    @SerializedName("strMealThumb")
    val strMealThumb: String?
) {
    fun toDomain(isFavorite: Boolean = false): MealSummary? {
        val id = idMeal?.takeIf { it.isNotBlank() } ?: return null
        val name = strMeal?.takeIf { it.isNotBlank() } ?: "Unknown Meal"
        return MealSummary(
            idMeal = id,
            strMeal = name,
            strMealThumb = strMealThumb,
            isFavorite = isFavorite
        )
    }
}

data class CategoryListDto(
    @SerializedName("meals")
    val meals: List<CategoryDto>?
)

data class CategoryDto(
    @SerializedName("strCategory")
    val strCategory: String?
)

data class IngredientListDto(
    @SerializedName("meals")
    val meals: List<IngredientDto>?
)

data class IngredientDto(
    @SerializedName("idIngredient")
    val idIngredient: String?,
    @SerializedName("strIngredient")
    val strIngredient: String?,
    @SerializedName("strDescription")
    val strDescription: String?,
    @SerializedName("strType")
    val strType: String?
)

data class MealDetailResponseDto(
    @SerializedName("meals")
    val meals: List<MealDetailDto>?
)

data class MealDetailDto(
    @SerializedName("idMeal") val idMeal: String?,
    @SerializedName("strMeal") val strMeal: String?,
    @SerializedName("strCategory") val strCategory: String?,
    @SerializedName("strArea") val strArea: String?,
    @SerializedName("strInstructions") val strInstructions: String?,
    @SerializedName("strMealThumb") val strMealThumb: String?,
    @SerializedName("strTags") val strTags: String?,
    @SerializedName("strYoutube") val strYoutube: String?,
    @SerializedName("strSource") val strSource: String?,

    @SerializedName("strIngredient1") val strIngredient1: String? = null,
    @SerializedName("strIngredient2") val strIngredient2: String? = null,
    @SerializedName("strIngredient3") val strIngredient3: String? = null,
    @SerializedName("strIngredient4") val strIngredient4: String? = null,
    @SerializedName("strIngredient5") val strIngredient5: String? = null,
    @SerializedName("strIngredient6") val strIngredient6: String? = null,
    @SerializedName("strIngredient7") val strIngredient7: String? = null,
    @SerializedName("strIngredient8") val strIngredient8: String? = null,
    @SerializedName("strIngredient9") val strIngredient9: String? = null,
    @SerializedName("strIngredient10") val strIngredient10: String? = null,
    @SerializedName("strIngredient11") val strIngredient11: String? = null,
    @SerializedName("strIngredient12") val strIngredient12: String? = null,
    @SerializedName("strIngredient13") val strIngredient13: String? = null,
    @SerializedName("strIngredient14") val strIngredient14: String? = null,
    @SerializedName("strIngredient15") val strIngredient15: String? = null,
    @SerializedName("strIngredient16") val strIngredient16: String? = null,
    @SerializedName("strIngredient17") val strIngredient17: String? = null,
    @SerializedName("strIngredient18") val strIngredient18: String? = null,
    @SerializedName("strIngredient19") val strIngredient19: String? = null,
    @SerializedName("strIngredient20") val strIngredient20: String? = null,

    @SerializedName("strMeasure1") val strMeasure1: String? = null,
    @SerializedName("strMeasure2") val strMeasure2: String? = null,
    @SerializedName("strMeasure3") val strMeasure3: String? = null,
    @SerializedName("strMeasure4") val strMeasure4: String? = null,
    @SerializedName("strMeasure5") val strMeasure5: String? = null,
    @SerializedName("strMeasure6") val strMeasure6: String? = null,
    @SerializedName("strMeasure7") val strMeasure7: String? = null,
    @SerializedName("strMeasure8") val strMeasure8: String? = null,
    @SerializedName("strMeasure9") val strMeasure9: String? = null,
    @SerializedName("strMeasure10") val strMeasure10: String? = null,
    @SerializedName("strMeasure11") val strMeasure11: String? = null,
    @SerializedName("strMeasure12") val strMeasure12: String? = null,
    @SerializedName("strMeasure13") val strMeasure13: String? = null,
    @SerializedName("strMeasure14") val strMeasure14: String? = null,
    @SerializedName("strMeasure15") val strMeasure15: String? = null,
    @SerializedName("strMeasure16") val strMeasure16: String? = null,
    @SerializedName("strMeasure17") val strMeasure17: String? = null,
    @SerializedName("strMeasure18") val strMeasure18: String? = null,
    @SerializedName("strMeasure19") val strMeasure19: String? = null,
    @SerializedName("strMeasure20") val strMeasure20: String? = null
) {
    fun extractIngredients(): List<IngredientAmount> {
        val pairs = listOf(
            strIngredient1 to strMeasure1,
            strIngredient2 to strMeasure2,
            strIngredient3 to strMeasure3,
            strIngredient4 to strMeasure4,
            strIngredient5 to strMeasure5,
            strIngredient6 to strMeasure6,
            strIngredient7 to strMeasure7,
            strIngredient8 to strMeasure8,
            strIngredient9 to strMeasure9,
            strIngredient10 to strMeasure10,
            strIngredient11 to strMeasure11,
            strIngredient12 to strMeasure12,
            strIngredient13 to strMeasure13,
            strIngredient14 to strMeasure14,
            strIngredient15 to strMeasure15,
            strIngredient16 to strMeasure16,
            strIngredient17 to strMeasure17,
            strIngredient18 to strMeasure18,
            strIngredient19 to strMeasure19,
            strIngredient20 to strMeasure20
        )

        return pairs.mapNotNull { (ing, meas) ->
            IngredientParser.normalize(ing, meas)
        }
    }

    fun toDomain(isFavorite: Boolean = false): MealDetail? {
        val id = idMeal?.takeIf { it.isNotBlank() } ?: return null
        val name = strMeal?.takeIf { it.isNotBlank() } ?: "Unknown Recipe"
        val tagsList = strTags?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        return MealDetail(
            idMeal = id,
            strMeal = name,
            strCategory = strCategory?.takeIf { it.isNotBlank() },
            strArea = strArea?.takeIf { it.isNotBlank() },
            strInstructions = strInstructions?.takeIf { it.isNotBlank() },
            strMealThumb = strMealThumb,
            strTags = tagsList,
            strYoutube = strYoutube?.takeIf { it.isNotBlank() },
            strSource = strSource?.takeIf { it.isNotBlank() },
            ingredients = extractIngredients(),
            isFavorite = isFavorite
        )
    }

    fun toMealSummary(isFavorite: Boolean = false): MealSummary? {
        val id = idMeal?.takeIf { it.isNotBlank() } ?: return null
        val name = strMeal?.takeIf { it.isNotBlank() } ?: "Unknown Meal"
        return MealSummary(
            idMeal = id,
            strMeal = name,
            strMealThumb = strMealThumb,
            strCategory = strCategory?.takeIf { it.isNotBlank() },
            strArea = strArea?.takeIf { it.isNotBlank() },
            isFavorite = isFavorite
        )
    }
}
