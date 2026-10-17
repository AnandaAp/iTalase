package com.anlian.italase.shared.di

import android.content.Context

/**
 * Jembatan satu arah dari layer Android ke Koin.
 *
 * Koin-annotation tidak bisa menyuntikkan `Context`, jadi nilainya diisi di
 * `Application.onCreate` sebelum graph dibangun. `lateinit` dipakai supaya pembacaan terlalu
 * dini gagal keras saat start, bukan menyerahkan Context kosong yang errornya baru muncul
 * jauh di dalam Room.
 */
object AndroidAppContext {
    /**
     * Application context; hanya boleh dibaca setelah `Application.onCreate` mengisinya.
     */
    lateinit var ctx: Context
}
