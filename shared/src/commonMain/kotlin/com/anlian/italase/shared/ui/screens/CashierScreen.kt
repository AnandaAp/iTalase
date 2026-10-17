package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.CategoryChips
import com.anlian.italase.shared.ui.components.GLASS_RADIUS
import com.anlian.italase.shared.ui.components.GlassButton
import com.anlian.italase.shared.ui.components.GlassChip
import com.anlian.italase.shared.ui.components.GlassDialog
import com.anlian.italase.shared.ui.components.GlassEmptyState
import com.anlian.italase.shared.ui.components.GlassIconButton
import com.anlian.italase.shared.ui.components.GlassRow
import com.anlian.italase.shared.ui.components.GlassTextField
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.glassSurface
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.aboveTabBar
import com.anlian.italase.shared.ui.navigation.bottomActionTailPadding
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import com.anlian.italase.shared.util.formatRupiah
import com.anlian.italase.shared.util.parseRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.action_cancel
import italase.shared.generated.resources.action_close
import italase.shared.generated.resources.action_pay
import italase.shared.generated.resources.action_save
import italase.shared.generated.resources.cash_add_cart
import italase.shared.generated.resources.cash_cart_items
import italase.shared.generated.resources.cash_change
import italase.shared.generated.resources.cash_customer
import italase.shared.generated.resources.cash_empty_h
import italase.shared.generated.resources.cash_empty_t
import italase.shared.generated.resources.cash_m_cash
import italase.shared.generated.resources.cash_m_transfer
import italase.shared.generated.resources.cash_paid
import italase.shared.generated.resources.cash_pay_title
import italase.shared.generated.resources.cash_product_line
import italase.shared.generated.resources.cash_search
import italase.shared.generated.resources.cash_short
import italase.shared.generated.resources.cash_short_hint
import italase.shared.generated.resources.cash_sub
import italase.shared.generated.resources.cd_add
import italase.shared.generated.resources.cd_remove
import italase.shared.generated.resources.cd_search
import italase.shared.generated.resources.prod_out
import italase.shared.generated.resources.tab_debt
import italase.shared.generated.resources.title_cashier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.stringResource

// Tinggi perkiraan kartu checkout. Dipakai untuk ruang ekor list (lihat
// bottomActionTailPadding), bukan untuk tinggi layout kartunya sendiri.
private val CHECKOUT_CARD_HEIGHT = 148.dp

@Composable
fun CashierScreen(
    query: String,
    onQuery: (String) -> Unit,
    categories: ImmutableList<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit,
    list: ImmutableList<ProductEntity>,
    cart: ImmutableMap<Long, Int>,
    lines: ImmutableList<CartLine>,
    total: Long,
    message: String,
    showPayDialog: Boolean,
    onAdd: (ProductEntity) -> Unit = {},
    onSetQty: (Long, Int) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
    onPay: () -> Unit = {},
    onDismissPay: () -> Unit = {},
    onConfirmPay: (Long, String, String) -> Unit = { _, _, _ -> },
) {
    CashierContent(
        query = query,
        onQuery = onQuery,
        categories = categories,
        activeCategory = activeCategory,
        onCategory = onCategory,
        list = list,
        cart = cart,
        lines = lines,
        total = total,
        message = message,
        onAdd = onAdd,
        onSetQty = onSetQty,
        onClear = onClear,
        onPay = onPay,
    )

    if (showPayDialog) {
        PayDialog(
            total = total,
            onDismiss = onDismissPay,
            onConfirm = onConfirmPay,
        )
    }
}

