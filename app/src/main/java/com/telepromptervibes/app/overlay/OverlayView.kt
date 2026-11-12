package com.telepromptervibes.app.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telepromptervibes.app.scrolling.ScrollEngine
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val AUTO_HIDE_MS = 2000L
private const val SPEED_DP_PER_SEC = 40f

@Composable
fun OverlayContent(
    script: String,
    onStop: () -> Unit,
) {
    var playing by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val speedPxPerSec = with(density) { SPEED_DP_PER_SEC.dp.toPx() }

    val engine = remember {
        ScrollEngine(speedPxPerSec = speedPxPerSec, initialMaxOffsetPx = 0f)
    }

    LaunchedEffect(scrollState.maxValue) {
        engine.maxOffsetPx = scrollState.maxValue.toFloat()
    }

    LaunchedEffect(playing, engine) {
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
            if (targetOffset >= 0) {
                scrollState.scrollTo(targetOffset)
            }
        }
    }

    LaunchedEffect(lastInteraction, controlsVisible) {
        if (!controlsVisible) return@LaunchedEffect
        delay(AUTO_HIDE_MS)
        controlsVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .background(Color(0x99000000))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        lastInteraction = System.currentTimeMillis()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = script,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 38.sp,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            Row {
                IconButton(
                    onClick = {
                        playing = !playing
                        lastInteraction = System.currentTimeMillis()
                    },
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
                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                }
            }
        }
    }
}
