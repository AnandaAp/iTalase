package com.anlian.italase.shared.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Jembatan satu arah dari layer Android ke Koin.
 *
 * Koin-annotation tidak bisa menyuntikkan `Context`, jadi nilainya diisi di
 * `Application.onCreate` sebelum graph dibangun. `lateinit` dipakai supaya pembacaan terlalu
 * dini gagal keras saat start, bukan menyerahkan Context kosong yang errornya baru muncul
 * jauh di dalam Room.
 */
object AndroidAppContext {
    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var lifecycleRegistered: Boolean = false

    /**
     * Application context; hanya boleh dibaca setelah `Application.onCreate` mengisinya.
     */
    var ctx: Context
        get() = appContext ?: throw UninitializedPropertyAccessException("lateinit property ctx has not been initialized")
        set(value) {
            val app = (value as? Application) ?: (value.applicationContext as? Application)
            appContext = app ?: value
            if (app != null && !lifecycleRegistered) {
                lifecycleRegistered = true
                app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                    override fun onActivityStarted(activity: Activity) {
                        currentActivityRef = WeakReference(activity)
                    }
                    override fun onActivityResumed(activity: Activity) {
                        currentActivityRef = WeakReference(activity)
                    }
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                    override fun onActivityDestroyed(activity: Activity) {
                        if (currentActivityRef?.get() === activity) {
                            currentActivityRef = null
                        }
                    }
                })
            }
        }

    /**
     * Activity yang sedang aktif dan belum dihancurkan.
     */
    val currentActivity: Activity?
        get() = currentActivityRef?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    /**
     * Context yang sama, tapi `null` kalau graf belum siap (preview, unit test,
     * atau pemanggilan sangat awal) alih-alih melempar `UninitializedPropertyAccessException`.
     *
     * Dipakai fitur yang punya jalan keluar saat context tidak ada — mis. ekspor
     * laporan, yang cukup mengembalikan `false` dan menampilkan pesan ke user.
     */
    fun orNull(): Context? = appContext
}

/**
 * Pintasan [AndroidAppContext.orNull] untuk kode androidMain yang tidak mau
 * mengimpor objeknya langsung.
 */
internal fun androidAppContextOrNull(): Context? = AndroidAppContext.orNull()
