package com.telepromptervibes.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telepromptervibes.app.scrolling.ScrollEngine
import kotlin.math.roundToInt

private const val AUTO_HIDE_MS = 2000L

private const val TOP_SPACER_FRACTION = 0.65f
private const val BOTTOM_SPACER_FRACTION = 0.60f
private const val GUIDE_POSITION_FRACTION = 0.425f
private const val GUIDE_ALPHA = 0.40f

@Composable
fun OverlayContent(
    script: String,
    speedDpPerSec: Float,
    initialProgress: Float,
    onStop: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    var playing by remember { mutableStateOf(false) }
    var isUserDragging by remember { mutableStateOf(false) }
    var hasRestoredInitialProgress by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val speedPxPerSec = with(density) { speedDpPerSec.dp.toPx() }

    val engine = remember {
        ScrollEngine(speedPxPerSec = speedPxPerSec, initialMaxOffsetPx = 0f)
    }

    // Sync speed changes
    LaunchedEffect(speedPxPerSec) {
        engine.speedPxPerSec = speedPxPerSec
    }

    // Sync maxValue + restore initial progress sekali
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            engine.maxOffsetPx = scrollState.maxValue.toFloat()
            if (!hasRestoredInitialProgress && initialProgress > 0f) {
                val target = (initialProgress * scrollState.maxValue)
                    .roundToInt()
                    .coerceIn(0, scrollState.maxValue)
                engine.syncTo(target.toFloat())
                scrollState.scrollTo(target)
                hasRestoredInitialProgress = true
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
                return Offset.Zero
            }
        }
    }

    // Deteksi drag selesai
    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.isScrollInProgress }
            .collect { inProgress ->
                if (!inProgress && isUserDragging) {
                    engine.syncTo(scrollState.value.toFloat())
                    isUserDragging = false
                }
            }
    }

    // Frame loop
    LaunchedEffect(playing, isUserDragging, engine) {
        if (isUserDragging) return@LaunchedEffect
        if (!playing) return@LaunchedEffect

        var lastNanos = 0L
        while (playing) {
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
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x99101114))
            .nestedScroll(nestedScrollConnection)
    ) {
        val usableHeight = maxHeight

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(usableHeight * TOP_SPACER_FRACTION))
            Text(
                text = script,
                color = Color(0xFFF5F5F7),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 38.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(usableHeight * BOTTOM_SPACER_FRACTION))
        }

        // Reading guide (fixed)
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

        // Drag handle (top, 32dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { /* optional */ },
                        onDragEnd = { /* optional */ },
                        onDragCancel = { /* optional */ }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = null,
                tint = Color(0xFFF5F5F7).copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Controls (selalu tampil)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            IconButton(
                onClick = { playing = !playing },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0x66000000),
                    contentColor = Color.White,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Resume",
                )
            }
            IconButton(
                onClick = onStop,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0x66000000),
                    contentColor = Color.White,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Keluar overlay")
            }
        }
    }
}
