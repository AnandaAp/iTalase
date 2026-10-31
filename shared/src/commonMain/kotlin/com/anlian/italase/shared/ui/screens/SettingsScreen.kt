package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
 * Titik masuk ke konfigurasi toko: metode pembayaran & QRIS kasir,
 * dengan tata letak responsif adaptive landscape & docking top bar liquid glass.
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val topSpacerHeight = LocalTopOverlayPadding.current + (if (isLandscape) 6.dp else 14.dp)
        val bottomSpacerHeight = LocalBottomOverlayPadding.current + (if (isLandscape) 14.dp else 24.dp)

        Column(
            Modifier
                .fillMaxSize()
                .nestedScrollConnection(scrollBehavior, null)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(topSpacerHeight))

            if (isLandscape) {
                // Adaptive 2-Column Split-Pane untuk Mode Landscape & Tablet
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SettingsRow(
                            title = "Metode Pembayaran",
                            desc = "Kelola QRIS kasir dan opsi pembayaran nontunai",
                            icon = AppIcon.PAYMENTS,
                            modifier = Modifier.enter(appeared, 0),
                        ) { onNavigate(Routes.SETTINGS_PAYMENT) }

                        SettingsRow(
                            title = "Format Struk & Cetak",
                            desc = "Format struk belanja, header toko & thermal printer",
                            icon = AppIcon.RECEIPT,
                            modifier = Modifier.enter(appeared, 1),
                        ) {}
                    }

                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SettingsRow(
                            title = "Keamanan & Database",
                            desc = "Basis data offline lokal & enkripsi SQLCipher",
                            icon = AppIcon.HANDSHAKE,
                            modifier = Modifier.enter(appeared, 2),
                        ) {}

                        SettingsRow(
                            title = "Tentang Toko & App",
                            desc = "Informasi versi aplikasi POS & profil warung",
                            icon = AppIcon.STOREFRONT,
                            modifier = Modifier.enter(appeared, 3),
                        ) {}
                    }
                }
            } else {
                // Layout Vertikal untuk Mode Portrait
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SettingsRow(
                        title = "Metode Pembayaran",
                        desc = "Kelola QRIS kasir dan opsi pembayaran nontunai",
                        icon = AppIcon.PAYMENTS,
                        modifier = Modifier.enter(appeared, 0),
                    ) { onNavigate(Routes.SETTINGS_PAYMENT) }

                    SettingsRow(
                        title = "Format Struk & Cetak",
                        desc = "Format struk belanja, header toko & thermal printer",
                        icon = AppIcon.RECEIPT,
                        modifier = Modifier.enter(appeared, 1),
                    ) {}

                    SettingsRow(
                        title = "Keamanan & Database",
                        desc = "Basis data offline lokal & enkripsi SQLCipher",
                        icon = AppIcon.HANDSHAKE,
                        modifier = Modifier.enter(appeared, 2),
                    ) {}

                    SettingsRow(
                        title = "Tentang Toko & App",
                        desc = "Informasi versi aplikasi POS & profil warung",
                        icon = AppIcon.STOREFRONT,
                        modifier = Modifier.enter(appeared, 3),
                    ) {}
                }
            }

            Spacer(Modifier.height(bottomSpacerHeight))
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    desc: String,
    icon: AppIcon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(onClick = onClick, modifier = modifier) {
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
