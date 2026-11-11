package com.telepromptervibes.app.data.repository

import com.telepromptervibes.app.data.local.ScriptDao
import com.telepromptervibes.app.data.local.ScriptEntity
import com.telepromptervibes.app.data.local.toDomain
import com.telepromptervibes.app.data.local.toEntity
import com.telepromptervibes.app.domain.model.Script
import com.telepromptervibes.app.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ScriptRepositoryImpl(
    private val dao: ScriptDao,
) : ScriptRepository {

    override fun observeAll(): Flow<List<Script>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Script? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(script: Script) {
        dao.upsert(script.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun createNew(title: String, content: String): Script {
        val now = System.currentTimeMillis()
        val entity = ScriptEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifEmpty { "Untitled" },
            content = content,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }
}
