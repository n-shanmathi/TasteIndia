package com.example.tasteindia.util

import com.example.tasteindia.domain.model.IngredientAmount

object IngredientParser {

    /**
     * Normalizes ingredient names and measurements.
     * Trims whitespace, capitalizes words properly, and filters out empty or invalid entries.
     */
    fun normalize(ingredientRaw: String?, measureRaw: String?): IngredientAmount? {
        val ingredient = ingredientRaw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?: return null

        val measure = measureRaw?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?: "To taste"

        val formattedIngredient = ingredient.split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        return IngredientAmount(
            name = formattedIngredient,
            measure = measure
        )
    }

    /**
     * Splits cooking instructions into distinct step strings.
     * Cleans up numbering like "STEP 1", "1.", line breaks, etc.
     */
    fun parseInstructionsToSteps(rawInstructions: String?): List<String> {
        if (rawInstructions.isNullOrBlank()) return emptyList()

        val lines = rawInstructions
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val steps = mutableListOf<String>()
        val currentStepBuilder = StringBuilder()

        for (line in lines) {
            // Check if line starts with step header like "STEP 1", "Step 1:", "1."
            val isStepHeader = line.matches(Regex("^(?i)(step\\s*\\d+|\\d+[.:)])(.*)$"))

            if (isStepHeader) {
                if (currentStepBuilder.isNotBlank()) {
                    steps.add(currentStepBuilder.toString().trim())
                    currentStepBuilder.clear()
                }
                // Strip the "Step X" prefix if desired, or keep clean text
                val cleanedLine = line.replace(Regex("^(?i)(step\\s*\\d+[.:]?|\\d+[.:)])\\s*"), "")
                if (cleanedLine.isNotBlank()) {
                    currentStepBuilder.append(cleanedLine)
                }
            } else {
                if (currentStepBuilder.isNotEmpty()) {
                    currentStepBuilder.append(" ").append(line)
                } else {
                    currentStepBuilder.append(line)
                }
            }
        }

        if (currentStepBuilder.isNotBlank()) {
            steps.add(currentStepBuilder.toString().trim())
        }

        // If paragraph splitting yielded only 1 huge block, split by sentence endings if needed
        if (steps.size == 1 && steps[0].length > 250) {
            val sentenceSteps = steps[0].split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (sentenceSteps.size > 1) {
                return sentenceSteps
            }
        }

        return if (steps.isNotEmpty()) steps else listOf(rawInstructions.trim())
    }
}
