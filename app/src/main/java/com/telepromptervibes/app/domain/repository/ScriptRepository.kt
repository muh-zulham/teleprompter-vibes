package com.telepromptervibes.app.domain.repository

import com.telepromptervibes.app.domain.model.Script
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak repository di domain layer.
 * Implementasi di data layer (data/repository/ScriptRepositoryImpl).
 *
 * Catatan: method createNew menangani fallback judul "Untitled"
 * jika title kosong (keputusan owner 2A #6).
 */
interface ScriptRepository {
    fun observeAll(): Flow<List<Script>>
    suspend fun getById(id: String): Script?
    suspend fun upsert(script: Script)
    suspend fun delete(id: String)
    suspend fun createNew(title: String, content: String): Script
}
