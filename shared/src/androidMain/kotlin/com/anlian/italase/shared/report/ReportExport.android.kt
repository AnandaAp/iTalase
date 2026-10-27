package com.anlian.italase.shared.report

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.anlian.italase.shared.di.androidAppContextOrNull
import com.anlian.italase.shared.di.currentActivity
import java.io.File

/**
 * Simpan PDF lalu tawarkan ke aplikasi lain lewat `ACTION_SEND`.
 *
 * Ditulis ke cache internal dulu, bukan langsung ke folder Downloads: menulis ke
 * Downloads butuh izin/scoped-storage per varian Android, sedangkan berkas di cache
 * bisa dibagikan tanpa izin apa pun dan user tetap memilih "Simpan ke Files" /
 * WhatsApp / email dari lembar berbagi.
 *
 * `FileProvider` dipakai karena `file://` URI ditolak sejak Android 7 — URInya
 * harus `content://` dengan authority `${packageName}.fileprovider`, yang sudah
 * didaftarkan di manifest.
 *
 * Mengembalikan `true` kalau lembar berbagi berhasil dibuka. Tidak menunggu user
 * memilih tujuan: hasil akhirnya di luar kendali app.
 */
actual suspend fun savePdf(suggestedName: String, bytes: ByteArray): Boolean {
    val context: Context = androidAppContextOrNull()
        ?: return false
    val dir: File = File(context.cacheDir, "reports").apply { mkdirs() }
    // Bersihkan berkas laporan sementara lama (> 24 jam) agar penyimpanan cache tidak menumpuk
    dir.listFiles()?.forEach { oldFile ->
        if (System.currentTimeMillis() - oldFile.lastModified() > 24 * 60 * 60 * 1000L) {
            oldFile.delete()
        }
    }
    // Sanitasi nama berkas untuk mencegah path traversal (mis. '../')
    val safeName = File(suggestedName).name.ifBlank { "laporan.pdf" }
    val file: File = File(dir, safeName).apply { writeBytes(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, safeName)
        clipData = android.content.ClipData.newRawUri("report", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Context aplikasi tidak boleh startActivity; butuh Activity yang sedang terlihat.
    val activity: Activity? = currentActivity()
    val chooser = Intent.createChooser(send, "Bagikan laporan").apply {
        if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    (activity ?: context).startActivity(chooser)
    return true
}
