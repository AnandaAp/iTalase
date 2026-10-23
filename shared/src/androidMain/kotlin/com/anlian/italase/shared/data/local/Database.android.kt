package com.anlian.italase.shared.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import net.zetetic.database.DatabaseErrorHandler
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import java.io.File

/**
 * Builder Room untuk Android, menunjuk berkas milik aplikasi.
 *
 * Bedanya dari builder SQLite biasa: yang dipakai adalah **SQLCipher**, jadi
 * isi database terenkripsi di disk. Driver-nya
 * `net.zetetic.database.sqlcipher.driver.SQLCipherDriver` — kelas yang sudah
 * mengimplementasikan `androidx.sqlite.SQLiteDriver` — sehingga Room tetap
 * bisa talking lewat API standarnya.
 *
 * Kuncinya dipasang **di dalam driver**, tepat setelah koneksi dibuka, bukan
 * lewat `PRAGMA key` yang dijalankan belakangan. Urutannya penting: Room
 * membaca `user_version` dan skema sebelum statement aplikasi apa pun, jadi
 * kunci yang dipasang belakangan akan terlambat.
 *
 * Penting: `sqlite-bundled` (SQLite bawaan) **tidak** boleh dipakai di target
 * yang sama dengan SQLCipher — keduanya mengekspor simbol `sqlite3_*` yang
 * sama, dan linker bisa memilih yang salah sehingga `PRAGMA key` diabaikan
 * tanpa error. Karena itu `sqlite-bundled` hanya dipakai di target yang tidak
 * memakai SQLCipher.
 *
 * @param context dipakai untuk menemukan lokasi database; yang dipakai adalah
 *   `applicationContext` supaya builder tidak menahan Activity.
 */
fun getDatabaseBuilder(
    context: Context,
): RoomDatabase.Builder<ITalaseDatabase> {
    val appContext: Context = context.applicationContext
    val dbFile = appContext.getDatabasePath(DbFiles.NAME)
    return Room.databaseBuilder<ITalaseDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}

/**
 * Driver SQLCipher untuk Android.
 *
 * Dibuat di sini, bukan di DI, karena driver ini yang memegang kunci — dan
 * kuncinya harus berasal dari nilai yang sama di semua pemanggilan.
 *
 * `SQLCipherDriver` punya connection pool sendiri, jadi Room bisa membuka
 * beberapa koneksi; kunci dipasang pada **setiap** koneksi karena
 * `open()` dipanggil sekali per koneksi.
 *
 * Argumen kedua dan ketiga adalah hook dan error handler; keduanya `null`
 * karena:
 *
 * - Hook dipakai untuk menyuntik perintah sebelum/sesudah kunci — tidak
 *   perlu di sini, kuncinya sudah diberikan ke driver.
 * - **Error handler `null` berarti "jangan hapus database saat korup".**
 *   Handler bawaan SQLCipher menghapus berkas saat breathless corruption,
 *   dan kunci yang salah akan memicu jalur itu. Untuk aplikasi yang menyimpan
 *   satu-satunya salinan riwayat penjualan, menghapus berkas karena kunci
 *   salah jauh lebih berbahaya daripada membiarkan app gagal start.
 */
internal fun createSqlCipherDriver(passphrase: String): SQLCipherDriver {
    loadSqlCipherNativeLibrary()
    return SQLCipherDriver(passphrase.toByteArray(Charsets.UTF_8), null, null)
}

/**
 * Muat pustaka native SQLCipher.
 *
 * **Wajib dipanggil sebelum operasi SQLCipher apa pun.** Pustaka `libsqlcipher.so`
 * berisi implementasi JNI dari `nativeOpen`/`nativeKey`; tanpa itu, aplikasi
 * berakhir dengan `UnsatisfiedLinkError` saat Room membuka database.
 *
 * `System.loadLibrary` idempoten: pemanggilan kedua pada library yang sama
 * tidak melakukan apa-apa, jadi tidak perlu remembers apakah sudah dimuat.
 */
private fun loadSqlCipherNativeLibrary() {
    try {
        System.loadLibrary("sqlcipher")
    } catch (t: Throwable) {
        // Kegagalan memuat pustaka berarti database tidak bisa dibuka sama
        // sekali. Melempar error apa adanya lebih baik daripada membiarkan
        // aplikasi jalan dengan database yang tidak bisa dipakai.
        throw IllegalStateException("Gagal memuat pustaka native SQLCipher", t)
    }
}

/** Driver terenkripsi untuk Android — implementasi dari `commonMain`. */
actual fun createEncryptedDriver(passphrase: String): SQLiteDriver =
    createSqlCipherDriver(passphrase)

/**
 * Migrasikan database SQLite polos menjadi database SQLCipher terenkripsi.
 *
 * Alurnya (semua lewat API SQLCipher, tanpa menyalin berkas di level
 * filesystem — menyalin berkas polos apa adanya tidak akan mengenkripsi
 * isinya):
 *
 * 1. Database polos dibuka **tanpa kunci** (SQLCipher dengan password `null`
 *    = mode polos).
 * 2. Database terenkripsi sementara dibuat dan di-`ATTACH` sebagai skema
 *    `enc` dengan kunci.
 * 3. `sqlcipher_export` menyalin seluruh skema dan data ke database terenkripsi.
 * 4. `PRAGMA enc.user_version` disalin manual — `sqlcipher_export` memang
 *    **tidak** menyalin versi skema, dan Room membacanya untuk memutuskan
 *    migrasi skema.
 * 5. Database terenkripsi menggantikan berkas yang lama.
 *
 * @param dbFile berkas database yang ada (bisa tidak ada).
 * @param passphrase kunci enkripsi 64 karakter heksadesimal.
 * @return true kalau migrasi benar-benar terjadi.
 */
