package com.telepromptervibes.app.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Facade tipis dari UI ke OverlayService.
 * TIDAK menyimpan state scroll, TIDAK menyentuh WindowManager.
 * Hanya start/stop service lewat Intent.
 */
object OverlayController {

    fun start(context: Context, script: String) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_SCRIPT, script)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        context.startService(intent)
    }
}
