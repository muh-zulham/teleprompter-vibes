package com.telepromptervibes.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telepromptervibes.app.R
import com.telepromptervibes.app.TeleprompterApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    scriptId: String?,
    onNavigateBack: () -> Unit = {},
    vm: EditorViewModel = viewModel(
        factory = EditorViewModel.factory(
            (LocalContext.current.applicationContext as TeleprompterApp).container.scriptRepository,
            scriptId,
        )
    ),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val event by vm.events.collectAsStateWithLifecycle()

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        when (e) {
            is EditorEvent.NavigateBack -> {
                onNavigateBack()
                vm.consumeEvent()
            }
        }
    }

    BackHandler(enabled = true) {
        vm.onBackPressed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = vm::onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = vm::onSave,
                        enabled = !(state.title.isBlank() && state.content.isBlank()) && !state.isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.editor_save),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.editor_title_placeholder)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp),
                label = { Text(stringResource(R.string.editor_content_placeholder)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Default,
                ),
            )
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = vm::onDiscardDismissed,
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(stringResource(R.string.editor_discard_message)) },
            confirmButton = {
                TextButton(onClick = vm::onDiscardConfirmed) {
                    Text(stringResource(R.string.editor_discard_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::onDiscardDismissed) {
                    Text(stringResource(R.string.editor_discard_no))
                }
            },
        )
    }
}
