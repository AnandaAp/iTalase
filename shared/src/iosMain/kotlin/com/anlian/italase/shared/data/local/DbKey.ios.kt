package com.anlian.italase.shared.data.local

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.Security.kSecRandomDefault

/**
 * Penyimpanan kunci enkripsi database di iOS.
 *
 * Kuncinya disimpan di **Keychain**, bukan di `UserDefaults` atau berkas biasa.
 * Alasannya sama dengan Android: `UserDefaults` ada di direktori app yang ikut
 * ter-backup ke iCloud dan bisa dibaca oleh proses lain di perangkat yang sudah
 * di-jailbreak. Entri Keychain dengan
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` hanya bisa dibaca app ini,
 * di perangkat ini saja, dan tidak ikut ter-backup.
 *
 * `AfterFirstUnlock` dipilih, bukan `WhenUnlocked`: kios kasir harus bisa
 * menyalakan app dan langsung berjualan, termasuk saat perangkat baru dinyalakan
 * tanpa sempat dibuka layarnya.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun loadOrCreateKey(): ByteArray {
    val existing: ByteArray? = readFromKeychain()
    if (existing != null && existing.size == DbKey.LENGTH) return existing
    val key: ByteArray = randomKey()
    writeToKeychain(key)
    return key
}

/**
 * Baca kunci dari Keychain; `null` kalau belum ada atau tidak bisa dibaca.
 *
 * Query dibangun sebagai `CFMutableDictionaryRef` (bukan `Map` Kotlin): API
 * Keychain menerima `CFDictionaryRef`, dan `Map` Kotlin bukan dictionary Core
 * Foundation — tidak bisa di-cast.
 */
@OptIn(ExperimentalForeignApi::class)
private fun readFromKeychain(): ByteArray? = memScoped {
    val query = buildKeychainQuery()
    try {
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status: Int = SecItemCopyMatching(query.reinterpret(), result.ptr)
        if (status != errSecSuccess) return null
        val value = result.value ?: return null
        try {
            // Hanya `kSecReturnData` yang diminta, jadi hasilnya `CFDataRef`.
            val data = value.reinterpret<platform.CoreFoundation.CFDataRef>()
            val count = CFDataGetLength(data).toInt()
            val bytes = CFDataGetBytePtr(data) ?: return null
            return bytes.readBytes(count)
        } finally {
            // `SecItemCopyMatching` mengikuti aturan Core Foundation: pemanggil
            // yang harus melepas hasilnya.
            CFRelease(value)
        }
    } finally {
        CFRelease(query)
    }
}

/** Simpan kunci ke Keychain, menimpa entri lama kalau ada. */
@OptIn(ExperimentalForeignApi::class)
private fun writeToKeychain(key: ByteArray) {
    // Hapus dulu supaya kunci lama (mis. dari versi sebelumnya) tidak
    // menyisakan dua entri dan pembacaannya jadi ambigu.
    val identity = buildKeychainQuery()
    try {
        SecItemDelete(identity.reinterpret())
    } finally {
        CFRelease(identity)
    }

    val query = buildKeychainQuery()
    try {
        val value = key.toNSData()
        CFDictionarySetValue(query, kSecValueData, value)
        CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        val status: Int = SecItemAdd(query.reinterpret(), null)
        if (status != errSecSuccess) {
            error("Gagal menyimpan kunci ke Keychain: status $status")
        }
    } finally {
        CFRelease(query)
    }
}

/**
 * Query Keychain dasar: kelas item, service, dan account.
 *
 * `CFRelease` pada nilai yang disalin dictionary: dictionary menahan
 * reference-nya sendiri, jadi string yang dibuat di sini bisa dilepas setelah
 * masuk.
 */
@OptIn(ExperimentalForeignApi::class)
private fun buildKeychainQuery(): platform.CoreFoundation.CFMutableDictionaryRef {
    val query = CFDictionaryCreateMutable(
        null,
        0,
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr,
    ) ?: error("Gagal membuat dictionary Keychain")
    val service = KEYCHAIN_SERVICE.toCFString()
    val account = KEYCHAIN_ALIAS.toCFString()
    try {
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, service)
        CFDictionarySetValue(query, kSecAttrAccount, account)
    } finally {
        CFRelease(service)
        CFRelease(account)
    }
    return query
}

/** `String` Kotlin jadi `CFStringRef` UTF-8. */
@OptIn(ExperimentalForeignApi::class)
private fun String.toCFString(): platform.CoreFoundation.CFStringRef =
    CFStringCreateWithCString(null, cstr, kCFStringEncodingUTF8)
        ?: error("Gagal membuat CFString")

/** 32 byte acak dari generator acak sistem (SecRandomCopyBytes). */
@OptIn(ExperimentalForeignApi::class)
private fun randomKey(): ByteArray = memScoped {
    val bytes = allocArray<ByteVar>(DbKey.LENGTH)
    val status: Int = SecRandomCopyBytes(kSecRandomDefault, DbKey.LENGTH.toULong(), bytes)
    if (status != errSecSuccess) {
        // Gagal generate acak berarti perangkat dalam kondisi aneh; lebih baik
        // gagal keras daripada memakai kunci yang bisa ditebak.
        error("Gagal membuat kunci acak: status $status")
    }
    ByteArray(DbKey.LENGTH) { bytes[it] }
}

/** Alias entri di Keychain; sama dengan [DbKey.KEYCHAIN_ALIAS]. */
private const val KEYCHAIN_ALIAS = DbKey.KEYCHAIN_ALIAS

/** Service Keychain; memisahkan entri iTalase dari entri app lain. */
private const val KEYCHAIN_SERVICE = "com.anlian.italase"

/** Bungkus `ByteArray` jadi `NSData` untuk disimpan di Keychain. */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}
