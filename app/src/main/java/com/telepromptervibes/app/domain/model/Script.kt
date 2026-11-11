package com.telepromptervibes.app.domain.model

/**
 * Domain model untuk naskah.
 * Sengaja tanpa anotasi Room agar domain layer tetap bersih.
 * Mapping ke ScriptEntity dilakukan di data layer.
 *
 * Catatan: isFavorite dan lastOpenedAt sengaja tidak ada (keputusan owner 2A).
 */
data class Script(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)
