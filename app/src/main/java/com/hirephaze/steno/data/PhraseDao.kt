package com.hirephaze.steno.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases ORDER BY shortcut COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Phrase>>

    @Query("SELECT * FROM phrases ORDER BY shortcut COLLATE NOCASE ASC")
    suspend fun getAll(): List<Phrase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: Phrase): Long

    @Update
    suspend fun update(phrase: Phrase)

    @Delete
    suspend fun delete(phrase: Phrase)

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun count(): Int
}
