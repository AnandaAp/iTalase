package com.anlian.italase.shared.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// LocalContext is usually a ContextWrapper chain rooted at the Activity, but
// previews and some hosts wrap it further — walk the chain until we hit one.
/**
 * Cari Activity di rantai `ContextWrapper`.
 *
 * Rekursi `tailrec` karena kedalaman rantai tidak dijamin (host preview membungkus berkali-kali);
 * `null` berarti tidak ada Activity sama sekali, dan pemanggil memperlakukannya sebagai no-op.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Aksi keluar aplikasi: tutup Activity yang menaungi komposisi.
 *
 * Tanpa Activity (preview/unit test) aksinya sengaja tidak melakukan apa-apa daripada
 * mematikan proses yang salah.
 */
@Composable
actual fun rememberAppExit(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            // ponytail: no Activity (unit test / preview host) -> no-op instead
            // of crashing. Swap for a logged no-op if this ever fires in prod.
            context.findActivity()?.finish()
        }
    }
}