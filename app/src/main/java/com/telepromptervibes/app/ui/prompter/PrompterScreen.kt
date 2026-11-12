package com.telepromptervibes.app.ui.prompter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telepromptervibes.app.R
import com.telepromptervibes.app.TeleprompterApp
import com.telepromptervibes.app.overlay.OverlayService
import com.telepromptervibes.app.permission.OverlayPermission
import com.telepromptervibes.app.scrolling.ScrollEngine
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val AUTO_HIDE_MS = 2000L

private const val TOP_SPACER_FRACTION = 0.65f
private const val BOTTOM_SPACER_FRACTION = 0.60f
private const val GUIDE_POSITION_FRACTION = 0.425f
private const val GUIDE_ALPHA = 0.40f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrompterScreen(
    scriptId: String,
    onExit: () -> Unit = {},
    onStartOverlay: (String, Float, Float, (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    vm: PrompterViewModel = viewModel(
        factory = PrompterViewModel.factory(
            (LocalContext.current.applicationContext as TeleprompterApp).container.scriptRepository,
            scriptId,
        )
    ),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingStartAfterPermission by remember { mutableStateOf(false) }
    var showOverlayError by remember { mutableStateOf(false) }
    var isStartingOverlay by remember { mutableStateOf(false) }

    // Pause otomatis saat rotasi (Composable di-recreate)
    LaunchedEffect(Unit) {
        if (vm.uiState.value.isPlaying) {
            vm.onPlayPauseToggle()
        }
    }

    LaunchedEffect(state.isNotFound) {
        if (state.isNotFound) onExit()
    }

    // Recheck permission saat activity resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                if (OverlayPermission.canDrawOverlays(context)) {
                    startOverlay(
                        state = state,
                        onStartOverlay = onStartOverlay,
                        onExit = onExit,
                        onError = {
                            showOverlayError = true
                            isStartingOverlay = false
                        },
                        onFinished = { isStartingOverlay = false },
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = true) {
        onExit()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.prompter_exit))
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isStartingOverlay,
                        onClick = {
                        if (!OverlayPermission.canDrawOverlays(context)) {
                            pendingStartAfterPermission = true
                            showPermissionDialog = true
                        } else {
                            startOverlay(
                        state = state,
                        onStartOverlay = onStartOverlay,
                        onExit = onExit,
                        onError = {
                            showOverlayError = true
                            isStartingOverlay = false
                        },
                        onFinished = { isStartingOverlay = false },
                    )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.PictureInPictureAlt,
                            contentDescription = stringResource(R.string.prompter_overlay_button),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                state.script != null -> {
                    PrompterContent(
                        content = state.script!!.content,
                        isPlaying = state.isPlaying,
                        speedDpPerSec = state.speedDpPerSec,
                        progress = state.progress,
                        savedOffsetPx = state.savedOffsetPx,
                        onPlayPauseToggle = vm::onPlayPauseToggle,
                        onSpeedChange = vm::onSpeedChange,
                        onProgressChanged = vm::onProgressChanged,
                        onScrollOffsetChanged = vm::onScrollOffsetChanged,
                    )
                }
            }
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                pendingStartAfterPermission = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.prompter_overlay_title), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.prompter_overlay_message), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    OverlayPermission.openSettings(context)
                }) {
                    Text(stringResource(R.string.prompter_overlay_grant), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    pendingStartAfterPermission = false
                }) {
                    Text(stringResource(R.string.prompter_overlay_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Overlay error dialog
    if (showOverlayError) {
        AlertDialog(
            onDismissRequest = { showOverlayError = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.prompter_overlay_error_title), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.prompter_overlay_error_message), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showOverlayError = false }) {
                    Text(stringResource(R.string.prompter_overlay_ok), color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

private fun startOverlay(
    state: PrompterUiState,
    onStartOverlay: (String, Float, Float, (Boolean) -> Unit) -> Unit,
    onExit: () -> Unit,
    onError: () -> Unit,
    onFinished: () -> Unit,
) {
    val script = state.script ?: return
    val safeProgress = if (state.progress.isFinite()) state.progress.coerceIn(0f, 1f) else 0f

    var resultHandled = false
    val onResult: (Boolean) -> Unit = { success ->
        if (!resultHandled) {
            resultHandled = true
            onFinished()
            if (success) onExit() else onError()
        }
    }
    onStartOverlay(script.content, state.speedDpPerSec, safeProgress, onResult)
}

@Composable
private fun PrompterContent(
    content: String,
    isPlaying: Boolean,
    speedDpPerSec: Float,
    progress: Float,
    savedOffsetPx: Float,
    onPlayPauseToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onScrollOffsetChanged: (Float) -> Unit,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isUserDragging by remember { mutableStateOf(false) }
    var hasRestoredInitialProgress by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val speedPxPerSec = with(density) { speedDpPerSec.dp.toPx() }

    val engine = remember {
        ScrollEngine(speedPxPerSec = speedPxPerSec, initialMaxOffsetPx = 0f)
    }

    LaunchedEffect(speedPxPerSec) {
        engine.speedPxPerSec = speedPxPerSec
    }

    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            engine.maxOffsetPx = scrollState.maxValue.toFloat()
            if (!hasRestoredInitialProgress && savedOffsetPx > 0f) {
                val target = savedOffsetPx.roundToInt().coerceIn(0, scrollState.maxValue)
                engine.syncTo(target.toFloat())
                scrollState.scrollTo(target)
                hasRestoredInitialProgress = true
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag) {
                    isUserDragging = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { inProgress ->
                if (!inProgress && isUserDragging) {
                    engine.syncTo(scrollState.value.toFloat())
                    isUserDragging = false
                    lastInteraction = System.currentTimeMillis()
                }
            }
    }

    LaunchedEffect(isPlaying, isUserDragging, engine) {
        if (isUserDragging) return@LaunchedEffect
        if (!isPlaying) return@LaunchedEffect

        var lastNanos = 0L
        while (isPlaying) {
            var targetOffset = -1
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val deltaSeconds = (now - lastNanos) / 1_000_000_000f
                    engine.advance(deltaSeconds)
                    targetOffset = engine.currentOffsetPx.roundToInt()
                }
                lastNanos = now
            }
            if (targetOffset >= 0 && targetOffset != scrollState.value) {
                scrollState.scrollTo(targetOffset)
                onScrollOffsetChanged(engine.currentOffsetPx)
                if (engine.maxOffsetPx > 0f) {
                    onProgressChanged(engine.currentOffsetPx / engine.maxOffsetPx)
                }
            } else if (targetOffset >= 0) {
                onScrollOffsetChanged(engine.currentOffsetPx)
                if (engine.maxOffsetPx > 0f) {
                    onProgressChanged(engine.currentOffsetPx / engine.maxOffsetPx)
                }
            }
        }
    }

    LaunchedEffect(lastInteraction, controlsVisible) {
        if (!controlsVisible) return@LaunchedEffect
        delay(AUTO_HIDE_MS)
        controlsVisible = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        lastInteraction = System.currentTimeMillis()
                    }
                )
            }
    ) {
        val usableHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(usableHeight * TOP_SPACER_FRACTION))
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 44.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(usableHeight * BOTTOM_SPACER_FRACTION))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(usableHeight * GUIDE_POSITION_FRACTION)
                .align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomStart)
                    .background(Color(0xFFA78BFA).copy(alpha = GUIDE_ALPHA))
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.prompter_progress, (progress * 100f).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.prompter_speed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = speedDpPerSec,
                        onValueChange = {
                            onSpeedChange(it)
                            lastInteraction = System.currentTimeMillis()
                        },
                        valueRange = PrompterUiState.MIN_SPEED_DP_PER_SEC..PrompterUiState.MAX_SPEED_DP_PER_SEC,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = {
                            onPlayPauseToggle()
                            lastInteraction = System.currentTimeMillis()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.prompter_pause)
                                              else stringResource(R.string.prompter_play),
                        )
                    }
                }
            }
        }
    }
}
