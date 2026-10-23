package com.anlian.italase.shared.di

import androidx.room.RoomDatabase
import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.createEncryptedDriver
import com.anlian.italase.shared.data.local.getDatabaseBuilder
import com.anlian.italase.shared.data.local.getRoomDatabase
import com.anlian.italase.shared.data.local.loadOrCreateKey
import com.anlian.italase.shared.data.local.toKeyPassphrase
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

/**
 * Bean platform iOS.
 *
 * Lokasi berkas dan pembukaan database dipisah ke dua bean supaya urutan penyiapan (berkas
 * dulu, driver & migrasi kemudian) terbaca langsung dari deklarasi, dan Koin memegang keduanya
 * sendiri — tidak ada dua koneksi ke berkas yang sama.
 */
@Module
actual class PlatformModule: KoinComponent {
    /**
     * Builder untuk berkas database di Documents; dibuat sekali per proses.
     */
    @Singleton
    fun provideDbBuilder(): RoomDatabase.Builder<ITalaseDatabase> = getDatabaseBuilder()

    /**
     * Buka database dari builder tersebut lewat `getRoomDatabase`, tempat driver dan migrasi
     * dipasang.
     *
     * Database ini **terenkripsi**: kuncinya disimpan di Keychain
     * (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) dan dipasang ke
     * koneksi lewat driver SQLCipher sebelum Room membaca apa pun.
     */
    @Singleton
    fun provideDb(builder: RoomDatabase.Builder<ITalaseDatabase>): ITalaseDatabase {
        val passphrase: String = loadOrCreateKey().toKeyPassphrase()
        return getRoomDatabase(builder, createEncryptedDriver(passphrase))
    }
}
