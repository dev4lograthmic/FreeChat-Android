package io.github.freechat.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.data.store.AppSettings
import io.github.freechat.android.core.model.ChatSourceOption
import io.github.freechat.android.core.model.MessageContentSource
import io.github.freechat.android.core.ui.R
import io.github.freechat.android.core.ui.components.FreeChatSectionHeader
import io.github.freechat.android.core.ui.components.FreeChatSettingRow

@Composable
fun DeveloperOptionsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    DeveloperOptionsScreen(
        settings = settings,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onUpdateSettings = { transform, reconnect ->
            viewModel.updateSettings(transform, reconnect)
        },
        getCharacterJson = {
            viewModel.activeChat?.sessionInfo?.rawCharacterJson
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperOptionsScreen(
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings, Boolean) -> Unit,
    getCharacterJson: () -> String?,
) {
    var showCharacterDialog by remember { mutableStateOf(false) }
    var characterJson by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_developer_options)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FreeChatSectionHeader(
                    text = stringResource(R.string.developer_section_character),
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    FreeChatSettingRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.developer_character_info),
                        description = stringResource(R.string.developer_character_info_summary),
                        onClick = {
                            characterJson = getCharacterJson()
                            showCharacterDialog = true
                        },
                    )
                }
            }

            item {
                FreeChatSectionHeader(
                    text = stringResource(R.string.developer_section_subscriptions),
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        ChatSourceOption.entries.forEachIndexed { index, source ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            SubscriptionSourceRow(
                                source = source,
                                checked = source in settings.developerSources,
                                onToggle = {
                                    onUpdateSettings(
                                        {
                                            it.copy(
                                                developerSources = toggleDeveloperSource(
                                                    it.developerSources,
                                                    source,
                                                ),
                                            )
                                        },
                                        true,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.developer_subscription_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.developer_subscription_duplicate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                FreeChatSectionHeader(
                    text = stringResource(R.string.developer_section_message_content),
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        MessageContentSource.entries.forEachIndexed { index, source ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            MessageContentRow(
                                source = source,
                                selected = settings.developerMessageContent == source,
                                onSelect = {
                                    onUpdateSettings(
                                        { it.copy(developerMessageContent = source) },
                                        true,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.developer_raw_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                FreeChatSectionHeader(
                    text = stringResource(R.string.developer_section_switches),
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        DeveloperSwitchRow(
                            title = stringResource(R.string.developer_raw_commands),
                            checked = settings.developerRawCommands,
                            onCheckedChange = { value ->
                                onUpdateSettings(
                                    { it.copy(developerRawCommands = value) },
                                    false,
                                )
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DeveloperSwitchRow(
                            title = stringResource(R.string.developer_split_toast),
                            checked = settings.developerSplitToast,
                            onCheckedChange = { value ->
                                onUpdateSettings(
                                    { it.copy(developerSplitToast = value) },
                                    false,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCharacterDialog) {
        AlertDialog(
            onDismissRequest = { showCharacterDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.developer_character_info)) },
            text = {
                val json = characterJson
                if (json.isNullOrBlank()) {
                    Text(stringResource(R.string.developer_character_unavailable))
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SelectionContainer {
                            Text(
                                text = json,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCharacterDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }
}

@Composable
private fun SubscriptionSourceRow(
    source: ChatSourceOption,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(source.developerLabel()) },
        trailingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
        modifier = Modifier.clickable(onClick = onToggle),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        tonalElevation = 0.dp,
    )
}

@Composable
private fun MessageContentRow(
    source: MessageContentSource,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(source.developerLabel()) },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        modifier = Modifier.clickable(onClick = onSelect),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        tonalElevation = 0.dp,
    )
}

@Composable
private fun DeveloperSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        tonalElevation = 0.dp,
    )
}

private fun toggleDeveloperSource(
    current: Set<ChatSourceOption>,
    source: ChatSourceOption,
): Set<ChatSourceOption> {
    val next = if (source in current) current - source else current + source
    return next.ifEmpty { setOf(ChatSourceOption.POST_HANDLE) }
}

private fun ChatSourceOption.developerLabel(): String = name

private fun MessageContentSource.developerLabel(): String = when (this) {
    MessageContentSource.TEXT -> "TEXT"
    MessageContentSource.ORIGINAL_TEXT -> "ORIGINAL_TEXT"
    MessageContentSource.RAW_MESSAGE -> "RAW_MESSAGE"
    MessageContentSource.ORIGINAL_RAW_MESSAGE -> "ORIGINAL_RAW"
}
