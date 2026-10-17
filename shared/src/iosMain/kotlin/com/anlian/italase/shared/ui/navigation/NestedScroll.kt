package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Di iOS tidak ada yang perlu dipasang: `scrollBehavior` dan `listState` hanya dipakai
 * implementasi Android, dan mengembalikan `this` menjaga tanda tangan tetap sama di kedua
 * target.
 */
@Composable
actual fun Modifier.nestedScrollConnection(
    scrollBehavior: TopAppBarScrollBehavior?,
    listState: LazyListState?,
): Modifier = this
