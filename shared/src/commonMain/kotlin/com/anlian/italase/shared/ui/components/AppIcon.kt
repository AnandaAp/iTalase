package com.anlian.italase.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.add
import italase.shared.generated.resources.arrow_back
import italase.shared.generated.resources.arrow_back_ios_new
import italase.shared.generated.resources.chevron_right
import italase.shared.generated.resources.delete
import italase.shared.generated.resources.edit
import italase.shared.generated.resources.handshake
import italase.shared.generated.resources.home
import italase.shared.generated.resources.inventory_2
import italase.shared.generated.resources.more_horiz
import italase.shared.generated.resources.payments
import italase.shared.generated.resources.receipt_long
import italase.shared.generated.resources.remove
import italase.shared.generated.resources.search
import italase.shared.generated.resources.shopping_cart
import italase.shared.generated.resources.storefront
import italase.shared.generated.resources.warning
import org.jetbrains.compose.resources.painterResource

// Enum agar parameter ikon di composable tetap stabil
// (DrawableResource/Painter sebagai parameter membuat composable non-skippable).
/**
 * Ikon app sebagai enum — pengganti langsung `DrawableResource`/`Painter` di
 * parameter composable. Tipe stabil & comparable membuat composable penerimanya
 * tetap skippable; resource mentah membatalkan itu. Resolusi ke drawable
 * dikerjakan [painter].
 */
enum class AppIcon {
    ARROW_BACK,
    ARROW_BACK_IOS_NEW,
    HOME,    SHOPPING_CART,
    INVENTORY,
    RECEIPT,
    HANDSHAKE,
    PAYMENTS,
    MORE,
    ADD,
    REMOVE,
    SEARCH,
    DELETE,
    EDIT,
    CHEVRON_RIGHT,
    WARNING,
    STOREFRONT,
}

/**
 * Petakan [AppIcon] ke resource drawable-nya. Satu-satunya tempat pemetaan ini
 * hidup; menambah entri enum wajib menambah cabang di sini, kalau tidak `when`
 * jadi tidak ekshaustif.
 */
@Composable
fun AppIcon.painter(): Painter = painterResource(
    when (this) {
        AppIcon.ARROW_BACK -> Res.drawable.arrow_back
        AppIcon.ARROW_BACK_IOS_NEW -> Res.drawable.arrow_back_ios_new
        AppIcon.HOME -> Res.drawable.home
        AppIcon.SHOPPING_CART -> Res.drawable.shopping_cart
        AppIcon.INVENTORY -> Res.drawable.inventory_2
        AppIcon.RECEIPT -> Res.drawable.receipt_long
        AppIcon.HANDSHAKE -> Res.drawable.handshake
        AppIcon.PAYMENTS -> Res.drawable.payments
        AppIcon.MORE -> Res.drawable.more_horiz
        AppIcon.ADD -> Res.drawable.add
        AppIcon.REMOVE -> Res.drawable.remove
        AppIcon.SEARCH -> Res.drawable.search
        AppIcon.DELETE -> Res.drawable.delete
        AppIcon.EDIT -> Res.drawable.edit
        AppIcon.CHEVRON_RIGHT -> Res.drawable.chevron_right
        AppIcon.WARNING -> Res.drawable.warning
        AppIcon.STOREFRONT -> Res.drawable.storefront
    },
)
