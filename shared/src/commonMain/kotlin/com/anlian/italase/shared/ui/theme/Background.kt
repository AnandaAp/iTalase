package com.anlian.italase.shared.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

// Background aplikasi: gradien hangat berlapis + cahaya warna lembut.
// Mengikuti colorScheme sehingga adaptif (Material You di Android 12+,
// skema statis di iOS). Kartu putih di atasnya tetap kontras dan terbaca,
// dan bar translusen di App.kt memberi kesan berlapis ala liquid glass.
@Composable
fun WarungBackground(content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(c.surface, c.surfaceContainerLow, c.surfaceContainer),
            ),
        ),
    ) {
        Box(
            Modifier.size(300.dp).offset(x = 90.dp, y = (-70).dp).align(Alignment.TopEnd)
                .clip(CircleShape).background(c.primaryContainer.copy(alpha = 0.45f)),
        )
        Box(
            Modifier.size(220.dp).offset(x = (-70).dp, y = 180.dp).align(Alignment.CenterStart)
                .clip(CircleShape).background(c.secondaryContainer.copy(alpha = 0.50f)),
        )
        Box(
            Modifier.size(260.dp).offset(x = 40.dp, y = 120.dp).align(Alignment.BottomEnd)
                .clip(CircleShape).background(c.tertiaryContainer.copy(alpha = 0.25f)),
        )
        content()
    }
}
