package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

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
