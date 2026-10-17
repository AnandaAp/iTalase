package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Sambungan scroll container layar ke scroll behavior top bar-nya, sehingga
 * frost top bar ikut bergerak saat layar digulir. Dipakai semua layar ber-top
 * bar, lewat `nestedScrollConnection(scrollBehavior, listState)`.
 *
 * @param listState diterima hanya demi signature yang sama dengan `expect`-nya;
 *   di Android tidak dipakai — tanpa gating, Compose sendiri yang mengurus
 *   scroll simultan antar container. Kalau behavior null, modifier ini no-op.
 */
@Composable
actual fun Modifier.nestedScrollConnection(
    scrollBehavior: TopAppBarScrollBehavior?,
    listState: LazyListState?,
): Modifier {
    if (scrollBehavior == null) return this
    if (listState == null) return nestedScroll(scrollBehavior.nestedScrollConnection)
    
    // Standard connection - no gating. Let Compose handle simultaneous scroll.
    return nestedScroll(scrollBehavior.nestedScrollConnection)
}
