package com.anlian.italase.shared.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementasi Android untuk [ImagePickerLauncher].
 *
 * - Kamera: Menggunakan [ActivityResultContracts.TakePicture] dengan berkas sementara
 *   di direktori cache internal (`cache/qris/`) yang diekspos melalui [FileProvider]
 *   dengan authority `${packageName}.fileprovider`.
 *   Sebelum meluncurkan kamera, memeriksa izin runtime [Manifest.permission.CAMERA]
 *   menggunakan [ContextCompat.checkSelfPermission]. Jika belum diizinkan, meminta izin
 *   melalui [ActivityResultContracts.RequestPermission]. Jika ditolak, memanggil [onPermissionDenied].
 * - Galeri: Menggunakan [ActivityResultContracts.PickVisualMedia] (Android Photo Picker native)
 *   yang mendukung pemilihan citra dengan prinsip privasi least-privilege tanpa memerlukan
 *   izin READ_EXTERNAL_STORAGE atau READ_MEDIA_IMAGES.
 * - Pembacaan I/O: Aliran byte citra dibaca di thread latar belakang ([Dispatchers.IO])
 *   untuk mencegah frame drop / ANR pada foto kamera beresolusi tinggi (10MB - 50MP),
 *   kemudian diserahkan ke [onImageReady] di thread utama ([Dispatchers.Main]).
 */
@Composable
actual fun rememberImagePickerLauncher(
    onImageReady: (ByteArray) -> Unit,
    onPermissionDenied: () -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Simpan string Uri berkas capture kamera agar tahan terhadap activity recreation / memory pressure
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // Launcher untuk mengambil foto kamera (TakePicture)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        val uriString = pendingCameraUriString
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            if (isSuccess) {
                coroutineScope.launch(Dispatchers.IO) {
                    val bytes = readBytesFromUri(context, uri)
                    cleanUpTempCaptureFile(context, uri)
                    if (bytes != null && bytes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onImageReady(bytes)
                        }
                    }
                }
            } else {
                coroutineScope.launch(Dispatchers.IO) {
                    cleanUpTempCaptureFile(context, uri)
                }
            }
        }
        pendingCameraUriString = null
    }

    // Helper untuk membuat Uri sementara dan membuka kamera
    val openCameraAction = remember(context, takePictureLauncher) {
        {
            try {
                val uri = createTempCaptureUri(context)
                pendingCameraUriString = uri.toString()
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Napier.e("Gagal membuat URI atau meluncurkan kamera", e)
                pendingCameraUriString = null
                onPermissionDenied()
            }
        }
    }

    // Launcher untuk meminta izin runtime CAMERA
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCameraAction()
        } else {
            onPermissionDenied()
        }
    }

    // Launcher untuk memilih dari Galeri (PickVisualMedia)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val bytes = readBytesFromUri(context, uri)
                if (bytes != null && bytes.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onImageReady(bytes)
                    }
                }
            }
        }
    }

    return remember(context, permissionLauncher, galleryLauncher, openCameraAction) {
        object : ImagePickerLauncher {
            override fun launchCamera() {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    openCameraAction()
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            override fun launchGallery() {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
}

/**
 * Menghasilkan [Uri] FileProvider untuk berkas temporary penyimpanan tangkapan kamera.
 */
private fun createTempCaptureUri(context: Context): Uri {
    val qrisDir = File(context.cacheDir, "qris").apply { mkdirs() }
    val tempFile = File(qrisDir, "temp_qris_capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

/**
 * Membaca raw byte array dari [Uri] menggunakan [Context.getContentResolver].
 */
private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        Napier.e("Gagal membaca byte citra dari URI: $uri", e)
        null
    }
}

/**
 * Menghapus berkas sementara capture jika masih tersimpan di cache.
 */
private fun cleanUpTempCaptureFile(context: Context, uri: Uri) {
    try {
        val qrisDir = File(context.cacheDir, "qris")
        val fileName = uri.lastPathSegment
        if (fileName != null) {
            val file = File(qrisDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        Napier.w("Pembersihan berkas temporary kamera gagal diabaikan", e)
    }
}
