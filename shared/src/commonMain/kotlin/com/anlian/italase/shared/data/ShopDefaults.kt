package com.anlian.italase.shared.data

object ShopDefaults {
    const val UNIT = "pcs"
    const val CATEGORY = "Operasional"
    const val CUSTOMER = "Pelanggan"
}

object StockRules {
    const val LOW_STOCK_FALLBACK = 3
}

object ProductCategories {
    const val FOOD = "Makanan"
    const val DRINK = "Minuman"
    const val STAPLE = "Sembako"
    const val HOUSEHOLD = "Rumah Tangga"
    const val HEALTH = "Kesehatan"
    const val OTHER = "Lainnya"

    val ALL: List<String> = listOf(FOOD, DRINK, STAPLE, HOUSEHOLD, HEALTH, OTHER)
}
