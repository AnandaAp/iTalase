package com.anlian.italase.shared.ui.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.nestedScrollConnection(
    scrollBehavior: TopAppBarScrollBehavior?,
    listState: LazyListState?,
): Modifier = this
