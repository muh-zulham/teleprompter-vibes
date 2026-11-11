package com.telepromptervibes.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.telepromptervibes.app.domain.model.Script

@Entity(
    tableName = "scripts",
    indices = [Index(value = ["updatedAt"])]
)
data class ScriptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ScriptEntity.toDomain(): Script = Script(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Script.toEntity(): ScriptEntity = ScriptEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
