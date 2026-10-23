package com.anlian.italase.shared.report

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

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
 * sedang terlihat; mengambil `rootViewController` dari scene jendela yang aktif
 * (menggantikan `keyWindow` yang deprecated) lalu menelusuri `presentedViewController`
 * memastikan lembar berbagi tidak gagal dipresentasikan saat ada dialog/sheet lain di layar.
 */
actual suspend fun savePdf(suggestedName: String, bytes: ByteArray): Boolean {
    val dir: NSURL = NSURL.fileURLWithPath(NSTemporaryDirectory())
    val file: NSURL = dir.URLByAppendingPathComponent(suggestedName) ?: return false
    val data: NSData = bytes.toNSData()
    if (!data.writeToURL(file, atomically = true)) return false

    val presenter: UIViewController = findTopViewController()
        ?: return false
    val controller =
        UIActivityViewController(activityItems = listOf(file), applicationActivities = null)
    // iPad: tanpa popover, presentasi melempar exception.
    controller.popoverPresentationController?.sourceView = presenter.view
    presenter.presentViewController(controller, animated = true, completion = null)
    return true
}

private fun findTopViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    val activeScene = scenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: scenes.filterIsInstance<UIWindowScene>().firstOrNull()

    val window = activeScene?.windows
        ?.filterIsInstance<UIWindow>()
        ?.firstOrNull { it.isKeyWindow }
        ?: activeScene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull()
        ?: @Suppress("DEPRECATION") UIApplication.sharedApplication.keyWindow

    val root = window?.rootViewController ?: return null
    return root.topVisibleViewController()
}

/** Telusuri rantai modal sampai ke controller teratas yang sedang terlihat. */
private tailrec fun UIViewController.topVisibleViewController(): UIViewController {
    val presented = presentedViewController ?: return this
    return presented.topVisibleViewController()
}

/**
 * Salin `ByteArray` Kotlin ke `NSData`.
 *
 * `usePinned` wajib: byte array Kotlin tidak dijamin tinggal di alamat yang sama
 * saat `memcpy` berjalan, dan tanpa pinning hasilnya bisa rusak tanpa error.
 */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}