internal fun migratePlainDatabaseIfNeeded(dbFile: File, passphrase: String): Boolean {
    if (!dbFile.exists() || dbFile.length() == 0L) return false
    val header = ByteArray(16)
    dbFile.inputStream().use { stream ->
        if (stream.read(header) < header.size) return false
    }
    if (!isPlainSqliteHeader(header)) return false

    // Migrasi ini sendiri memakai API SQLCipher, jadi pustaka native harus
    // sudah dimuat sebelum baris pertama database dibuka.
    loadSqlCipherNativeLibrary()
    Log.i(TAG, "Database terdeteksi masih polos; meng-enkripsi ke SQLCipher.")
    val encryptedFile = File(dbFile.parentFile, "${dbFile.name}.encrypted")
    encryptedFile.delete()
    var source: SQLiteDatabase? = null
    try {
        // Password null = mode polos: file dibaca apa adanya, tanpa enkripsi.
        // Tipe argumen ditulis eksplisit karena SQLCipher punya beberapa
        // overload `openDatabase` dengan jumlah argumen sama — tanpa ini,
        // Kotlin bisa memilih overload yang parameter flags-nya `Int`.
        // Error handler null = jangan hapus berkas saat terjadi korup.
        //
        // `CREATE_IF_NECESSARY` itu WAJIB: database yang di-`ATTACH` mewarisi
        // flag buka dari database utama, jadi tanpa flag ini file hasil
        // enkripsi tidak bisa dibuat ("unable to open database"). SQLCipher
        // sendiri memakai flag yang sama saat ia memindahkan database.
        source = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null as ByteArray?,
            null as SQLiteDatabase.CursorFactory?,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,
            null as DatabaseErrorHandler?,
            null as SQLiteDatabaseHook?,
        )
        // Versi skema dibaca dari header berkas (offset 60), bukan lewat cursor:
        // cursor SQLCipher bisa melepas koneksi ke pool dan memutus ikatan
        // database `enc` yang sedang di-attach.
        val version: Int = readUserVersionFromFile(dbFile)

        // **Tanpa transaksi.** SQLCipher sendiri menjalankan migrasi ini di luar
        // transaksi (`sqlcipher_codec_ctx_migrate` menolak jalan di dalam
        // transaksi), dan `PRAGMA enc.user_version = …` memang gagal dengan
        // "database enc is locked" kalau ada transaksi yang sedang menulis ke
        // database `enc`.
        //
        // Urutan perintahnya penting:
        // - `execSQL` (bukan `rawExecSQL`): `rawExecSQL` membuang kode error,
        //   jadi perintah yang GAGAL lolos diam-diam dan gejalanya baru
        //   muncul di perintah berikutnya.
        // - `sqlcipher_export` lewat `rawExecSQL`: `execSQL` menolak SELECT,
        //   dan `rawQuery` membuka cursor yang bisa melepas koneksi ke pool
        //   sehingga database `enc` yang ter-attach hilang lagi.
        source.execSQL("ATTACH DATABASE '${encryptedFile.absolutePath}' AS enc KEY '$passphrase'")
        source.rawExecSQL("SELECT sqlcipher_export('enc')")
        // sqlcipher_export tidak menyalin user_version; Room butuh nilainya
        // supaya tidak menganggap skema perlu dimigrasi dari nol.
        source.execSQL("PRAGMA enc.user_version = $version")
        source.execSQL("DETACH DATABASE enc")
        source.close()
        source = null

        // Ganti berkas lama dengan yang terenkripsi. Sidecar WAL/SHM milik
        // berkas polos tidak boleh ikut: isinya plaintext dan bisa merusak
        // database terenkripsi yang baru.
        dbFile.delete()
        for (suffix in WAL_SUFFIXES) {
            File(dbFile.parentFile, dbFile.name + suffix).delete()
        }
        if (!encryptedFile.renameTo(dbFile)) {
            Log.e(TAG, "Gagal mengganti berkas database dengan versi terenkripsi.")
            return false
        }
        Log.i(TAG, "Database berhasil di-enkripsi.")
        return true
    } catch (t: Throwable) {
        Log.e(TAG, "Migrasi ke SQLCipher gagal; database polos dipertahankan.", t)
        encryptedFile.delete()
        return false
    } finally {
        source?.close()
    }
}

/** Tag log untuk pesan database. */
private const val TAG = "iTalaseDb"

/**
 * Baca `PRAGMA user_version` langsung dari header berkas.
 *
 * Room menyimpan versi skema di header SQLite, offset 60, sebagai integer
 * 4 byte big-endian. Membacanya langsung dari berkas (bukan lewat cursor)
 * penting untuk migrasi: cursor SQLCipher bisa melepas koneksi ke pool dan
 * memutus ikatan database `enc` yang sedang di-attach.
 *
 * @return nomor versi skema; 0 kalau header tidak bisa dibaca.
 */
private fun readUserVersionFromFile(dbFile: File): Int {
    return try {
        dbFile.inputStream().use { stream ->
            stream.skip(USER_VERSION_OFFSET)
            val bytes = ByteArray(4)
            if (stream.read(bytes) < 4) return 0
            // Big-endian, seperti format header SQLite.
            ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Gagal membaca user_version dari berkas; diasumsikan 0.", t)
        0
    }
}

/** Offset `user_version` di header berkas SQLite. */
private const val USER_VERSION_OFFSET = 60L

/** Sidecar yang dibuat SQLite; harus ikut terhapus saat berkas diganti. */
private val WAL_SUFFIXES = listOf("-wal", "-shm", "-journal")
