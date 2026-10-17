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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

// Background aplikasi: gradien hangat berlapis + cahaya warna lembut.
// Skema warna statis dari brand (dynamic Color Material You sengaja tidak
// dipakai — lihat AppColorScheme.android.kt).
//
// Blob di sini DI-BLUR: kartu glass di atasnya translusen, jadi tepi blob yang
// keras akan menembus kartu sebagai garis miring dan terbaca sebagai artefak
// render. Dengan blur, blob jadi kabut cahaya ambient — tidak ada tepi, dan
// translucency kartu terbaca sebagai kaca.
//
// Diizinkan aturan mobile-fluid-ui-2026: blur hanya untuk permukaan STATIS.
// Blob ini tidak bergerak dan tidak ada di dalam container scroll, jadi nol
// biaya raster ulang per frame. Kalau background ini suatu saat bergerak,
// blur wajib dilepas.
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
            Modifier.size(420.dp).offset(x = 110.dp, y = (-140).dp).align(Alignment.TopEnd)
                .clip(CircleShape).background(c.primaryContainer.copy(alpha = 0.42f))
                .blur(70.dp),
        )
        Box(
            Modifier.size(340.dp).offset(x = (-110).dp, y = 160.dp).align(Alignment.CenterStart)
                .clip(CircleShape).background(c.secondaryContainer.copy(alpha = 0.38f))
                .blur(70.dp),
        )
        Box(
            Modifier.size(380.dp).offset(x = 60.dp, y = 140.dp).align(Alignment.BottomEnd)
                .clip(CircleShape).background(c.tertiaryContainer.copy(alpha = 0.22f))
                .blur(70.dp),
        )
        content()
    }
}
