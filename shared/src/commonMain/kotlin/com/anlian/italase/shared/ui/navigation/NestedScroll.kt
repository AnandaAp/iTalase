package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// When listState is provided, scroll-up only collapses the bar if the list can
// actually move (or the bar is mid-collapse and should finish) — so short
// screens that fit on screen never hide the bar. Scroll-down always reaches
// the bar so a hidden bar can reappear.
@Composable
expect fun Modifier.nestedScrollConnection(
    scrollBehavior: TopAppBarScrollBehavior?,
    listState: LazyListState? = null,
): Modifier
