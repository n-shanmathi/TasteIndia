package com.example.tasteindia.domain.model

data class CookStep(
    val stepNumber: Int,
    val totalSteps: Int,
    val instructionText: String,
    val mealName: String,
    val thumbnailUrl: String?
)
