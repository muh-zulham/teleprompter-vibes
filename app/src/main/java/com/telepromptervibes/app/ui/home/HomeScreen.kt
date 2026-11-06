package com.telepromptervibes.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telepromptervibes.app.overlay.OverlayController
import com.telepromptervibes.app.permission.OverlayPermission

@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val event by vm.events.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        when (e) {
            is HomeEvent.StartPrompter -> {
                if (!OverlayPermission.canDrawOverlays(context)) {
                    OverlayPermission.openSettings(context)
                } else {
                    OverlayController.start(context, e.script)
                }
                vm.consumeEvent()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Teleprompter",
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = state.script,
            onValueChange = vm::onScriptChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Naskah") },
            placeholder = { Text("Tulis naskah di sini…") },
        )
        Button(
            onClick = vm::onStartClicked,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.script.isNotBlank(),
        ) {
            Text("Start Prompter")
        }
    }
}
