package com.hirephaze.steno.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Phrase::class], version = 1, exportSchema = false)
abstract class StenoDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao

    companion object {
        @Volatile private var instance: StenoDatabase? = null

        fun get(context: Context): StenoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StenoDatabase::class.java,
                    "steno.db",
                ).build().also { instance = it }
            }
    }
}
