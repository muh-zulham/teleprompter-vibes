package com.telepromptervibes.app.scrolling

/**
 * Pure scroll engine: menghitung offset berdasarkan waktu yang berlalu,
 * bukan jumlah karakter. Lihat handover §7.
 *
 * Semua satuan dalam dp. Tidak ada Android import — mudah di-unit-test.
 */
class ScrollEngine(
    private val speedDpPerSec: Float,
    private val contentHeightDp: Float,
    private val viewportHeightDp: Float,
) {
    private var accumulatedMs: Long = 0L

    /** Offset saat ini dalam dp, dibatasi [0, maxOffsetDp]. */
    val offsetDp: Float
        get() {
            val raw = (speedDpPerSec * (accumulatedMs / 1000f))
            return raw.coerceIn(0f, maxOffsetDp)
        }

    val maxOffsetDp: Float
        get() = (contentHeightDp - viewportHeightDp).coerceAtLeast(0f)

    val isFinished: Boolean
        get() = offsetDp >= maxOffsetDp && maxOffsetDp > 0f

    /** Majukan waktu. Dipanggil dari frame loop saat state = playing. */
    fun advance(deltaMs: Long) {
        if (deltaMs > 0) accumulatedMs += deltaMs
    }

    /** Pause = berhenti menambah waktu. Posisi otomatis terjaga. */
    fun pause() { /* no-op: advance() tidak dipanggil */ }

    /** Resume = lanjut menambah waktu dari accumulatedMs yang sama. */
    fun resume() { /* no-op: advance() dipanggil lagi */ }

    fun reset() {
        accumulatedMs = 0L
    }
}
