package com.anlian.italase.shared.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * Modul Koin untuk seluruh kode bersama.
 *
 * `@ComponentScan` men-scan paket akar aplikasi, jadi setiap kelas ber-`@Singleton`
 * (seluruh ViewModel dan [ShopRepository]) terdaftar otomatis. Tidak ada daftar
 * binding manual yang harus ikut diperbarui setiap kali ada kelas baru.
 */
@Module
@ComponentScan("com.anlian.italase.shared")
object SharedModule
