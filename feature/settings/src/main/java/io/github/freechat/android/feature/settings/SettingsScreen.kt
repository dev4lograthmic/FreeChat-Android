package io.github.freechat.android.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.data.export.ExportFormat
import io.github.freechat.android.core.data.store.AppSettings
import io.github.freechat.android.core.model.AppLanguage
import io.github.freechat.android.core.model.ConnectionStatus
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.model.SessionInfo
import io.github.freechat.android.core.model.ThemeMode
import io.github.freechat.android.core.ui.R
import io.github.freechat.android.core.ui.components.FreeChatAvatar
import io.github.freechat.android.core.ui.components.FreeChatSectionHeader
import io.github.freechat.android.core.ui.components.FreeChatSettingRow
import io.github.freechat.android.core.ui.locale.LocaleController
import io.github.freechat.android.core.ui.theme.FreeChatTheme

@Composable
fun MeRoute(
    onOpenSettings: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val activeChat = viewModel.activeChat
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()

    MeScreen(
        profiles = profiles,
        activeProfile = activeProfile,
        activeSession = activeChat?.sessionInfo,
        connectionStatus = connectionStatus,
        onOpenSettings = onOpenSettings,
        onOpenPairing = onOpenPairing,
        onOpenAbout = onOpenAbout,
        onSelectProfile = viewModel::selectProfile,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeScreen(
    profiles: List<ServerProfile>,
    activeProfile: ServerProfile?,
    activeSession: SessionInfo?,
    connectionStatus: ConnectionStatus,
    onOpenSettings: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenAbout: () -> Unit,
    onSelectProfile: (String) -> Unit,
) {
    var sessionsExpanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (sessionsExpanded) 180f else 0f,
        label = "sessionArrow",
    )
    val currentSessionConnected = connectionStatus == ConnectionStatus.ONLINE &&
        activeProfile != null &&
        activeSession != null &&
        activeSession.profileId == activeProfile.profileId &&
        activeSession.characterId == activeProfile.characterId
    val topStatus = when {
        currentSessionConnected -> ConnectionStatus.ONLINE
        connectionStatus == ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
        else -> ConnectionStatus.OFFLINE
    }

    val characterName = activeSession?.characterName
        ?.takeIf { it.isNotBlank() }
        ?: activeProfile?.characterName
    val worldName = activeSession?.worldName
        ?.takeIf { it.isNotBlank() }
        ?: activeProfile?.worldName
    val serverAddress = activeProfile?.let { "${it.host}:${it.port}" }
    val displayName = when {
        characterName == null -> stringResource(R.string.me_no_character)
        worldName != null -> "$characterName@$worldName"
        else -> characterName
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_me),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FreeChatAvatar(
                                label = characterName ?: "?",
                                size = 64.dp,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = when (topStatus) {
                                        ConnectionStatus.ONLINE -> MaterialTheme.colorScheme.primary
                                        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
                                        ConnectionStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Badge(containerColor = statusColor)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(
                                            when (topStatus) {
                                                ConnectionStatus.ONLINE -> R.string.common_connected
                                                ConnectionStatus.CONNECTING -> R.string.common_connecting
                                                ConnectionStatus.OFFLINE -> R.string.common_not_connected
                                            },
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = statusColor,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = serverAddress
                                        ?: stringResource(R.string.me_no_server),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.me_switch_session))
                            },
                            supportingContent = {
                                Text(stringResource(R.string.me_switch_session_summary))
                            },
                            leadingContent = {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(arrowRotation),
                                )
                            },
                            modifier = Modifier.clickable {
                                sessionsExpanded = !sessionsExpanded
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            tonalElevation = 0.dp,
                        )

                        AnimatedVisibility(
                            visible = sessionsExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column {
                                profiles
                                    .filter { it.characterId != 0L }
                                    .forEach { profile ->
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                        SessionProfileRow(
                                            profile = profile,
                                            active = profile.profileId == activeProfile?.profileId,
                                            connectionStatus = connectionStatus,
                                            connected = connectionStatus == ConnectionStatus.ONLINE &&
                                                activeSession?.profileId == profile.profileId &&
                                                activeSession.characterId == profile.characterId,
                                            onSelect = {
                                                onSelectProfile(profile.profileId)
                                            },
                                        )
                                    }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                ListItem(
                                    headlineContent = {
                                        Text(stringResource(R.string.common_connect_server))
                                    },
                                    supportingContent = {
                                        Text(stringResource(R.string.me_connect_summary))
                                    },
                                    leadingContent = {
                                        Icon(Icons.Default.Link, contentDescription = null)
                                    },
                                    trailingContent = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                        )
                                    },
                                    modifier = Modifier.clickable(onClick = onOpenPairing),
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ),
                                    tonalElevation = 0.dp,
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.common_settings))
                            },
                            supportingContent = {
                                Text(stringResource(R.string.me_settings_summary))
                            },
                            leadingContent = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable(onClick = onOpenSettings),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            tonalElevation = 0.dp,
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.me_section_about))
                        },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable(onClick = onOpenAbout),
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        tonalElevation = 0.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionProfileRow(
    profile: ServerProfile,
    active: Boolean,
    connectionStatus: ConnectionStatus,
    connected: Boolean,
    onSelect: () -> Unit,
) {
    val connecting = active && connectionStatus == ConnectionStatus.CONNECTING
    val characterName = profile.characterName
    val title = when {
        characterName == null -> stringResource(R.string.settings_character_unnamed)
        profile.worldName != null -> "$characterName@${profile.worldName}"
        else -> characterName
    }
    val containerColor = when {
        connected -> MaterialTheme.colorScheme.primaryContainer
        connecting -> MaterialTheme.colorScheme.tertiaryContainer
        active -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        connected -> MaterialTheme.colorScheme.onPrimaryContainer
        connecting -> MaterialTheme.colorScheme.onTertiaryContainer
        active -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = when {
        connected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        connecting -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
        active -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text("${profile.host}:${profile.port}")
        },
        leadingContent = {
            RadioButton(
                selected = active,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = contentColor,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Badge(
                    containerColor = if (connected || connecting) {
                        contentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = stringResource(
                        when {
                            connected -> R.string.common_connected
                            connecting -> R.string.common_connecting
                            else -> R.string.common_not_connected
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (connected || connecting || active) {
                        contentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        modifier = Modifier.clickable(onClick = onSelect),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = contentColor,
            supportingColor = supportingColor,
        ),
        tonalElevation = 0.dp,
    )
}

@Preview(name = "Me connected", showBackground = true)
@Composable
private fun MeScreenPreview() {
    val profile = previewProfile()
    FreeChatTheme {
        MeScreen(
            profiles = listOf(profile),
            activeProfile = profile,
            activeSession = previewSession(profile),
            connectionStatus = ConnectionStatus.ONLINE,
            onOpenSettings = {},
            onOpenPairing = {},
            onOpenAbout = {},
            onSelectProfile = {},
        )
    }
}

@Preview(name = "Me disconnected", showBackground = true)
@Composable
private fun MeScreenDisconnectedPreview() {
    val profile = previewProfile()
    FreeChatTheme {
        MeScreen(
            profiles = listOf(profile),
            activeProfile = profile,
            activeSession = null,
            connectionStatus = ConnectionStatus.OFFLINE,
            onOpenSettings = {},
            onOpenPairing = {},
            onOpenAbout = {},
            onSelectProfile = {},
        )
    }
}

private fun previewProfile() = ServerProfile(
    serverId = "preview",
    host = "192.168.1.10",
    port = 14778,
    fingerprintHex = "00".repeat(32),
    serverName = "Preview",
    characterId = 18014469510158240,
    characterName = "Y'shtola",
    worldName = "Mateus",
)

private fun previewSession(profile: ServerProfile) = SessionInfo(
    serverId = profile.serverId,
    serverName = profile.serverName.orEmpty(),
    profileId = profile.profileId,
    characterId = profile.characterId,
    characterName = profile.characterName.orEmpty(),
    worldName = profile.worldName.orEmpty(),
    rawCharacterJson = "{}",
    sessionId = "preview-session",
    enabledFeatures = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var includeRaw by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.export(it, context.contentResolver, ExportFormat.JSON, includeRaw) }
    }
    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri?.let { viewModel.export(it, context.contentResolver, ExportFormat.TXT, includeRaw) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FreeChatSectionHeader(stringResource(R.string.settings_section_appearance), Modifier.padding(top = 0.dp)) }
                item {
                    AppearanceSettings(
                        settings = settings,
                        onThemeModeChange = { mode ->
                            viewModel.updateSettings({ it.copy(themeMode = mode) })
                        },
                        onLanguageChange = { language ->
                            viewModel.updateSettings({ it.copy(appLanguage = language) })
                            LocaleController.setLanguage(context, language)
                        },
                    )
                }

                item { FreeChatSectionHeader(stringResource(R.string.settings_section_server)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        SwitchRow(
                            title = "订阅战斗日志",
                            checked = settings.subscribeBattleLog,
                            onCheckedChange = { value ->
                                viewModel.updateSettings({ it.copy(subscribeBattleLog = value) }, reconnect = true)
                            },
                        )
                    }
                }

                item { FreeChatSectionHeader(stringResource(R.string.settings_section_data)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            FreeChatSettingRow(
                                icon = Icons.Default.Download,
                                title = stringResource(R.string.settings_export),
                                onClick = { showExportSheet = true },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FreeChatSettingRow(
                                icon = Icons.Default.DeleteSweep,
                                title = "重置/清空聊天记录",
                                onClick = { showClearDialog = true },
                            )
                        }
                    }
                }

                item { FreeChatSectionHeader(stringResource(R.string.settings_section_notifications)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            SwitchRow(
                                title = "私聊通知",
                                checked = settings.notifyPrivate,
                                onCheckedChange = { value ->
                                    viewModel.updateSettings({ it.copy(notifyPrivate = value) })
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SwitchRow(
                                title = "小队通知",
                                checked = settings.notifyParty,
                                onCheckedChange = { value ->
                                    viewModel.updateSettings({ it.copy(notifyParty = value) })
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SwitchRow(
                                title = "锁屏隐藏通知内容",
                                checked = settings.lockScreenHideContent,
                                onCheckedChange = { value ->
                                    viewModel.updateSettings({ it.copy(lockScreenHideContent = value) })
                                },
                            )
                        }
                    }
                }

                item { FreeChatSectionHeader(stringResource(R.string.settings_section_developer)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        FreeChatSettingRow(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.settings_developer_options),
                            description = stringResource(R.string.settings_developer_summary),
                            onClick = onOpenDeveloperOptions,
                        )
                    }
                }
            }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = exportSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_export_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExportOptionButton(
                        icon = Icons.Default.DataObject,
                        label = stringResource(R.string.settings_export_json),
                        onClick = {
                            showExportSheet = false
                            jsonLauncher.launch("freechat.json")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ExportOptionButton(
                        icon = Icons.Default.Description,
                        label = stringResource(R.string.settings_export_txt),
                        onClick = {
                            showExportSheet = false
                            txtLauncher.launch("freechat.txt")
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeRaw = !includeRaw }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includeRaw,
                        onCheckedChange = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_export_include_raw),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("清空聊天记录") },
            text = { Text("会删除本地全部消息与会话，无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllMessages()
                        showClearDialog = false
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun AppearanceSettings(
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(themeLabel(mode)) },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = settings.appLanguage == language,
                        onClick = { onLanguageChange(language) },
                        label = { Text(languageLabel(language)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)

@Composable
private fun languageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.SYSTEM -> R.string.settings_language_system
        AppLanguage.ENGLISH -> R.string.settings_language_en
        AppLanguage.CHINESE -> R.string.settings_language_zh
    },
)


@Composable
private fun ExportOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(104.dp),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
