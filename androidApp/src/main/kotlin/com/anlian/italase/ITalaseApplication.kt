package com.anlian.italase

import android.app.Application
import com.anlian.italase.shared.di.AndroidAppContext
import com.anlian.italase.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * `Application` Android — titik start Koin di sisi Android.
 *
 * Urutannya penting: [AndroidAppContext] diisi lebih dulu karena beberapa
 * provider platform (pembuat Room, pembaca file) memintanya saat graf disusun,
 * bukan saat dipakai.
 */
class ITalaseApplication: Application() {
    /** Mengisi context platform, lalu menyalakan Koin dengan [androidContext]. */
    override fun onCreate() {
        super.onCreate()

        AndroidAppContext.ctx = this
        initKoin {
            androidContext(this@ITalaseApplication)
        }
    }
}