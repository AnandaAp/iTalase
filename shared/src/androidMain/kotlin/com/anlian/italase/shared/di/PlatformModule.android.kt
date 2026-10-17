package com.anlian.italase.shared.di

import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
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
     */
    @Singleton
    fun provideDb(): ITalaseDatabase {
        val context = AndroidAppContext.ctx
        val builder = getDatabaseBuilder(context, ByteArray(0))
        return builder
            .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                com.anlian.italase.shared.data.local.MIGRATION_1_2,
                com.anlian.italase.shared.data.local.MIGRATION_2_3
            )
            .build()
    }
}
