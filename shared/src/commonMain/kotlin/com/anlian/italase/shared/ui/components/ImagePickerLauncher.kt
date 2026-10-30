package com.anlian.italase.shared.ui.components

import androidx.compose.runtime.Composable

/**
 * Kontrak controller untuk memicu pengambilan gambar dari kamera atau galeri.
 */
interface ImagePickerLauncher {
    /**
     * Membuka kamera perangkat untuk mengambil foto QRIS.
     * Memeriksa dan meminta izin runtime CAMERA:
     * - Jika diizinkan -> kamera dibuka via TakePicture
     * - Jika ditolak -> onPermissionDenied() dipanggil
     */
    fun launchCamera()

    /**
     * Membuka native Photo Picker sistem (PickVisualMedia) untuk memilih gambar dari galeri.
     * Tidak memerlukan izin storage runtime khusus.
     */
    fun launchGallery()
}

/**
 * Composable launcher untuk memilih gambar dari kamera atau galeri.
 *
 * @param onImageReady Callback saat citra berhasil diambil/dipilih, berupa raw ByteArray.
 * @param onPermissionDenied Callback jika izin kamera ditolak oleh pengguna.
 */
@Composable
expect fun rememberImagePickerLauncher(
    onImageReady: (ByteArray) -> Unit,
    onPermissionDenied: () -> Unit,
): ImagePickerLauncher
