package com.anlian.italase.shared.di

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Activity yang sedang terlihat.
 *
 * `startActivity` dari Application context wajib memakai `FLAG_ACTIVITY_NEW_TASK`
 * dan membuka lembar berbagi di task yang salah; dari Activity, perilakunya benar.
 * `null` berarti tidak ada Activity (proses latar, preview), dan pemanggil harus
 * punya fallback.
 *
 * Ditaruh di paket `di` bersama [AndroidAppContext] karena keduanya membaca
 * jembatan yang sama.
 */
internal fun currentActivity(): Activity? =
    AndroidAppContext.currentActivity ?: AndroidAppContext.orNull()?.let { findActivityIn(it) }

/** Telusuri rantai `ContextWrapper` sampai ketemu Activity. */
private tailrec fun findActivityIn(context: Context): Activity? = when (context) {
    is Activity -> context.takeUnless { it.isFinishing || it.isDestroyed }
    is ContextWrapper -> findActivityIn(context.baseContext)
    else -> null
}
