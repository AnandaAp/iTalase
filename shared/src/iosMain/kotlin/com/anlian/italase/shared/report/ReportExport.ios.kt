package com.anlian.italase.shared.report

import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * Simpan PDF ke direktori sementara iOS lalu tampilkan lembar berbagi.
 *
 * iOS tidak punya "folder Downloads" yang bisa ditulis app tanpa izin user, dan
 * cara yang benar untuk menyimpan/membagikan berkas adalah lembar berbagi sistem
 * ("Simpan ke Files", AirDrop, WhatsApp, email). Berkas ditaruh di
 * `NSTemporaryDirectory()` — iOS yang membersihkannya, jadi app tidak perlu
 * mengelola daur hidup file.
 *
 * `UIActivityViewController` harus dipresentasikan dari view controller yang
 * sedang terlihat; mengambil `keyWindow.rootViewController` adalah cara standar
 * di app berbasis Compose Multiplatform yang tidak punya referensi VC sendiri.
 */
actual suspend fun savePdf(suggestedName: String, bytes: ByteArray): Boolean {
    val dir: NSURL = NSURL.fileURLWithPath(NSTemporaryDirectory())
    val file: NSURL = dir.URLByAppendingPathComponent(suggestedName) ?: return false
    val data: NSData = bytes.toNSData()
    if (!data.writeToURL(file, atomically = true)) return false

    val root: UIViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: return false
    val controller =
        UIActivityViewController(activityItems = listOf(file), applicationActivities = null)
    // iPad: tanpa popover, presentasi melempar exception.
    controller.popoverPresentationController?.sourceView = root.view
    root.presentViewController(controller, animated = true, completion = null)
    return true
}

/**
 * Salin `ByteArray` Kotlin ke `NSData`.
 *
 * `usePinned` wajib: byte array Kotlin tidak dijamin tinggal di alamat yang sama
 * saat `memcpy` berjalan, dan tanpa pinning hasilnya bisa rusak tanpa error.
 */
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}
