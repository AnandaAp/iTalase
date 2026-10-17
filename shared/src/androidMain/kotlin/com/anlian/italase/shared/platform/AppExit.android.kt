package com.anlian.italase.shared.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// LocalContext is usually a ContextWrapper chain rooted at the Activity, but
// previews and some hosts wrap it further — walk the chain until we hit one.
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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