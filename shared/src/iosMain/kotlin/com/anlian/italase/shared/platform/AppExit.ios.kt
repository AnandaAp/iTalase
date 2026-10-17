package com.anlian.italase.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoProcessTerminateWithSuccess

// iOS: no Activity to finish, so ask Foundation to terminate the process.
// ponytail: non-persistent data in the cart is lost on relaunch — acceptable
// because checkout already commits to SQLite before the dialog can appear.
/**
 * Aksi keluar aplikasi: minta iOS menghentikan proses (lihat catatan di bawah soal data yang
 * hilang).
 */
@Composable
actual fun rememberAppExit(): () -> Unit = remember {
    {
        NSProcessInfo.processInfo.terminateWithSuccess(NSProcessInfoProcessTerminateWithSuccess)
    }
}