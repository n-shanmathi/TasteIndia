package com.example.tasteindia

import com.example.tasteindia.util.IngredientParser
import org.junit.Assert.*
import org.junit.Test

class IngredientParserTest {

    @Test
    fun testIngredientAndMeasurementNormalization() {
        val ingredientRaw = "  CHICKEN BREAST  "
        val measureRaw = "  500g  "

        val result = IngredientParser.normalize(ingredientRaw, measureRaw)

        assertNotNull(result)
        assertEquals("Chicken Breast", result?.name)
        assertEquals("500g", result?.measure)
    }

    @Test
    fun testNullOrEmptyIngredientReturnsNull() {
        val result1 = IngredientParser.normalize("", "1 tsp")
        val result2 = IngredientParser.normalize("null", "1 tsp")
        val result3 = IngredientParser.normalize(null, "1 tsp")

        assertNull(result1)
        assertNull(result2)
        assertNull(result3)
    }

    @Test
    fun testEmptyMeasurementDefaultsToToTaste() {
        val result = IngredientParser.normalize("Garam Masala", "  ")

        assertNotNull(result)
        assertEquals("Garam Masala", result?.name)
        assertEquals("To taste", result?.measure)
    }

    @Test
    fun testParseInstructionsToSteps() {
        val rawInstructions = """
            Preheat oven to 350 degrees F.
            STEP 1: Mix soy sauce, water, and brown sugar in a bowl.
            STEP 2: Bake chicken for 45 minutes.
            STEP 3: Serve warm with rice.
        """.trimIndent()

        val steps = IngredientParser.parseInstructionsToSteps(rawInstructions)

        assertEquals(4, steps.size)
        assertTrue(steps[1].contains("Mix soy sauce"))
        assertTrue(steps[2].contains("Bake chicken"))
    }
}
