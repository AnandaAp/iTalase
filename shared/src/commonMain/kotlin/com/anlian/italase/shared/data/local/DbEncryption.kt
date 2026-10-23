package com.anlian.italase.shared.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Deteksi apakah berkas database masih SQLite polos.
 *
 * Header berkas SQLite polos selalu diawali 16 byte persis:
 * `"SQLite format 3"`. Berkas yang sudah terenkripsi SQLCipher **tidak**
 * punya header itu — isinya acak — jadi satu pembacaan 16 byte sudah cukup
 * untuk membedakan keduanya tanpa membuka database.
 *
 * Ini dipakai untuk decides apakah database lama perlu dimigrasikan ke
 * bentuk terenkripsi, dan sekaligus untuk memastikan file yang dihasilkan
 * benar-benar terenkripsi (bukan diam-diam masih polos).
 *
 * @param header 16 byte pertama berkas database.
 * @return true kalau header itu milik SQLite polos.
 */
fun isPlainSqliteHeader(header: ByteArray): Boolean {
    if (header.size < PLAIN_SQLITE_HEADER.size) return false
    for (i in PLAIN_SQLITE_HEADER.indices) {
        if (header[i] != PLAIN_SQLITE_HEADER[i]) return false
    }
    return true
}

/** Header SQLite polos: 16 byte pertama setiap berkas database SQLite. */
private val PLAIN_SQLITE_HEADER: ByteArray = byteArrayOf(
    'S'.code.toByte(), 'Q'.code.toByte(), 'L'.code.toByte(), 'i'.code.toByte(),
    't'.code.toByte(), 'e'.code.toByte(), ' '.code.toByte(), 'f'.code.toByte(),
    'o'.code.toByte(), 'r'.code.toByte(), 'm'.code.toByte(), 'a'.code.toByte(),
    't'.code.toByte(), ' '.code.toByte(), '3'.code.toByte(), 0,
)

/**
 * Baca `PRAGMA user_version` — nomor versi skema yang dipakai Room.
 *
 * Room menyimpan versi skema di `PRAGMA user_version`, bukan di tabel biasa.
 * Nilai ini wajib ikut tersalin saat database polos dimigrasikan ke bentuk
 * terenkripsi, karena `sqlcipher_export` **tidak** menyalinnya — kalau
 * tertinggal di 0, Room akan menganggap skema perlu dimigrasi dari nol dan
 * gagal.
 *
 * @return nomor versi skema; 0 kalau belum ada database.
 */
fun SQLiteConnection.readUserVersion(): Int {
    val statement = prepare("PRAGMA user_version")
    statement.use {
        if (!it.step()) return 0
        return it.getInt(0)
    }
}

/**
 * Salin `user_version` ke database lain yang sedang ter-attach.
 *
 * Dipakai setelah `sqlcipher_export`: exporting menyalin skema dan data, tapi
 * tidak menyalin versi skema.
 *
 * @param schemaName nama database yang di-attach (tanpa awalan `PRAGMA`).
 */
fun SQLiteConnection.copyUserVersionTo(schemaName: String) {
    val version = readUserVersion()
    execSQL("PRAGMA $schemaName.user_version = $version")
}

/**
 * Nyatakan apakah koneksi ini benar-benar talking ke SQLCipher.
 *
 * `PRAGMA cipher_version` mengembalikan nomor versi SQLCipher kalau build-nya
 * punya codec. Di SQLite biasa, pragma ini **tidak ada** dan hasilnya kosong —
 * artinya file itu tidak terenkripsi.
 *
 * Pemeriksaan ini penting karena driver SQLite yang salah tautan (mis. resolved
 * ke `libsqlite3` bawaan sistem, bukan SQLCipher) tidak akan error: `PRAGMA
 * key` di sana diabaikan diam-diam, dan database tetap polos. Tanpa pemeriksaan
 * ini, "terenkripsi" hanya klaim.
 *
 * @return nomor versi SQLCipher, atau string kosong kalau build ini bukan
 *   SQLCipher.
 */
fun SQLiteConnection.cipherVersion(): String {
    val statement = prepare("PRAGMA cipher_version")
    statement.use {
        if (!it.step()) return ""
        // `PRAGMA cipher_version` mengembalikan satu kolom bernama
        // "cipher_version"; pada SQLite biasa kolom itu tidak ada sama sekali,
        // jadi `step()` sudah lebih dulu mengembalikan false di atas.
        return it.getText(0)
    }
}
