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

    /**
     * Context yang sama, tapi `null` kalau graf belum siap (preview, unit test,
     * atau pemanggilan sangat awal) alih-alih melempar `UninitializedPropertyAccessException`.
     *
     * Dipakai fitur yang punya jalan keluar saat context tidak ada — mis. ekspor
     * laporan, yang cukup mengembalikan `false` dan menampilkan pesan ke user.
     */
    fun orNull(): Context? = if (::ctx.isInitialized) ctx else null
}

/**
 * Pintasan [AndroidAppContext.orNull] untuk kode androidMain yang tidak mau
 * mengimpor objeknya langsung.
 */
internal fun androidAppContextOrNull(): Context? = AndroidAppContext.orNull()