@Composable
fun CashierContent(
    query: String,
    onQuery: (String) -> Unit,
    categories: List<String>,
    activeCategory: String?,
    onCategory: (String?) -> Unit,
    list: List<ProductEntity>,
    cart: Map<Long, Int>,
    lines: List<CartLine>,
    total: Long,
    message: String,
    onAdd: (ProductEntity) -> Unit = {},
    onSetQty: (Long, Int) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
    onPay: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val listState = rememberLazyListState()
    // Layar ini punya kartu checkout yang mengambang DI ATAS tab bar. Jadi list
    // butuh dua lapis ruang gulir: tinggi tab bar (agar item bisa lewat di
    // belakang tab bar) + tinggi kartu checkout (agar item terakhir tidak
    // tertutup permanen oleh keduanya).
    // Jarak & tinggi tab bar datang dari helper yang SAMA dengan layar ber-FAB,
    // supaya jarak aksi-vs-tab-bar identik di seluruh app.
    val checkoutSpace = bottomActionTailPadding(CHECKOUT_CARD_HEIGHT)

    // Header layar hidup di top bar. Diletakkan DI LUAR Column: nilai ini tidak
    // merender apa pun, jadi kalau jadi child Column ia tetap memakan jarak
    // Arrangement.spacedBy di dua sisinya.
    val appeared = rememberAppear()
    TopBarInfoEffect(
        eyebrow = stringResource(Res.string.cash_sub),
        title = stringResource(Res.string.title_cashier),
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
            GlassTextField(
                value = query,
                onValueChange = onQuery,
                label = stringResource(Res.string.cash_search),
                leadingIcon = {
                    Icon(
                        AppIcon.SEARCH.painter(),
                        contentDescription = stringResource(Res.string.cd_search),
                    )
                },
                modifier = Modifier.enter(appeared, 0),
            )
            CategoryChips(
                categories = categories,
                active = activeCategory,
                onSelect = onCategory,
            )
            if (list.isEmpty()) {
                GlassEmptyState(
                    stringResource(Res.string.cash_empty_t),
                    stringResource(Res.string.cash_empty_h),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .nestedScrollConnection(scrollBehavior, listState).enter(appeared, 1),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Ruang ekstra di ekor list: item terakhir bisa digulir ke ATAS
                    // tab bar dan checkout card yang mengambang, bukan berhenti
                    // tertutup di bawahnya.
                    contentPadding = checkoutSpace,
                ) {
                    // Tanpa `take(60)`: dulu daftarnya dipotong 60 baris, jadi
                    // produk ke-61 ke atas TIDAK BISA dijangkau dengan scroll —
                    // dan `take()` sendiri menyalin ulang list di tiap
                    // recomposisi. LazyColumn hanya menyusun baris yang terlihat,
                    // jadi 5.000 baris sama murahnya dengan 20 di layar.
                    items(list, key = { it.id }) { p ->
                        val qty = cart[p.id] ?: 0
                        GlassRow(
                            title = p.name,
                            subtitle = stringResource(
                                Res.string.cash_product_line,
                                formatRupiah(p.sellPrice),
                                p.stock,
                            ),
                            // Baris yang sudah masuk keranjang diberi tint brand —
                            // bahasa yang sama dengan pill aktif di tab bar.
                            tint = if (qty > 0) scheme.primaryContainer else scheme.surface,
                            alpha = if (qty > 0) 0.95f else 0.88f,
                            trailing = {
                                if (qty == 0) {
                                    // Aksi utama baris ini. Fill brand pekat + gloss
                                    // PUTIH di tepi atas: gloss cuma terbaca di atas
                                    // permukaan gelap — di atas mint, putih@0.30 jadi
                                    // #E6F4EA alias tak terlihat (itu sebabnya versi
                                    // mint terlihat "tumpul" dan tanpa rim kaca).
                                    // Kontras label putih di atas primary = 6.52:1.
                                    // Stok habis → labelnya BERGANTI KATA, bukan cuma
                                    // dipudarkan, supaya affordance-nya jelas.
                                    GlassButton(
                                        text = if (p.stock > 0) {
                                            stringResource(Res.string.cash_add_cart)
                                        } else {
                                            stringResource(Res.string.prod_out)
                                        },
                                        onClick = { onAdd(p) },
                                        enabled = p.stock > 0,
                                        height = 42.dp,
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        GlassIconButton(
                                            icon = AppIcon.REMOVE,
                                            contentDescription = stringResource(Res.string.cd_remove),
                                            onClick = { onSetQty(p.id, qty - 1) },
                                            size = 38.dp,
                                        )
                                        Text(
                                            "$qty",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        GlassIconButton(
                                            icon = AppIcon.ADD,
                                            contentDescription = stringResource(Res.string.cd_add),
                                            onClick = { onSetQty(p.id, qty + 1) },
                                            enabled = qty < p.stock,
                                            size = 38.dp,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        } // Column

        // Checkout card mengambang di atas konten; tab bar ada di layer lebih
        // atas lagi (lihat AppNavHost).
        CheckoutCard(
            lines = lines.size,
            total = total,
            message = message,
            onClear = onClear,
            onPay = onPay,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .aboveTabBar()
                    .enter(appeared, 2),
        )
    } // Box
}

/**
 * Kartu total + aksi bayar. Memakai gaya hero yang sama seperti kartu omzet di
 * Beranda: tint brand pekat, tapi tetap dilapisi resep kaca (sheen + veil), jadi
 * ia terbaca satu keluarga dengan kartu lain — bukan blok warna Material.
 */
@Composable
private fun CheckoutCard(
    lines: Int,
    total: Long,
    message: String,
    onClear: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .glassSurface(
                // Radius kontainer HARUS > radius anak. Pill tombol 48dp → radius
                // efektif 24dp, jadi kartu di 28dp. Kalau sama-sama 24dp, tepi
                // pill terlihat terjepit di sudut kartu.
                shape = RoundedCornerShape(GLASS_RADIUS),
                tint = scheme.primary,
                // 0.94 hampir opak: kartu terbaca sebagai blok warna solid dan
                // keluar dari bahasa kaca. 0.90 + gloss lebih kuat supaya tetap
                // satu keluarga dengan kartu lain. Batasnya: di bawah ~0.88
                // konten di belakang mulai bocor dan kartunya jadi kotor.
                alpha = 0.90f,
                elevation = 22.dp,
                specular = 0.34f,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(Res.string.cash_cart_items, lines),
                modifier = Modifier.alignByBaseline(),
                color = scheme.onPrimary.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                formatRupiah(total),
                modifier = Modifier.alignByBaseline(),
                color = scheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (message.isNotBlank()) {
            Text(
                message,
                color = scheme.onPrimary.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // Semua tombol di kartu ini mati selama belum ada produk yang masuk
        // keranjang. Disabled di kartu GELAP = fill lebih GELAP (recessed),
        // label tetap putih penuh: terukur 8.64:1, dan beda 7.37:1 terhadap
        // state enabled (sand) sehingga transisi mati → menyala sangat jelas.
        val barEnabled = lines > 0
        val disabledFill = scheme.onSurface.copy(alpha = 0.30f)
        // Label disabled ikut diredupkan. Sebelumnya putih PENUH di atas fill
        // gelap (9.00:1) — kontrasnya bagus, tapi labelnya jadi terlihat solid
        // sehingga tombolnya terbaca masih aktif. 0.66 → 4.96:1: jelas redup,
        // masih lolos AA. Batasnya 0.60 (4.38:1) — di bawah itu gagal.
        val disabledInk = scheme.onPrimary.copy(alpha = 0.66f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // SEKUNDER — ghost: tint tipis 0.10, TANPA gloss dan TANPA bayangan.
            // Sebelumnya 0.16: fill terlalu terang sehingga teks putih cuma
            // 4.54:1 (nyaris gagal AA), dan blob-nya lebih berat dari tombol
            // utama sehingga mata tertarik ke "Batal". Di 0.10 → 5.19:1.
            GlassButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onClear,
                enabled = barEnabled,
                container = scheme.onPrimary.copy(alpha = 0.10f),
                content = scheme.onPrimary,
                elevation = 0.dp,
                specular = 0f,
                disabledContainer = disabledFill,
                disabledContent = disabledInk,
                modifier = Modifier.weight(1f),
            )
            // PRIMER — satu-satunya elemen yang menyala: sand @8.86:1, bergloss,
            // dan paling tinggi. Beda luminance vs ghost 4.42:1 → jelas dominan.
            GlassButton(
                text = stringResource(Res.string.action_pay),
                onClick = onPay,
                enabled = barEnabled,
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer,
                elevation = 14.dp,
                disabledContainer = disabledFill,
                disabledContent = disabledInk,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PayDialog(total: Long, onDismiss: () -> Unit, onConfirm: (Long, String, String) -> Unit) {
    var paidText by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PayMethod.TUNAI) }
    val paid = parseRupiah(paidText)
    val scheme = MaterialTheme.colorScheme

    GlassDialog(onDismiss = onDismiss) {
        Text(
            stringResource(Res.string.cash_pay_title, formatRupiah(total)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        GlassTextField(
            value = customer,
            onValueChange = { customer = it },
            label = stringResource(Res.string.cash_customer),
        )
        Spacer(Modifier.height(10.dp))
        GlassTextField(
            value = paidText,
            onValueChange = { paidText = it },
            label = stringResource(Res.string.cash_paid),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip(
                text = stringResource(Res.string.cash_m_cash),
                selected = method == PayMethod.TUNAI,
                onClick = { method = PayMethod.TUNAI },
            )
            GlassChip(
                text = stringResource(Res.string.cash_m_transfer),
                selected = method == PayMethod.TRANSFER,
                onClick = { method = PayMethod.TRANSFER },
            )
            GlassChip(
                text = stringResource(Res.string.tab_debt),
                selected = method == PayMethod.BON,
                onClick = { method = PayMethod.BON },
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .glassSurface(
                    shape = RoundedCornerShape(16.dp),
                    tint = scheme.surfaceContainerLow,
                    alpha = 0.90f,
                    elevation = 6.dp,
                    specular = 0.18f,
                    veil = false,
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (paid >= total) stringResource(Res.string.cash_change)
                else stringResource(Res.string.cash_short),
                color = scheme.onSurfaceVariant,
            )
            Text(
                formatRupiah(if (paid >= total) paid - total else total - paid),
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
        }
        if (method != PayMethod.BON && paid < total) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.cash_short_hint),
                color = scheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                text = stringResource(Res.string.action_close),
                onClick = onDismiss,
                container = scheme.onSurface.copy(alpha = 0.06f),
                content = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            GlassButton(
                text = stringResource(Res.string.action_save),
                onClick = { onConfirm(paid, method, customer) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
