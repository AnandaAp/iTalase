package com.anlian.italase.shared.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Builder Room untuk Android, menunjuk berkas milik aplikasi.
 *
 * [passphrase] belum dipakai: SQLCipher membutuhkan `SQLiteDriver` khusus yang belum
 * dipasang, jadi database saat ini SQLite biasa (lihat komentar di bawah). Driver dan konteks
 * coroutine juga tidak diatur di sini — pemanggilnya (`PlatformModule`) yang melengkapi,
 * supaya berkas ini hanya tahu soal lokasi berkas.
 *
 * Context diambil dari `applicationContext` agar builder tidak menahan Activity.
 */
fun getDatabaseBuilder(context: Context, passphrase: ByteArray): RoomDatabase.Builder<ITalaseDatabase> {
    val appContext: Context = context.applicationContext
    val dbFile = appContext.getDatabasePath(DbFiles.NAME)
    // Room 2.8+ uses driver APIs; SQLCipher requires custom SQLiteDriver (not implemented).
    // Fallback: plain SQLite with backup protection only.
    return Room.databaseBuilder<ITalaseDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
