package com.anlian.italase.shared.data.local

/** Nama file SQLite; dipakai Android (getDatabasePath) dan iOS (Documents)
 *  supaya kedua platform menunjuk file database yang sama dan model backup
 *  bisa menyebut satu nama saja. */
object DbFiles {
    /** Satu-satunya tempat nama file database didefinisikan. */
    const val NAME = "italase.db"
}
