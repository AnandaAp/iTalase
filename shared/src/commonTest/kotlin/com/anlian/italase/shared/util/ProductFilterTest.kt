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

    // ════════════════════════════════════════════════════════════════════
    //  ProductList — urut sekali, saring per ketikan
    // ════════════════════════════════════════════════════════════════════

    /**
     * Implementasi LAMA, disalin apa adanya sebagai acuan. Kalau optimasi
     * mengubah perilaku, test ini yang menangkapnya — bukan mata user.
     */
    private fun referencePipeline(
        products: List<ProductEntity>,
        query: String,
        category: String?,
    ): List<ProductEntity> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return products
            .filter { p ->
                (category.isNullOrBlank() || p.category == category) &&
                    tokens.all { t ->
                        p.name.lowercase().contains(t) ||
                            p.barcode.lowercase().contains(t) ||
                            p.category.lowercase().contains(t)
                    }
            }
            .sortedWith(compareBy<ProductEntity>({ it.stock <= 0 }, { it.name.lowercase() }))
    }

    @Test
    fun productListMatchesTheOldPipelineOnRandomData() {
        val rnd = kotlin.random.Random(20261017)
        val alphabet = listOf("a", "b", "c", "B", "A", " ", "1", "goreng", "kopi", "5kg")
        val products = List(400) { i ->
            ProductEntity(
                id = i.toLong() + 1,
                name = List(rnd.nextInt(1, 4)) { alphabet.random(rnd) }.joinToString(""),
                barcode = if (rnd.nextBoolean()) List(rnd.nextInt(0, 4)) { alphabet.random(rnd) }.joinToString("") else "",
                category = listOf("Makanan", "Minuman", "Lain").random(rnd),
                stock = rnd.nextInt(0, 3),
            )
        }
        val queries = listOf("", " ", "a", "A", "ab", "ko", "goreng kopi", "  5kg  ", "1", "zzz", "GORENG")
        val categories = listOf(null, "", "Makanan", "Minuman", "TidakAda")

        var compared = 0
        for (q in queries) {
            for (c in categories) {
                assertEquals(
                    referencePipeline(products, q, c).map { it.id },
                    ProductList.of(products).visible(q, c).map { it.id },
                    "beda hasil untuk query=«$q» kategori=«$c»",
                )
                compared++
            }
        }
        assertEquals(queries.size * categories.size, compared)
    }

    /**
     * Warung dengan katalog besar. Yang diukur: biaya menyiapkan ([ProductList.of],
     * sekali per perubahan tabel) vs biaya menyaring (per ketikan user).
     * Angka acuan pada mesin dev: siapkan 5.000 baris ≈ 5-10 ms, satu saring ≈ 0,2 ms.
     */
    @Test
    fun bigCatalogueStaysFast() {
        val size = 5_000
        val products = List(size) { i ->
            ProductEntity(
                id = i.toLong() + 1,
                name = "Barang ${(i * 7919) % size} varian ${i % 37}",
                barcode = "899000000$i",
                category = listOf("Makanan", "Minuman", "Rumah Tangga", "Lainnya")[i % 4],
                sellPrice = 1_000L + (i % 90) * 500L,
                stock = if (i % 11 == 0) 0 else (i % 30) + 1,
            )
        }

        val prepare = kotlin.time.measureTime { ProductList.of(products) }
        val list = ProductList.of(products)

        // 20 ketikan = 20 saring. Ini yang terjadi saat penjual mengetik nama barang.
        val filter = kotlin.time.measureTime {
            repeat(20) { i -> list.visible("barang ${i}0", null) }
        }

        // Pembanding: pipeline LAMA atas data yang sama. Kalau angkanya tidak
        // terpisah jauh, berarti optimasinya tidak benar-benar menghemat apa pun.
        val oldWay = kotlin.time.measureTime {
            repeat(20) { i -> referencePipeline(products, "barang ${i}0", null) }
        }

        println(
            "[produk] $size baris → siapkan ${prepare.inWholeMilliseconds} ms, " +
                "20x saring (baru) ${filter.inWholeMilliseconds} ms, " +
                "20x saring (lama) ${oldWay.inWholeMilliseconds} ms",
        )

        // Kebenaran dulu, baru kecepatan: isi & urutan harus utuh.
        val all = list.visible("", null)
        assertEquals(size, all.size)
        assertEquals(all.filter { it.stock > 0 }.size + all.count { it.stock <= 0 }, all.size)
        assertTrue(all.indexOfFirst { it.stock <= 0 } >= 0, "harus ada yang habis di data uji")
        assertTrue(
            all.takeWhile { it.stock > 0 }.all { it.stock > 0 },
            "semua yang masih ada stok harus di depan",
        )
        assertTrue(
            all.dropWhile { it.stock > 0 }.all { it.stock <= 0 },
            "semua yang habis harus di belakang",
        )
        val names = all.takeWhile { it.stock > 0 }.map { it.name.lowercase() }
        assertEquals(names.sorted(), names, "tiap kelompok harus urut abjad")

        // Batas longgar — tujuannya menangkap kemunduran O(n²) atau pengurutan
        // ulang per ketikan, bukan mengukur mesin. Angka aslinya dicetak di atas.
        assertTrue(prepare.inWholeMilliseconds < 2_000, "menyiapkan 5.000 baris kelamaan: $prepare")
        assertTrue(filter.inWholeMilliseconds < 2_000, "20 saring atas 5.000 baris kelamaan: $filter")
    }
}
