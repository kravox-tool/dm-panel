package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY lastRun DESC")
    suspend fun getAllScripts(): List<ScriptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity): Long

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScript(id: Long)
}
