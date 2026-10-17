package com.anlian.italase.shared.util

import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.local.entity.ProductEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductFilterTest {
    private val items = listOf(
        ProductEntity(id = 1, name = "Indomie Goreng", barcode = "899001", category = ProductCategories.FOOD, sellPrice = 3500, stock = 10),
        ProductEntity(id = 2, name = "Beras 5kg", barcode = "", category = ProductCategories.STAPLE, sellPrice = 68000, stock = 5),
        ProductEntity(id = 3, name = "Kopi Sachet", barcode = "899002", category = ProductCategories.DRINK, sellPrice = 2000, stock = 20),
    )

    @Test
    fun blankQueryReturnsAll() = assertEquals(3, filterProducts(items, "", null).size)

    @Test
    fun caseInsensitivePartialMatch() {
        val found = filterProducts(items, "INDO", null)
        assertEquals(listOf(1L), found.map { it.id })
    }

    @Test
    fun multiWordAllMustMatch() {
        assertEquals(listOf(1L), filterProducts(items, "indomie goreng", null).map { it.id })
        assertTrue(filterProducts(items, "indomie beras", null).isEmpty())
    }

    @Test
    fun extraSpacesIgnored() {
        assertEquals(listOf(2L), filterProducts(items, "  beras   5kg ", null).map { it.id })
    }

    @Test
    fun barcodeMatch() {
        assertEquals(listOf(3L), filterProducts(items, "899002", null).map { it.id })
    }

    @Test
    fun categoryTextMatch() {
        assertEquals(1, filterProducts(items, "sembako", null).size)
    }

    @Test
    fun categoryFilter() {
        assertEquals(listOf(2L), filterProducts(items, "", ProductCategories.STAPLE).map { it.id })
        assertTrue(filterProducts(items, "kopi", ProductCategories.FOOD).isEmpty())
    }

    @Test
    fun sortedAlphabeticallyCaseInsensitive() {
        // Beras 5kg (2), Indomie Goreng (1), Kopi Sachet (3)
        assertEquals(listOf(2L, 1L, 3L), filterProducts(items, "", null).map { it.id })
    }

    @Test
    fun outOfStockGoesLastAndEachGroupStaysAlphabetical() {
        val mixed = listOf(
            ProductEntity(id = 10, name = "Zebra", stock = 4),
            ProductEntity(id = 11, name = "apel", stock = 0),
            ProductEntity(id = 12, name = "Beras", stock = 0),
            ProductEntity(id = 13, name = "anggur", stock = 7),
        )
        // tersedia dulu — abjad case-insensitive: anggur, Zebra
        // lalu yang habis: apel, Beras
        assertEquals(listOf(13L, 10L, 11L, 12L), filterProducts(mixed, "", null).map { it.id })
    }

    @Test
    fun sortingSurvivesTheQueryAndCategoryFilter() {
        val mixed = listOf(
            ProductEntity(id = 20, name = "Sabun Mandi", stock = 0, category = ProductCategories.FOOD),
            ProductEntity(id = 21, name = "Sarden", stock = 3, category = ProductCategories.FOOD),
        )
        assertEquals(listOf(21L, 20L), filterProducts(mixed, "sa", ProductCategories.FOOD).map { it.id })
    }
}
