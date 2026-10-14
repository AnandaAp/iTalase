package com.anlian.italase.shared.ui.preview

import com.anlian.italase.shared.data.local.dao.TopRow
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.DaySummary
import com.anlian.italase.shared.data.repository.ShopRepository

fun sampleProducts() = listOf(
    ProductEntity(id = 1, name = "Indomie Goreng", barcode = "8992388110101", category = "Makanan", buyPrice = 2800, sellPrice = 3500, stock = 48, unit = "pcs", minStock = 10),
    ProductEntity(id = 2, name = "Beras 5kg", barcode = "", category = "Sembako", buyPrice = 62000, sellPrice = 68000, stock = 2, unit = "karung", minStock = 5),
    ProductEntity(id = 3, name = "Minyak Goreng 1L", barcode = "", category = "Sembako", buyPrice = 18500, sellPrice = 21000, stock = 24, unit = "pcs", minStock = 6),
    ProductEntity(id = 4, name = "Gula Pasir 1kg", barcode = "", category = "Sembako", buyPrice = 16500, sellPrice = 18500, stock = 0, unit = "pcs", minStock = 5),
    ProductEntity(id = 5, name = "Kopi Sachet", barcode = "", category = "Minuman", buyPrice = 1200, sellPrice = 2000, stock = 120, unit = "pcs", minStock = 20),
)

fun sampleSummary() = DaySummary(omzet = 485000, profit = 96000, trxCount = 23, expense = 25000, bersih = 71000)

fun sampleTop() = listOf(
    TopRow("Indomie Goreng", 18, 63000),
    TopRow("Kopi Sachet", 15, 30000),
    TopRow("Minyak Goreng 1L", 4, 84000),
)

fun sampleSales() = listOf(
    SaleEntity(id = 12, dateMillis = 1, total = 45500, profit = 9000, payMethod = PayMethod.TUNAI, paidAmount = 45500, status = TxnStatus.LUNAS),
    SaleEntity(id = 11, dateMillis = 1, total = 68000, profit = 6000, payMethod = PayMethod.BON, paidAmount = 20000, customerName = "Bu RT", status = TxnStatus.BON),
    SaleEntity(id = 10, dateMillis = 1, total = 21000, profit = 2500, payMethod = PayMethod.TRANSFER, paidAmount = 21000, status = TxnStatus.LUNAS),
)

fun sampleSaleDetail() = ShopRepository.SaleDetail(
    sale = SaleEntity(
        id = 12, dateMillis = 1_700_000_000_000L, total = 45500, profit = 9000,
        payMethod = PayMethod.TUNAI, paidAmount = 45500, tendered = 50000,
        status = TxnStatus.LUNAS,
    ),
    items = listOf(
        SaleItemEntity(id = 1, saleId = 12, productId = 1, productName = "Indomie Goreng", category = "Makanan", qty = 10, buyPrice = 2800, sellPrice = 3500, subtotal = 35000, profit = 7000),
        SaleItemEntity(id = 2, saleId = 12, productId = 5, productName = "Kopi Sachet", category = "Minuman", qty = 3, buyPrice = 1200, sellPrice = 2000, subtotal = 6000, profit = 2400),
        SaleItemEntity(id = 3, saleId = 12, productId = 3, productName = "Minyak Goreng 1L", category = "Sembako", qty = 1, buyPrice = 18500, sellPrice = 21000, subtotal = 21000, profit = 2500),
    ),
)

fun sampleDebts() = listOf(
    DebtEntity(id = 1, customerName = "Bu RT", totalAmount = 68000, paidAmount = 20000, remaining = 48000, status = DebtStatus.BELUM_LUNAS, note = "Bon dari kasir #11"),
    DebtEntity(id = 2, customerName = "Pak Budi", totalAmount = 35000, paidAmount = 35000, remaining = 0, status = DebtStatus.LUNAS),
)

fun sampleExpenses() = listOf(
    ExpenseEntity(id = 1, name = "Plastik & kresek", category = "Operasional", amount = 15000),
    ExpenseEntity(id = 2, name = "Listrik", category = "Utilitas", amount = 50000),
    ExpenseEntity(id = 3, name = "Parkir kulakan", category = "Kulakan", amount = 10000),
)
