package com.anlian.italase.shared.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// ════════════════════════════════════════════════════════════════════════
//  WAKTU & TANGGAL — satu implementasi untuk Android dan iOS
// ════════════════════════════════════════════════════════════════════════
//  Sebelumnya ini `expect`/`actual`: Android memakai `java.time`, iOS memakai
//  NSCalendar + NSDateFormatter. Dua implementasi berarti dua perilaku — dan
//  memang berbeda: yang satu menyimpan formatter sekali, yang lain membuatnya
//  tiap panggilan, dan nama bulan bergantung locale perangkat.
//
//  Sekarang semuanya lewat kotlinx-datetime di `commonMain`. Satu kode, satu
//  hasil, dan nama bulan Indonesia ditulis sendiri (di bawah) sehingga tidak
//  lagi bergantung pada locale perangkat yang bisa saja bukan `id_ID`.

/**
 * Jam dinding perangkat, dalam epoch millis.
 *
 * Zona waktu tidak terlibat di sini: epoch millis selalu UTC, dan zona baru
 * dipakai saat nilainya ditampilkan atau saat batas hari dihitung.
 */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Waktu sekarang sebagai epoch millis — pembungkus tipis [currentTimeMillis]. */
fun nowMillis(): Long = currentTimeMillis()

/** Zona waktu perangkat; dasar semua perhitungan "hari" di app ini. */
private fun zone(): TimeZone = TimeZone.currentSystemDefault()

/** Zona waktu **perangkat**, bukan UTC — dipakai penjual yang hidup di WIB/WITA/WIT. */
fun localDateTimeOf(millis: Long): LocalDateTime =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone())

/** Tanggal lokal dari sebuah epoch millis. */
fun localDateOf(millis: Long): LocalDate = localDateTimeOf(millis).date

/**
 * Awal hari menurut zona waktu perangkat.
 *
 * Zona lokal dipakai karena batas "hari ini" di laporan harus sama dengan hari
 * yang dialami penjual, bukan hari UTC.
 */
fun startOfLocalDayMillis(millis: Long): Long =
    localDateOf(millis).atStartOfDayIn(zone()).toEpochMilliseconds()

/**
 * Akhir hari = awal hari berikutnya dikurangi 1 ms.
 *
 * Rentang `[awal, akhir]` jadi tertutup penuh: tidak ada milidetik yang jatuh di
 * dua hari, dan tidak ada celah yang bisa membuat transaksi terlewat dari laporan.
 */
fun endOfLocalDayMillis(millis: Long): Long =
    localDateOf(millis).plus(DatePeriod(days = 1)).atStartOfDayIn(zone()).toEpochMilliseconds() - 1

/** Awal hari untuk sebuah tanggal; `null` berarti tanpa batas bawah. */
fun startOfDayMillis(date: LocalDate?): Long? =
    date?.atStartOfDayIn(zone())?.toEpochMilliseconds()

/** Akhir hari (23:59:59.999 lokal) untuk sebuah tanggal; `null` berarti tanpa batas atas. */
fun endOfDayMillis(date: LocalDate?): Long? =
    date?.plus(DatePeriod(days = 1))?.atStartOfDayIn(zone())?.toEpochMilliseconds()?.minus(1)

/**
 * Batas rentang tanggal inklusif untuk query `BETWEEN`.
 *
 * Mengembalikan pasangan `[awal, akhir]` yang selalu terisi, jadi pemanggil tidak
 * perlu memikirkan sisi mana yang kosong: sisi kosong jatuh ke nilai terjauh yang
 * aman untuk epoch millis.
 */
fun dayRangeMillis(from: LocalDate?, to: LocalDate?): Pair<Long, Long> =
    (startOfDayMillis(from) ?: Long.MIN_VALUE) to (endOfDayMillis(to) ?: Long.MAX_VALUE)

/**
 * Nama hari.
 *
 * Diambil dari `ordinal` enum [kotlinx.datetime.DayOfWeek], yang urutannya
 * dijamin MONDAY..SUNDAY — jadi indeks 0 = Senin. Sejak kotlinx-datetime 0.8,
 * `DayOfWeek` adalah enum biasa tanpa properti nomor; `ordinal` inilah API-nya.
 */
private val DAY_NAMES_ID = listOf(
    "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu",
)

/**
 * Nama bulan penuh dalam bahasa Indonesia.
 *
 * Indeks 0 = Januari, mengikuti `ordinal` enum [kotlinx.datetime.Month]
 * (JANUARY..DECEMBER).
 */
private val MONTH_NAMES_ID = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

