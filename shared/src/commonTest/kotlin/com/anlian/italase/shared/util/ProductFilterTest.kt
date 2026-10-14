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
}
