package com.anlian.italase.shared.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Builder Room untuk iOS.
 *
 * Berkas database diletakkan di direktori Documents supaya tidak terhapus
 * saat app diperbarui — iOS tidak punya padanan `applicationContext`.
 *
 * Database ini **terenkripsi**: driver-nya memasang kunci SQLCipher sebelum
 * Room membaca apa pun (lihat `SqlCipherDriver.ios.kt`). Driver dan migrasi
 * diatur oleh `PlatformModule`.
 */
fun getDatabaseBuilder(): RoomDatabase.Builder<ITalaseDatabase> {
    val dbPath: String = documentDirectory() + "/${DbFiles.NAME}"
    return Room.databaseBuilder<ITalaseDatabase>(name = dbPath)
}

/**
 * Direktori Documents milik app.
 *
 * Gagal keras kalau tidak ada: tanpa direktori itu database tidak punya tempat tinggal, dan
 * errornya jauh lebih jelas di titik ini daripada di dalam Room.
 */
@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { MissingDocumentDir.MESSAGE }
}

/**
 * Satu tempat untuk pesan kegagalan [documentDirectory]; konstanta terpisah supaya teksnya
 * mudah dicari di log maupun crash report.
 */
private object MissingDocumentDir {
    /**
     * Pesan yang dilempar `requireNotNull`; dipakai sebagai konstanta agar sama persis di setiap
     * pemanggilan.
     */
    const val MESSAGE = "Document directory not found"
}
