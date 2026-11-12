package com.telepromptervibes.app.scrolling

/**
 * Pure scroll engine. Unit: Pixels, Seconds.
 * Tidak ada Android import. Mudah di-unit-test.
 */
class ScrollEngine(
    speedPxPerSec: Float,
    initialMaxOffsetPx: Float,
) {
    var speedPxPerSec: Float = if (speedPxPerSec.isFinite()) speedPxPerSec.coerceAtLeast(0f) else 0f
        set(value) {
            field = if (value.isFinite()) value.coerceAtLeast(0f) else 0f
        }

    var maxOffsetPx: Float = if (initialMaxOffsetPx.isFinite()) initialMaxOffsetPx.coerceAtLeast(0f) else 0f
        set(value) {
            val safeMax = if (value.isFinite()) value.coerceAtLeast(0f) else 0f
            field = safeMax
            currentOffsetPx = currentOffsetPx.coerceIn(0f, safeMax)
        }

    var currentOffsetPx: Float = 0f
        private set

    val isFinished: Boolean
        get() = maxOffsetPx > 0f && currentOffsetPx >= maxOffsetPx

    /** Majukan waktu. deltaSeconds dalam detik (bukan ms). */
    fun advance(deltaSeconds: Float) {
        if (!deltaSeconds.isFinite() || deltaSeconds <= 0f) return
        if (isFinished) return
        
        val deltaPx = speedPxPerSec * deltaSeconds
        if (!deltaPx.isFinite() || deltaPx <= 0f) return

        currentOffsetPx = (currentOffsetPx + deltaPx).coerceIn(0f, maxOffsetPx)
    }

    /** Sinkronisasi posisi dari ScrollState (saat manual drag). */
    fun syncTo(offsetPx: Float) {
        val safe = if (offsetPx.isFinite()) offsetPx else 0f
        currentOffsetPx = safe.coerceIn(0f, maxOffsetPx)
    }
}
