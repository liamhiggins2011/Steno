package com.hirephaze.steno.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One expansion rule: type [shortcut], get [expansion].
 *
 * The expansion may contain placeholders resolved at expansion time:
 *   {date}   current date      {time}  current time
 *   {nl}     a newline         {cursor} where the caret lands afterwards
 */
@Entity(
    tableName = "phrases",
    indices = [Index(value = ["shortcut"], unique = true)],
)
data class Phrase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcut: String,
    val expansion: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
