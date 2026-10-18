package com.anlian.italase.shared.ui.components

import italase.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Logo aplikasi sebagai byte JPEG, untuk disisipkan ke laporan PDF.
 *
 * Logo disimpan sebagai JPEG di `composeResources/files/` (bukan drawable XML):
 * penulis PDF menyisipkan JPEG apa adanya lewat filter `DCTDecode`, jadi tidak
 * perlu decode/re-encode gambar di runtime.
 *
 * `suspend` karena pembacaan resource bisa menyentuh I/O; dipanggil dari
 * `viewModelScope`, bukan dari komposisi.
 */
@OptIn(ExperimentalResourceApi::class)
suspend fun readAppLogo(): ByteArray = Res.readBytes("files/italase_logo.png")
