package com.telepromptervibes.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: String)
}