/**
 * Singkatan bulan Indonesia (3 huruf) untuk tempat yang sempit.
 *
 * "Agu" untuk Agustus, bukan "Aug": inilah singkatan yang dipakai orang Indonesia,
 * jadi tanggal tetap terbaca sebagai bahasa Indonesia, bukan terjemahan.
 */
private val MONTH_ABBR_ID = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

/** Jam lokal `HH:mm` (24 jam). */
fun formatTime(millis: Long): String {
    val t: LocalDateTime = localDateTimeOf(millis)
    return "${twoDigits(t.hour)}:${twoDigits(t.minute)}"
}

/**
 * Tanggal Indonesia yang enak dibaca: `"17 Agustus 2026"`.
 *
 * Dipakai di tempat yang punya ruang — baris riwayat, judul laporan PDF.
 */
fun formatDateLong(millis: Long): String {
    val d: LocalDate = localDateOf(millis)
    return "${d.day} ${MONTH_NAMES_ID[d.month.ordinal]} ${d.year}"
}

/** Tanggal + jam, contoh `"17 Agustus 2026 • 09:41"`. */
fun formatDateTimeLong(millis: Long): String = "${formatDateLong(millis)} • ${formatTime(millis)}"

/** Tanggal ringkas untuk kolom sempit, contoh `"17 Agu 2026"`. */
fun formatDateShort(millis: Long): String {
    val d: LocalDate = localDateOf(millis)
    return "${d.day} ${MONTH_ABBR_ID[d.month.ordinal]} ${d.year}"
}

/**
 * Tanggal + jam ringkas, contoh `"17 Agu 2026 • 09:41"`.
 *
 * Bentuk inilah yang dipakai layar detail struk sejak awal, jadi formatnya
 * sengaja dipertahankan supaya tidak ada yang berubah tampilannya.
 */
fun formatDateTime(millis: Long): String = "${formatDateShort(millis)} • ${formatTime(millis)}"

/** Tanggal + jam + nama hari, contoh `"Senin, 17 Agustus 2026 • 09:41"`. */
fun formatDateTimeWithDay(millis: Long): String {
    val d: LocalDate = localDateOf(millis)
    return "${DAY_NAMES_ID[d.dayOfWeek.ordinal]}, ${formatDateTimeLong(millis)}"
}

/**
 * Label rentang tanggal untuk judul laporan.
 *
 * Satu tanggal ditulis sekali saja ("17 Agustus 2026"), rentang ditulis
 * "17 – 31 Agustus 2026" kalau bulannya sama, dan penuh kalau tidak. Sisi yang
 * kosong berarti terbuka ke arah itu.
 */
fun formatDateRange(from: LocalDate?, to: LocalDate?): String = when {
    from == null && to == null -> "Semua tanggal"
    from != null && to == null -> "Sejak ${formatDateLong(startOfDayMillis(from)!!)}"
    from == null && to != null -> "Sampai ${formatDateLong(endOfDayMillis(to)!!)}"
    from != null && to != null && from == to -> formatDateLong(startOfDayMillis(from)!!)
    from != null && to != null && from.year == to.year && from.month == to.month ->
        // Hubung biasa, bukan en dash: font standar PDF memetakan en dash ke kode
        // WinAnsi 0x96 yang di banyak pembaca diekstrak jadi "?" — laporan yang
        // di-copy-paste jadi "(1 ? 17)". Angka di sekitar tanda ini tidak pernah
        // ambigu, jadi hubung ASCII lebih aman dan tetap enak dibaca.
        "${from.day} - ${to.day} ${MONTH_NAMES_ID[to.month.ordinal]} ${to.year}"

    else -> "${formatDateLong(startOfDayMillis(from!!)!!)} – ${formatDateLong(endOfDayMillis(to!!)!!)}"
}

/**
 * Jumlah hari dalam rentang inklusif; `null` kalau salah satu sisi kosong.
 *
 * Dipakai laporan untuk menulis "7 hari" tanpa menghitung sendiri di UI.
 */
fun daysBetweenInclusive(from: LocalDate?, to: LocalDate?): Int? {
    if (from == null || to == null) return null
    return from.daysUntil(to) + 1
}

private fun LocalDate.daysUntil(to: LocalDate): Int =
    (to.toEpochDays() - this.toEpochDays()).toInt()

/** Pergeseran tanggal `n` hari; dipakai tombol pintasan rentang (7/30 hari). */
fun LocalDate.shiftDays(days: Int): LocalDate = this.plus(DatePeriod(days = days))

/** `n` hari sebelum tanggal ini. */
fun LocalDate.minusDays(days: Int): LocalDate = this.minus(DatePeriod(days = days))
