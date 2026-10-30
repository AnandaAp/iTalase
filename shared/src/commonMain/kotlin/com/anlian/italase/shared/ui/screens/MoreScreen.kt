package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.GlassCard
import com.anlian.italase.shared.ui.components.enter
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.components.rememberAppear
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopAppBarScrollBehavior
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.Routes
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import com.anlian.italase.shared.ui.navigation.nestedScrollConnection
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cd_open
import italase.shared.generated.resources.more_about_d
import italase.shared.generated.resources.more_about_t
import italase.shared.generated.resources.more_app_info_d
import italase.shared.generated.resources.more_app_info_t
import italase.shared.generated.resources.more_db_security_d
import italase.shared.generated.resources.more_db_security_t
import italase.shared.generated.resources.more_expense_d
import italase.shared.generated.resources.more_history_d
import italase.shared.generated.resources.more_menu_count
import italase.shared.generated.resources.more_sub
import italase.shared.generated.resources.title_expenses
import italase.shared.generated.resources.title_history
import italase.shared.generated.resources.title_more
import org.jetbrains.compose.resources.stringResource

/**
 * Layar "Lainnya": pintu masuk ke Riwayat, Pengeluaran, dan info app.
 *
 * [onNavigate] menerima route string (`Routes.*`) alih-alih lambda per tujuan, jadi
 * menambah menu cukup menambah satu baris dan daftar jalur tetap di AppNavHost.
 */
@Composable
fun MoreScreen(onNavigate: (String) -> Unit = {}) {
    val appeared = rememberAppear()
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.value) {
        scrollBehavior?.state?.contentOffset = -scrollState.value.toFloat()
    }
    TopBarInfoEffect(
        eyebrow = stringResource(Res.string.more_menu_count, 5),
        title = stringResource(Res.string.title_more),
    )
    Column(
        Modifier
            .fillMaxSize()
            .enter(appeared, 0)
            .nestedScrollConnection(scrollBehavior, null)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(LocalTopOverlayPadding.current + 8.dp))
        MoreRow(
            title = stringResource(Res.string.title_history),
            desc = stringResource(Res.string.more_history_d),
            icon = AppIcon.RECEIPT,
        ) { onNavigate(Routes.HISTORY) }
        MoreRow(
            title = stringResource(Res.string.title_expenses),
            desc = stringResource(Res.string.more_expense_d),
            icon = AppIcon.PAYMENTS,
        ) { onNavigate(Routes.EXPENSE) }
        MoreRow(
            title = "Pengaturan",
            desc = "Metode pembayaran & QRIS kasir",
            icon = AppIcon.PAYMENTS,
        ) { onNavigate(Routes.SETTINGS) }
        MoreInfoCard(
            title = stringResource(Res.string.more_about_t),
            desc = stringResource(Res.string.more_about_d),
            icon = AppIcon.STOREFRONT,
        )
        MoreInfoCard(
            title = stringResource(Res.string.more_db_security_t),
            desc = stringResource(Res.string.more_db_security_d),
            icon = AppIcon.HANDSHAKE,
        )
        MoreInfoCard(
            title = stringResource(Res.string.more_app_info_t),
            desc = stringResource(Res.string.more_app_info_d),
            icon = AppIcon.INVENTORY,
        )
        // Ruang ekor untuk scroll menembus bottom tab bar
        Spacer(Modifier.height(LocalBottomOverlayPadding.current + 32.dp))
    }
}

/**
 * Satu baris menu interaktif: ikon berwarna primary, judul, deskripsi, lalu chevron penanda
 * "bisa dibuka".
 */
@Composable
private fun MoreRow(title: String, desc: String, icon: AppIcon, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon.painter(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                AppIcon.CHEVRON_RIGHT.painter(),
                contentDescription = stringResource(Res.string.cd_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Kartu informasi statis: menampilkan informasi keamanan database, status offline, atau versi app.
 */
@Composable
private fun MoreInfoCard(title: String, desc: String, icon: AppIcon) {
    GlassCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon.painter(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
