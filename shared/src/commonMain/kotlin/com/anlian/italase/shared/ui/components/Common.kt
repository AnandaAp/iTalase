package com.anlian.italase.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.cat_all
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            subtitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String = "") {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (trailing.isNotBlank()) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(text: String, hint: String = "") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcon.WARNING.painter(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (hint.isNotBlank()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Lencana status dengan kontras terkontrol (pengganti AssistChip disabled
// yang warnanya dikalahkan state disabled).
@Composable
fun StatusChip(text: String, good: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (good) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (good) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// Kartu daftar: putih dengan garis tepi agar tegas di atas background krem.
@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = if (tonal) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        content()
    }
}

@Composable
fun MoneyBig(amount: String, label: String, light: Boolean = false) {
    val labelColor = if (light) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = if (light) Color.White else MaterialTheme.colorScheme.onSurface
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = FontWeight.Bold)
        Text(amount, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun CategoryChips(
    categories: List<String>,
    active: String?,
    onSelect: (String?) -> Unit,
) {
    if (categories.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "cat_all") {
            FilterChip(
                selected = active == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(Res.string.cat_all)) },
            )
        }
        items(categories, key = { "cat_$it" }) { c ->
            FilterChip(
                selected = active == c,
                onClick = { onSelect(if (active == c) null else c) },
                label = { Text(c) },
            )
        }
    }
}
