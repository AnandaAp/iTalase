package com.anlian.italase.shared.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tes untuk bagian enkripsi database yang bisa diuji tanpa database sungguhan:
 * bentuk kunci, perintah SQL yang dipakai, dan deteksi header SQLite polos.
 *
 * Fungsi-fungsi ini menentukan apakah file database benar-benar terenkripsi,
 * jadi tesnya memeriksa perilakunya, bukan hanya kompilasi.
 */
class DbEncryptionTest {

    @Test
    fun kunci32ByteJadiPassphrase64KarakterHeksadesimal() {
        val key = ByteArray(32) { it.toByte() }
        val passphrase = key.toKeyPassphrase()
        assertEquals(64, passphrase.length)
        // Byte 0..15 = 00..0f, byte 16..31 = 10..1f.
        assertEquals("000102030405060708090a0b0c0d0e0f", passphrase.substring(0, 32))
        assertEquals("101112131415161718191a1b1c1d1e1f", passphrase.substring(32, 64))
    }

    @Test
    fun passphraseHanyaBerisiKarakterAmanUntukSQL() {
        // Semua nilai byte harus jadi heksadesimal, jadi tidak ada karakter
        // yang bisa menutup literal SQL.
        val key = ByteArray(32) { (it * 7 - 128).toByte() }
        val passphrase = key.toKeyPassphrase()
        assertTrue(
            passphrase.all { it in '0'..'9' || it in 'a'..'f' },
            "Passphrase harus hanya berisi heksadesimal, dapat: $passphrase",
        )
    }

    @Test
    fun passphraseMenolakKunciPanjangSalah() {
        assertFailsWith<IllegalArgumentException> { ByteArray(16).toKeyPassphrase() }
        assertFailsWith<IllegalArgumentException> { ByteArray(31).toKeyPassphrase() }
        assertFailsWith<IllegalArgumentException> { ByteArray(33).toKeyPassphrase() }
        assertFailsWith<IllegalArgumentException> { ByteArray(0).toKeyPassphrase() }
    }

    @Test
    fun perintahPragmaKeyMenggunakanNilaiPersis() {
        val passphrase = "00".repeat(32)
        assertEquals("PRAGMA key = '$passphrase'", sqlCipherKeyPragma(passphrase))
    }

    @Test
    fun perintahPragmaKeyTidakBisaDiusulDenganSQLLain() {
        // Kalau kuncinya bisa mengandung tanda kutip, penyerang bisa menutup
        // literal dan menyisipkan perintah lain. Heksadesimal tidak bisa:
        // hanya ada dua tanda kutip, keduanya pembatas literal.
        val passphrase = "ff".repeat(32)
        val sql = sqlCipherKeyPragma(passphrase)
        assertEquals(2, sql.count { it == '\'' })
        assertFalse(sql.contains("';"))
        // Isi literal persis passphrase-nya, tanpa modifikasi.
        assertEquals(passphrase, sql.substringAfter("PRAGMA key = '").substringBeforeLast("'"))
    }

    @Test
    fun headerSqlitePolosTerdeteksi() {
        val header = PLAIN_HEADER.copyOf()
        assertTrue(isPlainSqliteHeader(header))
    }

    @Test
    fun headerTerenkripsiTidakTerdeteksiSebagaiPolos() {
        // Isi berkas terenkripsi adalah acak; header SQLite polos tidak akan
        // pernah muncul di sana.
        val header = ByteArray(16) { (it * 37 + 11).toByte() }
        assertFalse(isPlainSqliteHeader(header))
    }

    @Test
    fun headerPendekTidakTerdeteksi() {
        assertFalse(isPlainSqliteHeader(ByteArray(0)))
        assertFalse(isPlainSqliteHeader(byteArrayOf('S'.code.toByte())))
    }

    @Test
    fun headerYangHampirBenarTidakTerdeteksi() {
        // "SQLite format 3" dengan satu byte berbeda harus gagal terdeteksi —
        // ini yang mencegah file rusak dianggap database polos lalu di-overwrite.
        val header = PLAIN_HEADER.copyOf()
        header[3] = 'X'.code.toByte()
        assertFalse(isPlainSqliteHeader(header))
    }

    private companion object {
        /** 16 byte header SQLite polos: "SQLite format 3" + byte nol. */
        val PLAIN_HEADER: ByteArray = byteArrayOf(
            'S'.code.toByte(), 'Q'.code.toByte(), 'L'.code.toByte(), 'i'.code.toByte(),
            't'.code.toByte(), 'e'.code.toByte(), ' '.code.toByte(), 'f'.code.toByte(),
            'o'.code.toByte(), 'r'.code.toByte(), 'm'.code.toByte(), 'a'.code.toByte(),
            't'.code.toByte(), ' '.code.toByte(), '3'.code.toByte(), 0,
        )
    }
}
