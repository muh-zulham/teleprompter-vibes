package com.telepromptervibes.app.overlay

import android.content.Context
import android.content.Intent
import android.os.ResultReceiver
import androidx.core.content.ContextCompat

/**
 * Facade tipis dari UI ke OverlayService.
 * TIDAK menyimpan state scroll, TIDAK menyentuh WindowManager.
 * Hanya start/stop service lewat Intent.
 */
object OverlayController {

    fun start(
        context: Context,
        script: String,
        speedDpPerSec: Float,
        initialProgress: Float,
        resultReceiver: ResultReceiver? = null,
    ) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_SCRIPT, script)
            putExtra(OverlayService.EXTRA_SPEED_DP_PER_SEC, speedDpPerSec)
            putExtra(OverlayService.EXTRA_INITIAL_PROGRESS, initialProgress.coerceIn(0f, 1f))
            putExtra(OverlayService.EXTRA_RESULT_RECEIVER, resultReceiver)
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
