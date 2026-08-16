package com.appotato.shared.ingredients

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IngredientResolverTest {

    @Test
    fun `Given tags read general to specific When resolving Then the most specific one wins`() {
        // Both tags match something. Read left to right this is milk; read the way the taxonomy is
        // actually ordered it is what the product really is.
        assertEquals("yogurt", ingredientFromTags(listOf("en:milks", "en:yogurts")))
    }

    @Test
    fun `Given one tag matching two words When resolving Then the longer word wins`() {
        // `milk` and `chocolate` both occur in this tag; only one of them is what is in the packet.
        assertEquals("chocolate", ingredientFromTags(listOf("en:milk-chocolates")))
    }

    @Test
    fun `Given tags matching nothing When resolving Then it returns null`() {
        assertNull(ingredientFromTags(listOf("en:plant-based-foods-and-beverages", "en:snacks")))
    }

    @Test
    fun `Given no tags When resolving Then it returns null`() {
        assertNull(ingredientFromTags(emptyList()))
    }

    @Test
    fun `Given a name with the brand and the pack size When resolving Then the food is found`() {
        assertEquals("milk", ingredientFromName("Mleko UHT 3,2%"))
    }

    @Test
    fun `Given a stem inside another word When resolving Then it does not match`() {
        // `ser` is cheese, but `deser` is not — a stem only ever matches at the start of a word.
        assertNull(ingredientFromName("Deser waniliowy"))
    }

    @Test
    fun `Given a stem starting a word When resolving Then it matches`() {
        assertEquals("cheese", ingredientFromName("Serek wiejski"))
    }

    @Test
    fun `Given two stems matching one word When resolving Then the longer stem wins`() {
        // `makaron` starts with `maka`, which is flour. The longer stem is the honest one.
        assertEquals("pasta", ingredientFromName("Makaron pełnoziarnisty"))
    }

    @Test
    fun `Given a name typed without Polish diacritics When resolving Then it still matches`() {
        assertEquals(ingredientFromName("Śmietana 18%"), ingredientFromName("Smietana 18%"))
        assertEquals("cream", ingredientFromName("Smietana 18%"))
    }

    @Test
    fun `Given an inflected Polish name When resolving Then the stem still matches`() {
        listOf("mleko", "mleka", "mleku", "Mleczko kokosowe").forEach { name ->
            assertEquals("milk", ingredientFromName(name), "failed for: $name")
        }
    }

    @Test
    fun `Given an English name When resolving Then it matches the same code as the Polish one`() {
        assertEquals(ingredientFromName("chicken breast"), ingredientFromName("Pierś z kurczaka"))
        assertEquals("chicken", ingredientFromName("chicken breast"))
    }

    @Test
    fun `Given a name matching nothing When resolving Then it returns null`() {
        assertNull(ingredientFromName("Zestaw upominkowy"))
    }

    @Test
    fun `Given a blank name When resolving Then it returns null`() {
        assertNull(ingredientFromName(""))
        assertNull(ingredientFromName("   "))
        assertNull(ingredientFromName("3,2%"))
    }

    @Test
    fun `Given the vocabulary When inspected Then every code appears exactly once`() {
        // Codes are written into the database and into the suggestion cache key. A duplicate would
        // make one of the two entries unreachable, silently.
        val duplicates = Vocabulary.groupBy { it.code }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet(), duplicates)
    }

    @Test
    fun `Given the vocabulary When inspected Then no stem carries a Polish diacritic`() {
        // Names are folded before matching, so a stem written with one could never be reached.
        val unfolded = Vocabulary.flatMap { it.stems }.filter { stem -> stem.any { it.code > 127 } }
        assertEquals(emptyList(), unfolded)
    }
}
