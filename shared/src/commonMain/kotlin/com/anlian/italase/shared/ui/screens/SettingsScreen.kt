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

/**
 * Layar Hub Pengaturan Toko (AUD-107).
 *
 * Titik masuk ke konfigurasi toko: metode pembayaran & QRIS kasir.
 */
@Composable
fun SettingsScreen(onNavigate: (String) -> Unit = {}) {
    val appeared = rememberAppear()
    val scrollBehavior = LocalTopAppBarScrollBehavior.current
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.value) {
        scrollBehavior?.state?.contentOffset = -scrollState.value.toFloat()
    }

    TopBarInfoEffect(
        eyebrow = "PENGATURAN",
        title = "Pengaturan Toko",
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

        SettingsRow(
            title = "Metode Pembayaran",
            desc = "Kelola QRIS kasir dan opsi pembayaran nontunai",
            icon = AppIcon.PAYMENTS,
        ) { onNavigate(Routes.SETTINGS_PAYMENT) }

        // Ruang ekor untuk scroll menembus bottom tab bar
        Spacer(Modifier.height(LocalBottomOverlayPadding.current + 32.dp))
    }
}

@Composable
private fun SettingsRow(
    title: String,
    desc: String,
    icon: AppIcon,
    onClick: () -> Unit,
) {
    GlassCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon.painter(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 14.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                AppIcon.CHEVRON_RIGHT.painter(),
                contentDescription = "Buka",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
