package com.telepromptervibes.app.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OverlayPermission {

    /** Cek apakah izin SYSTEM_ALERT_WINDOW sudah diberikan. */
    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Buka halaman Settings untuk memberikan izin overlay.
     * User harus grant manual, lalu kembali ke app.
     */
    fun openSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
