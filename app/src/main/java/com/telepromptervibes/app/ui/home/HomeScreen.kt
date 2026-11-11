package com.telepromptervibes.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telepromptervibes.app.R
import com.telepromptervibes.app.TeleprompterApp
import com.telepromptervibes.app.domain.model.Script
import com.telepromptervibes.app.ui.common.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (String?) -> Unit = {},
    vm: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            (LocalContext.current.applicationContext as TeleprompterApp).container.scriptRepository
        )
    ),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val event by vm.events.collectAsStateWithLifecycle()

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        when (e) {
            is HomeEvent.NavigateToEditor -> {
                onNavigateToEditor(e.scriptId)
                vm.consumeEvent()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::onAddClicked) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_fab_new))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.scripts.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    ScriptList(
                        scripts = state.scripts,
                        onScriptClick = vm::onScriptClicked,
                        onDeleteRequested = vm::onDeleteRequested,
                    )
                }
            }
        }
    }

    state.scriptToDelete?.let { script ->
        AlertDialog(
            onDismissRequest = vm::onDeleteDismissed,
            title = { Text(stringResource(R.string.home_delete_confirm_title)) },
            text = { Text(stringResource(R.string.home_delete_confirm_message, script.title)) },
            confirmButton = {
                TextButton(onClick = vm::onDeleteConfirmed) {
                    Text(stringResource(R.string.home_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::onDeleteDismissed) {
                    Text(stringResource(R.string.home_delete_confirm_no))
                }
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ScriptList(
    scripts: List<Script>,
    onScriptClick: (String) -> Unit,
    onDeleteRequested: (Script) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(scripts, key = { it.id }) { script ->
            ScriptItem(
                script = script,
                onClick = { onScriptClick(script.id) },
                onDeleteRequested = { onDeleteRequested(script) },
            )
        }
    }
}

@Composable
private fun ScriptItem(
    script: Script,
    onClick: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = script.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatRelativeTime(script.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.home_item_menu))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_menu_delete)) },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequested()
                        },
                    )
                }
            }
        }
    }
}
