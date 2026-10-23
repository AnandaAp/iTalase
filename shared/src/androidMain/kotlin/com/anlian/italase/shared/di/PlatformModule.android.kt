package com.anlian.italase.shared.di

import com.anlian.italase.shared.data.local.DbFiles
import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.createEncryptedDriver
import com.anlian.italase.shared.data.local.getDatabaseBuilder
import com.anlian.italase.shared.data.local.getRoomDatabase
import com.anlian.italase.shared.data.local.loadOrCreateKey
import com.anlian.italase.shared.data.local.migratePlainDatabaseIfNeeded
import com.anlian.italase.shared.data.local.toKeyPassphrase
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
        val passphrase: String = loadOrCreateKey().toKeyPassphrase()
        val builder = getDatabaseBuilder(context)
        // Database versi lama masih polos SQLite. Migrasi dijalankan SEBELUM
        // Room membuka database, supaya Room bertemu skema yang sudah terenkripsi
        // dan versi skemanya sudah benar.
        migratePlainDatabaseIfNeeded(context.getDatabasePath(DbFiles.NAME), passphrase)
        return getRoomDatabase(builder, createEncryptedDriver(passphrase))
    }
}
