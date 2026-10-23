package com.anlian.italase.shared.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.NativeSQLiteDriver
import androidx.sqlite.execSQL

/**
 * Driver terenkripsi untuk iOS.
 *
 * **Cara kerjanya:** pod `SQLCipher` berisi SQLite fork yang mengekspor simbol
 * C `sqlite3_*` yang sama dengan SQLite bawaan iOS. Karena pod itu yang
 * di-link, simbol `sqlite3_*` yang dipanggil driver AndroidX tadi sebenarnya
 * memanggil implementasi **SQLCipher**. Jadi driver ini tidak perlu cinterop
 * sendiri — cukup memasang kuncinya.
 *
 * Kenapa bukan cinterop SQLCipher sendiri: cinterop itu akan mendefinisikan
 * `cnames.structs.sqlite3` — nama yang sama dengan milik cinterop
 * `androidx.sqlite:sqlite-framework` (yang ikut taruh `room-runtime`) — dan
 * dua definisi struct dengan nama sama di satu modul compile bentrok
 * ("conflicting dependencies"). Dengan cara ini tidak ada cinterop baru sama
 * sekali, jadi tidak ada bentrok.
 *
 * Kunci dipasang **tepat setelah koneksi dibuka** dan sebelum Room
 * membaca apa pun. Urutannya penting: Room membaca `PRAGMA user_version`
 * sebelum statement aplikasi pertama, jadi kunci yang dipasang belakangan
 * akan terlambat dan Room akan menyimpulkan file itu bukan database.
 *
 * Setiap `open()` memasang ulang kuncinya, jadi driver aman dipakai bersama
 * connection pool.
 */
class SqlCipherDriver(private val passphrase: String) : SQLiteDriver {

    /** Driver bawaan AndroidX; ia memanggil simbol `sqlite3_*` milik SQLCipher. */
    private val delegate: NativeSQLiteDriver = NativeSQLiteDriver()

    override val hasConnectionPool: Boolean get() = delegate.hasConnectionPool

    override fun open(fileName: String): SQLiteConnection {
        val connection: SQLiteConnection = delegate.open(fileName)
        try {
            // Kunci dipasang di sini — sebelum statement pertama pada koneksi.
            connection.execSQL(sqlCipherKeyPragma(passphrase))
            // **Pemeriksaan inilah yang membuat "terenkripsi" jadi fakta, bukan
            // klaim.** Kalau simbol `sqlite3_*` sampai resolve ke SQLite bawaan
            // iOS (mis. karena urutan link salah), `PRAGMA key` akan diabaikan
            // TANPA error dan database tetap polos. `PRAGMA cipher_version`
            // hanya ada di build SQLCipher, jadi kosong = build ini bukan
            // SQLCipher. Gagal keras di sini jauh lebih baik daripada
            // diam-diam menyimpan data seller tanpa enkripsi.
            val version: String = connection.cipherVersion()
            if (version.isBlank()) {
                throw IllegalStateException(
                    "Koneksi ini bukan SQLCipher (PRAGMA cipher_version kosong). " +
                        "Pastikan pod SQLCipher ter-link dan SQLite bawaan iOS tidak dipakai.",
                )
            }
        } catch (t: Throwable) {
            connection.close()
            throw t
        }
        return connection
    }
}

/** Driver terenkripsi untuk iOS — implementasi dari `commonMain`. */
actual fun createEncryptedDriver(passphrase: String): SQLiteDriver =
    SqlCipherDriver(passphrase)
