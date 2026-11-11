package com.telepromptervibes.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.editor_title_placeholder)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text(stringResource(R.string.editor_content_placeholder)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Default,
                ),
            )

            // Word count + estimasi durasi (derived, bukan state)
            if (state.wordCount > 0) {
                Text(
                    text = stringResource(R.string.editor_stats, state.wordCount, state.estimatedMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Start Prompter: placeholder disabled (Prompter Mode belum ada)
            Button(
                onClick = { /* Placeholder — Prompter Mode di Langkah 3 */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = "  " + stringResource(R.string.editor_start_prompter),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.editor_start_prompter_coming_soon),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = vm::onDiscardDismissed,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.editor_discard_title), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.editor_discard_message), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = vm::onDiscardConfirmed) {
                    Text(stringResource(R.string.editor_discard_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::onDiscardDismissed) {
                    Text(stringResource(R.string.editor_discard_no), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}
