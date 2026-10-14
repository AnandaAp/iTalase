package com.anlian.italase.shared.di

import androidx.room.RoomDatabase
import com.anlian.italase.shared.data.local.ITalaseDatabase
import com.anlian.italase.shared.data.local.getDatabaseBuilder
import com.anlian.italase.shared.data.local.getRoomDatabase
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
actual class PlatformModule {
    @Singleton
    fun provideDbBuilder(): RoomDatabase.Builder<ITalaseDatabase> =
        getDatabaseBuilder(AndroidAppContext.ctx)

    @Singleton
    fun provideDb(builder: RoomDatabase.Builder<ITalaseDatabase>): ITalaseDatabase =
        getRoomDatabase(builder)
}
