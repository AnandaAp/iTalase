package com.anlian.italase.shared.data.local

/**
 * Kunci enkripsi database — bagian bersama (common) yang tidak tahu-menahu
 * platform mana yang menyimpan kuncinya.
 *
 * Isi file ini sengaja **tidak** melakukan I/O. Semua operasi baca/tulis kunci
 * ada di `DbKey.android.kt` dan `DbKey.ios.kt`; yang di sini cuma bentuk
 * nilainya supaya kedua platform memakai representasi yang sama persis.
 *
 * Kenapa kunci dibuat acak 32 byte, bukan dari sandi user:
 *
 * 1. **Tidak ada sandi yang bisa dicatat.** Aplikasi ini tidak punya layar
 *    "masukkan sandi", dan menambahkannya berarti seller harus mengingat
 *    password tambahan hanya untuk membuka app-nya.
 * 2. **32 byte acak = 256 bit entropi.** Jadi passphrase yang lemah mustahil
 *    terjadi, dan KDF (PBKDF2-HMAC-SHA512, 256.000 iterasi) tidak menambah
 *    keamanan yang berarti di atas kunci yang sudah acak.
 * 3. **Tidak perlu hashing yang bisa dibalik.** Kunci disimpan apa adanya di
 *    keystore yang sudah mengunci aksesnya ke proses app ini.
 */
object DbKey {
    /**
     * Panjang kunci dalam byte: 32 byte = 256 bit, ukuran kunci AES-256.
     */
    const val LENGTH: Int = 32

    /**
     * Nama entri tempat kunci disimpan di Keychain/Keystore.
     *
     * Prefixed dengan nama app supaya tidak bentrok dengan entri lain di
     * keystore perangkat yang dipakai bersama.
     */
    const val KEYCHAIN_ALIAS: String = "com.anlian.italase.dbkey"
}

/**
 * Kunci enkripsi dalam bentuk 64 karakter heksadesimal — bentuk yang dipakai
 * SQLCipher sebagai **passphrase**.
 *
 * Kenapa hex dan bukan byte mentah:
 *
 * - **Konsisten dengan Android.** Driver SQLCipher Android hanya menerima
 *   frasa sandi (`sqlite3_key` = passphrase API, KDF diterapkan). Untuk iOS
 *   supaya bisa memakai bentuk yang sama, kuncinya diratakan menjadi teks
 *   hex yang bisa di-*encode* jadi UTF-8 di kedua platform. Database yang sama
 *   lalu bisa dibuka di Android maupun iOS.
 * - **Aman disisipkan ke SQL tanpa escaping.** Teks heksadesimal hanya punya
 *   karakter `0-9a-f`, jadi tidak mungkin memuat tanda kutip yang bisa
 *   menutup literal SQL. `PRAGMA key = '…'` jadi selalu aman.
 *
 * Raw key tanpa KDF (`PRAGMA key = "x'…'"`) memang sedikit lebih cepat, tapi
 * bentuk itu tidak bisa dipakai lewat driver Android di sini, jadi memilih
 * bentuk yang bisa dipakai kedua platform jauh lebih berharga daripada
 * menghemat satu putaran PBKDF2.
 *
 * @return 64 karakter heksadesimal (32 byte).
 */
fun ByteArray.toKeyPassphrase(): String {
    require(size == DbKey.LENGTH) {
        "Kunci SQLCipher harus tepat ${DbKey.LENGTH} byte, dapat $size"
    }
    val hex = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        hex.append(HEX_DIGITS[v shr 4])
        hex.append(HEX_DIGITS[v and 0x0F])
    }
    return hex.toString()
}

/** Tabel heksadesimal; konstanta supaya tidak dialokasikan per pemanggilan. */
private const val HEX_DIGITS = "0123456789abcdef"

/**
 * Bangun perintah SQL yang memasang kunci pada koneksi SQLCipher.
 *
 * Dipakai lewat `execSQL` (bukan `prepare`) karena `PRAGMA key` tidak menerima
 * parameter binding — nilainya harus masuk ke dalam teks SQL.
 *
 * Kunci diapit tanda kutip tunggal. Karena [toKeyPassphrase] hanya menghasilkan
 * karakter heksadesimal, tidak ada tanda kutip di dalamnya, jadi tidak ada
 * yang bisa "keluar" dari literal SQL.
 *
 * @param passphrase kunci 64 karakter heksadesimal, hasil [toKeyPassphrase].
 */
fun ByteArray.toRawKeyBlob(): String = "x'${toKeyPassphrase()}'"

fun sqlCipherKeyPragma(passphrase: String): String =
    "PRAGMA key = \"x'$passphrase'\""
