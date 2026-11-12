package com.telepromptervibes.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ResultReceiver
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.telepromptervibes.app.R

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        const val ACTION_START = "com.telepromptervibes.app.action.START"
        const val ACTION_STOP = "com.telepromptervibes.app.action.STOP"
        const val EXTRA_SCRIPT = "extra_script"
        const val EXTRA_SPEED_DP_PER_SEC = "extra_speed_dp_per_sec"
        const val EXTRA_INITIAL_PROGRESS = "extra_initial_progress"
        const val EXTRA_RESULT_RECEIVER = "extra_result_receiver"
        const val RESULT_OVERLAY_STARTED = 1001
        const val RESULT_OVERLAY_FAILED = 1002
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "teleprompter_overlay"
        private const val NOTIF_ID = 1001
        private const val PERMISSION_CHECK_INTERVAL_MS = 1000L
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val handler = Handler(Looper.getMainLooper())
    private val permissionCheck = object : Runnable {
        override fun run() {
            if (!Settings.canDrawOverlays(this@OverlayService)) {
                Log.w(TAG, "Overlay permission revoked, stopping service")
                stopSelf()
                return
            }
            handler.postDelayed(this, PERMISSION_CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val script = intent.getStringExtra(EXTRA_SCRIPT).orEmpty()
                val speedDpPerSec = intent.getFloatExtra(EXTRA_SPEED_DP_PER_SEC, 40f)
                val initialProgress = intent.getFloatExtra(EXTRA_INITIAL_PROGRESS, 0f)
                val resultReceiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_RECEIVER, ResultReceiver::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)
                }
                startForeground(NOTIF_ID, buildNotification())
                showOverlay(script, speedDpPerSec, initialProgress, resultReceiver)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(
        script: String,
        speedDpPerSec: Float,
        initialProgress: Float,
        resultReceiver: ResultReceiver?,
    ) {
        if (overlayView != null) {
            resultReceiver?.send(RESULT_OVERLAY_STARTED, null)
            return
        }

        // Cek izin SEBELUM addView
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "showOverlay: overlay permission not granted")
            resultReceiver?.send(RESULT_OVERLAY_FAILED, null)
            stopSelf()
            return
        }

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val halfScreenHeight = (screenHeight * 0.5f).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            halfScreenHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 120
        }

        lateinit var view: ComposeView
        view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                OverlayContent(
                    script = script,
                    speedDpPerSec = speedDpPerSec,
                    initialProgress = initialProgress,
                    onStop = { stopSelf() },
                    onDrag = { dx, dy ->
                        val screenWidth = resources.displayMetrics.widthPixels
                        val screenHeight = resources.displayMetrics.heightPixels

                        // Update posisi
                        params.x = (params.x + dx.toInt()).coerceIn(-screenWidth + 100, screenWidth - 100)
                        params.y = (params.y + dy.toInt()).coerceIn(0, screenHeight - 100)

                        // Paksa update
                        runCatching {
                            windowManager.updateViewLayout(view, params)
                        }.onFailure { e ->
                            android.util.Log.e("OverlayService", "Drag update failed", e)
                        }
                    },
                )
            }
        }

        // Tangkap addView failure
        val addResult = runCatching {
            windowManager.addView(view, params)
        }
        if (addResult.isFailure) {
            Log.e(TAG, "addView failed", addResult.exceptionOrNull())
            resultReceiver?.send(RESULT_OVERLAY_FAILED, null)
            stopSelf()
            return
        }

        overlayView = view
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        startPermissionPolling()
        resultReceiver?.send(RESULT_OVERLAY_STARTED, null)
    }

    private fun startPermissionPolling() {
        handler.removeCallbacks(permissionCheck)
        handler.postDelayed(permissionCheck, PERMISSION_CHECK_INTERVAL_MS)
    }

    private fun stopPermissionPolling() {
        handler.removeCallbacks(permissionCheck)
    }

    override fun onDestroy() {
        stopPermissionPolling()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notif_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        return builder
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(0, getString(R.string.overlay_notif_stop), stopIntent)
            .build()
    }
}
