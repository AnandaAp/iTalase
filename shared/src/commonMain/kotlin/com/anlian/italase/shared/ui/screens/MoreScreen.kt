package com.anlian.italase.shared.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anlian.italase.shared.ui.components.AppIcon
import com.anlian.italase.shared.ui.components.painter
import com.anlian.italase.shared.ui.navigation.LocalBottomOverlayPadding
import com.anlian.italase.shared.ui.navigation.LocalTopOverlayPadding
import com.anlian.italase.shared.ui.navigation.Routes
import com.anlian.italase.shared.ui.navigation.TopBarInfoEffect
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cd_open
import italase.shared.generated.resources.more_about_d
import italase.shared.generated.resources.more_about_t
import italase.shared.generated.resources.more_expense_d
import italase.shared.generated.resources.more_history_d
import italase.shared.generated.resources.more_stock_d
import italase.shared.generated.resources.more_sub
import italase.shared.generated.resources.title_expenses
import italase.shared.generated.resources.title_history
import italase.shared.generated.resources.title_more
import italase.shared.generated.resources.title_products
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoreScreen(onNavigate: (String) -> Unit = {}) {
    TopBarInfoEffect(
        eyebrow = stringResource(Res.string.more_sub),
        title = stringResource(Res.string.title_more),
    )
    Column(
        Modifier
            .fillMaxSize()
            // Inset atas di LUAR scrollable. Konten mulai DI BAWAH top bar dan
            // tidak pernah masuk ke belakangnya — sama seperti layar dalam lain
            // yang scrollable-nya LazyColumn ber-weight. Ini wajib karena layar
            // ini TIDAK memasang frost di top bar (frost hanya di Beranda, lihat
            // AppNavHost): andai kontennya lewat di belakang bar, ia akan
            // bertabrakan dengan eyebrow + judul tanpa pelindung apa pun.
            .padding(top = LocalTopOverlayPadding.current + 8.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
            title = stringResource(Res.string.title_products),
            desc = stringResource(Res.string.more_stock_d),
            icon = AppIcon.INVENTORY,
        ) { onNavigate(Routes.STOCK) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcon.STOREFRONT.painter(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(Res.string.more_about_t),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(Res.string.more_about_d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // Card terakhir bisa digulir ke atas tab bar kaca yang mengambang.
        Spacer(Modifier.height(LocalBottomOverlayPadding.current))
    }
}

@Composable
private fun MoreRow(title: String, desc: String, icon: AppIcon, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
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
