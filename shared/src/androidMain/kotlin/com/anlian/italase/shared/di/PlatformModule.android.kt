package com.anlian.italase.shared.di

import com.anlian.italase.shared.data.local.DbFiles
import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.createEncryptedDriver
import com.anlian.italase.shared.data.local.getDatabaseBuilder
import com.anlian.italase.shared.data.local.getRoomDatabase
import com.anlian.italase.shared.data.local.loadOrCreateKey
import com.anlian.italase.shared.data.local.migratePlainDatabaseIfNeeded
import com.anlian.italase.shared.data.local.migrateToRawKeyIfNeeded
import com.anlian.italase.shared.data.local.toKeyPassphrase
import com.anlian.italase.shared.data.local.toRawKeyBlob
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

/**
 * Bean platform Android — satu-satunya tempat `ITalaseDatabase` dibangun di target ini.
 *
 * Migrasi didaftarkan eksplisit, bukan `fallbackToDestructiveMigration`: database ini adalah
 * satu-satunya salinan riwayat penjualan, jadi jatuh ke drop-table berarti kehilangan data
 * penjual.
 */
@Module
actual class PlatformModule: KoinComponent {
    /**
     * Bangun database dari berkas di penyimpanan aplikasi; satu instance per proses.
     *
     * Coroutine query dialihkan ke `Dispatchers.IO` supaya I/O SQLite tidak ikut mengunci
     * dispatcher Compose saat daftar panjang dimuat.
     *
     * Database ini **terenkripsi**: driver-nya SQLCipher dan kuncinya disimpan di
     * Android Keystore (lihat `DbKey.android.kt`). Kunci dibaca sekali di sini lalu
     * dipakai untuk membuka database dan, kalau perlu, meng-enkripsi database polos
     * milik versi app sebelumnya.
     */
    @Singleton
    fun provideDb(): ITalaseDatabase {
        val context = AndroidAppContext.ctx
        val keyBytes = loadOrCreateKey()
        val passphrase: String = keyBytes.toKeyPassphrase()
        val rawKeyBlob: String = keyBytes.toRawKeyBlob()
        val dbFile = context.getDatabasePath(DbFiles.NAME)
        val builder = getDatabaseBuilder(context)
        // Database versi lama masih polos SQLite atau memakai PBKDF2 passphrase.
        // Migrasi dijalankan SEBELUM Room membuka database, sehingga Room bertemu
        // database terenkripsi raw key yang terbuka seketika tanpa overhead PBKDF2.
        migratePlainDatabaseIfNeeded(dbFile, rawKeyBlob)
        migrateToRawKeyIfNeeded(dbFile, passphrase, rawKeyBlob)
        return getRoomDatabase(builder, createEncryptedDriver(rawKeyBlob))
    }
}
