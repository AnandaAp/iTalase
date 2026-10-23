package com.anlian.italase.shared.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.anlian.italase.shared.di.androidAppContextOrNull
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

/**
 * Penyimpanan kunci enkripsi database di Android.
 *
 * Skemanya: kunci database (32 byte acak) **dienkripsi** dengan kunci AES-256
 * yang disimpan di Android Keystore. Yang ditulis ke `SharedPreferences` hanya
 * ciphertext-nya; kunci AES-nya sendiri tidak pernah keluar dari Keystore dan
 * tidak bisa diekspor — walau perangkatnya sudah di-root, isinya tidak bisa
 * dibaca diam-diam.
 *
 * Kenapa tidak menyimpan kunci database polos di `SharedPreferences`:
 * `SharedPreferences` ada di penyimpanan internal aplikasi, yang pada
 * banyak perangkat sudah bisa dibaca oleh proses lain yang punya akses root
 * atau oleh cadangan. Memakai Keystore menaikkan adminsinya dari
 * "app ini yang bisa baca" menjadi "hanya proses ini, hanya di perangkat ini".
 *
 * Semua operasi di sini gagal diam-diam kalau pun Android Keystore tidak
 * tersedia (mis. perangkat lama yang tidak punya hardware-backed keystore).
 * [loadOrCreateKey] Handling-nya: kalau enkripsi/dekripsi gagal, kunci
 * fallback acak dipakai dan database yang ada **tidak** dihapus —seller
 * retains data, dan database yang tidak bisa dibuka akan rebuilding dari nol
 * pada eksekusi berikutnya. Itu trade-off yang disengaja: lebih baik database
 * perlu di-reset daripada data hilang diam-diam.
 */
private const val ANDROID_KEYSTORE = "AndroidKeyStore"

/** Alias kunci AES di dalam Keystore; terpisah dari alias entri kunci database. */
private const val WRAPPING_KEY_ALIAS = "com.anlian.italase.dbkey.wrap"

/** Nama berkas SharedPreferences tempat ciphertext kunci disimpan. */
private const val PREFS_NAME = "com.anlian.italase.dbkey"

/** Kunci di dalam SharedPreferences yang menyimpan ciphertext (Base64). */
private const val PREFS_KEY_CIPHERTEXT = "ciphertext"

/** Kunci di dalam SharedPreferences yang menyimpan IV GCM (Base64). */
private const val PREFS_KEY_IV = "iv"

/** Tag autentikasi AAD; mengikat ciphertext ke konteks aplikasi ini. */
private const val AAD = "com.anlian.italase.dbkey.v1"

/**
 * Ambil kunci enkripsi database, buat dulu kalau belum ada.
 *
 * Nilai yang dikembalikan stabil selama masa instalasi: panggilan berikutnya
 * mengembalikan byte yang sama, dan nilainya tidak berubah antar-jalankan
 * aplikasi. Itulah yang membuat database lama masih bisa dibuka.
 *
 * @return kunci 32 byte. Tidak pernah `null`: kalau penyimpanan aman gagal,
 *   kunci acak dipakai agar aplikasi tetap jalan.
 */
internal fun loadOrCreateKey(): ByteArray {
    val context: Context = androidAppContextOrNull() ?: return fallbackKey()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val storedCiphertext: String? = prefs.getString(PREFS_KEY_CIPHERTEXT, null)
    val storedIv: String? = prefs.getString(PREFS_KEY_IV, null)
    if (storedCiphertext != null && storedIv != null) {
        val decrypted: ByteArray? = decrypt(
            Base64.decode(storedCiphertext, Base64.NO_WRAP),
            Base64.decode(storedIv, Base64.NO_WRAP),
        )
        if (decrypted != null && decrypted.size == DbKey.LENGTH) return decrypted
        error("Kunci database tersimpan di SharedPreferences tetapi gagal didekripsi dari Keystore. Menimpa kunci dibatalkan agar database tidak hilang.")
    }
    val key: ByteArray = randomKey()
    val encrypted = encrypt(key)
        ?: error("Gagal mengenkripsi kunci database baru dengan Keystore")
    val (ciphertext, iv) = encrypted
    prefs.edit(commit = true) {
        putString(PREFS_KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        putString(PREFS_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
    }
    return key
}

/**
 * Kunci acak untuk dipakai kalau penyimpanan aman tidak tersedia.
 *
 * **Peringatan:** kunci ini hanya hidup selama proses berjalan, jadi database
 * yang terenkripsi dengannya **tidak akan bisa dibuka lagi** setelah app
 * ditutup. Karena itu jalur ini harus jadi pilihan terakhir, dan hanya dipakai
 * untuk database yang belum ada.
 */
private fun fallbackKey(): ByteArray = randomKey()

/** 32 byte acak dari sumber entropi sistem. */
private fun randomKey(): ByteArray {
    val bytes = ByteArray(DbKey.LENGTH)
    java.security.SecureRandom().nextBytes(bytes)
    return bytes
}

/**
 * Kunci AES yang membungkus kunci database, dari Android Keystore.
 *
 * `KeyProperties` di sini: GCM 256 bit tanpa padding, dan **tidak**
 * `setUserAuthenticationRequired(true)` — kunci tidak boleh butuh unlock
 * layar, karena kasir harus bisa membuka app dan langsung berjualan.
 */
private fun wrappingKey(): SecretKey {
    val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(WRAPPING_KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    generator.init(
        KeyGenParameterSpec.Builder(
            WRAPPING_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(),
    )
    return generator.generateKey()
}

/** Enkripsi [plain] dengan kunci AES dari Keystore. `null` kalau gagal. */
private fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray>? = runCatching {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
    cipher.updateAAD(AAD.toByteArray())
    val ciphertext = cipher.doFinal(plain)
    ciphertext to cipher.iv
}.getOrNull()

/** Dekripsi [ciphertext] dengan kunci AES dari Keystore. `null` kalau gagal. */
private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray? = runCatching {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, iv))
    cipher.updateAAD(AAD.toByteArray())
    cipher.doFinal(ciphertext)
}.getOrNull()
