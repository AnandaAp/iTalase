package com.anlian.italase.shared.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<ITalaseDatabase> {
    val appContext: Context = context.applicationContext
    val dbFile = appContext.getDatabasePath(DbFiles.NAME)
    return Room.databaseBuilder<ITalaseDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
