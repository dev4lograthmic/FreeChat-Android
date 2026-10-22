package io.github.freechat.android.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.freechat.android.core.model.ServerProfile
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
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val activeChat = viewModel.activeChat
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val characterName = activeChat?.sessionInfo?.characterName
        ?.takeIf { it.isNotBlank() }
        ?: activeProfile?.characterName
    val worldName = activeChat?.sessionInfo?.worldName
        ?.takeIf { it.isNotBlank() }
        ?: activeProfile?.worldName

    MeScreen(
        characterName = characterName,
        worldName = worldName,
        serverAddress = activeProfile?.let { "${it.host}:${it.port}" },
        isConnected = isConnected,
        onOpenSettings = onOpenSettings,
        onOpenPairing = onOpenPairing,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeScreen(
    characterName: String?,
    worldName: String?,
    serverAddress: String?,
    isConnected: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPairing: () -> Unit,
) {
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
                                    Badge(
                                        containerColor = if (isConnected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(
                                            if (isConnected) {
                                                R.string.common_connected
                                            } else {
                                                R.string.common_not_connected
                                            },
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isConnected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
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
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.me_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                Text(
                    text = stringResource(R.string.me_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "Me connected", showBackground = true)
@Composable
private fun MeScreenPreview() {
    FreeChatTheme {
        MeScreen(
            characterName = "Y'shtola",
            worldName = "Mateus",
            serverAddress = "192.168.1.10:14700",
            isConnected = true,
            onOpenSettings = {},
            onOpenPairing = {},
        )
    }
}

@Preview(name = "Me disconnected", showBackground = true)
@Composable
private fun MeScreenDisconnectedPreview() {
    FreeChatTheme {
        MeScreen(
            characterName = null,
            worldName = null,
            serverAddress = null,
            isConnected = false,
            onOpenSettings = {},
            onOpenPairing = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var includeRaw by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

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

                item { FreeChatSectionHeader(stringResource(R.string.settings_section_connection), Modifier.padding(top = 0.dp)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            FreeChatSettingRow(
                                icon = Icons.Default.Link,
                                title = "连接服务器",
                                description = "扫码、深链或手动输入",
                                onClick = onOpenPairing,
                            )
                            profiles.forEach { profile ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                ServerRow(
                                    profile = profile,
                                    selected = profile.profileId == activeProfile?.profileId,
                                    onSelect = { viewModel.selectProfile(profile.profileId) },
                                    onDelete = { viewModel.deleteProfile(profile.profileId) },
                                )
                            }
                        }
                    }
                }

                item { FreeChatSectionHeader(stringResource(R.string.settings_section_data)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column {
                            FreeChatSettingRow(
                                icon = Icons.Default.Download,
                                title = "导出 JSON",
                                onClick = { jsonLauncher.launch("freechat.json") },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FreeChatSettingRow(
                                icon = Icons.Default.Download,
                                title = "导出 TXT",
                                onClick = { txtLauncher.launch("freechat.txt") },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SwitchRow(
                                title = "导出包含原始数据",
                                checked = includeRaw,
                                onCheckedChange = { includeRaw = it },
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
                                title = "订阅战斗日志",
                                checked = settings.subscribeBattleLog,
                                onCheckedChange = { value ->
                                    viewModel.updateSettings({ it.copy(subscribeBattleLog = value) }, reconnect = true)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        AppLanguage.ENGLISH -> R.string.settings_language_en
        AppLanguage.CHINESE -> R.string.settings_language_zh
    },
)

@Composable
private fun ServerRow(
    profile: ServerProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.characterName
                    ?: profile.serverName
                    ?: stringResource(R.string.settings_character_unnamed),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOfNotNull(
                    profile.worldName,
                    "${profile.host}:${profile.port}",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.common_delete),
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
