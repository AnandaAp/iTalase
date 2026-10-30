package com.anlian.italase.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS Quarantine Stub untuk [ImagePickerLauncher].
 *
 * Implementasi iOS native (PHPickerViewController / UIImagePickerController)
 * berada dalam status hardware quarantine sesuai aturan AGENTS.md karena lingkungan
 * pengembangan saat ini berbasis Windows tanpa Xcode/macOS hardware.
 *
 * Stub ini disediakan agar kompilasi KMP expect/actual di target iOS tetap
 * valid dan tidak merusak build bersama.
 */
@Composable
actual fun rememberImagePickerLauncher(
    onImageReady: (ByteArray) -> Unit,
    onPermissionDenied: () -> Unit,
): ImagePickerLauncher = remember {
    object : ImagePickerLauncher {
        override fun launchCamera() {
            // iOS Hardware Quarantine: Operasi kamera iOS dikarantina
        }

        override fun launchGallery() {
            // iOS Hardware Quarantine: Operasi galeri iOS dikarantina
        }
    }
}
