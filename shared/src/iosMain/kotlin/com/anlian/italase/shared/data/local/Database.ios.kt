package com.anlian.italase.shared.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun getDatabaseBuilder(): RoomDatabase.Builder<ITalaseDatabase> {
    val dbPath: String = documentDirectory() + "/${DbFiles.NAME}"
    return Room.databaseBuilder<ITalaseDatabase>(name = dbPath)
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { MissingDocumentDir.MESSAGE }
}

private object MissingDocumentDir {
    const val MESSAGE = "Document directory not found"
}
