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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telepromptervibes.app.R
import com.telepromptervibes.app.TeleprompterApp
import com.telepromptervibes.app.scrolling.ScrollEngine
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val AUTO_HIDE_MS = 2000L

// Layout constants (approved)
private const val TOP_SPACER_FRACTION = 0.65f
private const val BOTTOM_SPACER_FRACTION = 0.60f
private const val GUIDE_POSITION_FRACTION = 0.425f
private const val GUIDE_ALPHA = 0.40f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrompterScreen(
    scriptId: String,
    onExit: () -> Unit = {},
    vm: PrompterViewModel = viewModel(
        factory = PrompterViewModel.factory(
            (LocalContext.current.applicationContext as TeleprompterApp).container.scriptRepository,
            scriptId,
        )
    ),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Pause otomatis saat rotasi (Composable di-recreate)
    // LaunchedEffect(Unit) berjalan sekali setiap kali Composable masuk ke composition baru.
    LaunchedEffect(Unit) {
        if (vm.uiState.value.isPlaying) {
            vm.onPlayPauseToggle()
        }
    }

    LaunchedEffect(state.isNotFound) {
        if (state.isNotFound) onExit()
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
    
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val speedPxPerSec = with(density) { speedDpPerSec.dp.toPx() }

    // Engine stabil
    val engine = remember {
        ScrollEngine(speedPxPerSec = speedPxPerSec, initialMaxOffsetPx = 0f)
    }

    // Sync speed
    LaunchedEffect(speedPxPerSec) {
        engine.speedPxPerSec = speedPxPerSec
    }

    // Sync maxValue (resize)
    // Sinkronisasi maxValue dan restore posisi.
    // Menggunakan snapshotFlow agar hanya terpicu saat maxValue BERUBAH,
    // bukan setiap frame.
    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.maxValue }
            .collect { maxVal ->
                if (maxVal > 0) {
                    engine.maxOffsetPx = maxVal.toFloat()
                    // Pulihkan posisi hanya jika engine masih di awal (0f)
                    // dan ada savedOffsetPx dari rotasi sebelumnya.
                    if (engine.currentOffsetPx == 0f && savedOffsetPx > 0f) {
                        engine.syncTo(savedOffsetPx)
                    }
                    // Sinkronkan Compose ScrollState dengan engine (hanya sekali saat maxValue berubah)
                    val synced = engine.currentOffsetPx.roundToInt()
                    if (scrollState.value != synced) {
                        scrollState.scrollTo(synced)
                    }
                }
            }
    }

    // NestedScrollConnection: deteksi user drag
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag) {
                    isUserDragging = true
                }
                return Offset.Zero  // jangan konsumsi delta
            }
        }
    }

    // Deteksi drag selesai (finger lift / fling selesai)
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

    // Frame loop
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
                // Tetap update progress dan offset meski posisi sama (misal di akhir)
                onScrollOffsetChanged(engine.currentOffsetPx)
                if (engine.maxOffsetPx > 0f) {
                    onProgressChanged(engine.currentOffsetPx / engine.maxOffsetPx)
                }
            }
        }
    }

    // Auto-hide controls
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

        // Scrollable content (dengan spacer atas & bawah)
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

        // Reading guide (fixed, tidak ikut scroll)
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

        // Controls
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
