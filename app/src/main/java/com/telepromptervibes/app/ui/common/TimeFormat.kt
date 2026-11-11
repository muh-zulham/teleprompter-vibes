package com.telepromptervibes.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.telepromptervibes.app.R

/**
 * Format waktu relatif sederhana (manual, tanpa library).
 * Dipanggil dari Composable agar bisa akses stringResource.
 */
@Composable
fun formatRelativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - epochMillis).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L

    return when {
        minutes < 1L -> stringResource(R.string.time_just_now)
        minutes < 60L -> stringResource(R.string.time_minutes_ago, minutes.toInt())
        hours < 24L -> stringResource(R.string.time_hours_ago, hours.toInt())
        days == 1L -> stringResource(R.string.time_yesterday)
        else -> stringResource(R.string.time_days_ago, days.toInt())
    }
}
