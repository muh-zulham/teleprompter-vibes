package com.telepromptervibes.app.di

import android.content.Context
import androidx.room.Room
import com.telepromptervibes.app.data.local.AppDatabase
import com.telepromptervibes.app.data.repository.ScriptRepositoryImpl
import com.telepromptervibes.app.domain.repository.ScriptRepository

/**
 * Manual DI container. Satu instance per Application lifecycle.
 * Tidak pakai Hilt (keputusan owner 2A).
 *
 * Catatan: TANPA fallbackToDestructiveMigration().
 * Kalau schema berubah tanpa Migration, Room akan crash saat runtime
 * (data utuh, bukan terhapus diam-diam).
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "teleprompter.db"
    ).build()

    val scriptRepository: ScriptRepository = ScriptRepositoryImpl(database.scriptDao())
}
