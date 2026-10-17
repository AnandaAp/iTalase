package com.anlian.italase.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.anlian.italase.shared.ui.components.ExitConfirmDialog

/**
 * Back policy for the whole app.
 *
 * Bottom tabs are roots: switching tabs collapses the stack (see [AppNavHost]),
 * so back on a tab means "leave the app", never "return to the previous tab".
 * Sub-screens (Riwayat, Detail Struk, Pengeluaran) still pop normally — this
 * handler is disabled there so the default NavHost back wins.
 *
 * Dialog keluar sengaja tetap memakai android.ui.window.Dialog (jendela
 * terpisah) supaya kontennya tidak ikut ter-blur ketika [AppNavHost] mem-blur
 * konten app di belakangnya.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExitOnBackFromTab(
    currentRoute: String?,
    visible: Boolean,
    onShow: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BackHandler(enabled = Routes.isTabRoute(currentRoute) && !visible) { onShow() }

    if (visible) {
        ExitConfirmDialog(onConfirm = onConfirm, onDismiss = onDismiss)
    }
}
