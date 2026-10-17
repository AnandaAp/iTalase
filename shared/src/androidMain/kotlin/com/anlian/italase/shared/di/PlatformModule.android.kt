package com.anlian.italase.shared.di

import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.getDatabaseBuilder
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

@Module
actual class PlatformModule: KoinComponent {
    @Singleton
    fun provideDb(): ITalaseDatabase {
        val context = AndroidAppContext.ctx
        val builder = getDatabaseBuilder(context, ByteArray(0))
        return builder
            .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                com.anlian.italase.shared.data.local.MIGRATION_1_2,
                com.anlian.italase.shared.data.local.MIGRATION_2_3
            )
            .build()
    }
}
