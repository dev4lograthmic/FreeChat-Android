package io.github.freechat.android.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.data.export.ExportFormat
import io.github.freechat.android.core.model.ChatSourceOption
import io.github.freechat.android.core.model.MessageContentSource
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.ui.components.FreeChatAvatar
import io.github.freechat.android.core.ui.components.FreeChatCard
import io.github.freechat.android.core.ui.components.FreeChatSectionHeader
import io.github.freechat.android.core.ui.components.FreeChatSettingRow
import io.github.freechat.android.core.ui.components.FreeChatStatusPill
import io.github.freechat.android.core.ui.components.ScreenBackground

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
    val connected = isConnected

    ScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("我", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FreeChatAvatar(
                                label = characterName ?: "?",
                                size = 72.dp,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                FreeChatStatusPill(
                                    text = if (connected) "已连接到服务端" else "未连接",
                                    color = if (connected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    contentColor = if (connected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (characterName != null) {
                                        if (worldName != null) "$characterName@$worldName" else characterName
                                    } else {
                                        "未连接角色"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = activeProfile?.let { "${it.host}:${it.port}" }
                                        ?: "尚未配置服务器",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "在手机上继续 FFXIV 的聊天。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }
            item {
                FreeChatCard {
                    Column {
                        FreeChatSettingRow(
                            icon = Icons.Default.Link,
                            title = "连接服务器",
                            description = "扫码、深链或手动输入",
                            onClick = onOpenPairing,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FreeChatSettingRow(
                            icon = Icons.Default.Settings,
                            title = "设置",
                            description = "通知、数据、开发者选项",
                            onClick = onOpenSettings,
                        )
                    }
                }
            }
            item {
                Text(
                    "FreeChat Android · 非官方 FFXIV 客户端",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPairing: () -> Unit,
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
        ScreenBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FreeChatSectionHeader("连接", Modifier.padding(top = 0.dp)) }
                item {
                    FreeChatCard {
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
                                    selected = profile.serverId == activeProfile?.serverId,
                                    onSelect = { viewModel.setActive(profile.serverId) },
                                    onDelete = { viewModel.deleteServer(profile.serverId) },
                                )
                            }
                        }
                    }
                }

                item { FreeChatSectionHeader("数据") }
                item {
                    FreeChatCard {
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

                item { FreeChatSectionHeader("通知") }
                item {
                    FreeChatCard {
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

                item { FreeChatSectionHeader("开发者") }
                item {
                    Text(
                        "订阅来源（调试用，默认 POST_HANDLE）",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChatSourceOption.entries.forEach { source ->
                            FilterChip(
                                selected = source in settings.developerSources,
                                onClick = {
                                    viewModel.updateSettings(
                                        transform = {
                                            it.copy(
                                                developerSources = toggleSource(
                                                    it.developerSources,
                                                    source,
                                                ),
                                            )
                                        },
                                        reconnect = true,
                                    )
                                },
                                label = { Text(source.label()) },
                            )
                        }
                    }
                }
                item {
                    Text(
                        "同时选择 POST_HANDLE 与 STRUCTURED 可能收到重复事件。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                item {
                    Text(
                        "消息内容字段（调试用）",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MessageContentSource.entries.take(2).forEach { source ->
                                FilterChip(
                                    selected = settings.developerMessageContent == source,
                                    onClick = {
                                        viewModel.updateSettings(
                                            transform = {
                                                it.copy(developerMessageContent = source)
                                            },
                                            reconnect = true,
                                        )
                                    },
                                    label = { Text(source.label()) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MessageContentSource.entries.drop(2).forEach { source ->
                                FilterChip(
                                    selected = settings.developerMessageContent == source,
                                    onClick = {
                                        viewModel.updateSettings(
                                            transform = {
                                                it.copy(developerMessageContent = source)
                                            },
                                            reconnect = true,
                                        )
                                    },
                                    label = { Text(source.label()) },
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "raw 字段以 Base64 显示，仅用于调试。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                item {
                    FreeChatCard {
                        Column {
                            SwitchRow(
                                title = "允许 raw 命令",
                                checked = settings.developerRawCommands,
                                onCheckedChange = { value ->
                                    viewModel.updateSettings({ it.copy(developerRawCommands = value) })
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SwitchRow(
                                title = "超长消息拆分提示",
                                checked = settings.developerSplitToast,
                                onCheckedChange = { value ->
                                    viewModel.updateSettings({ it.copy(developerSplitToast = value) })
                                },
                            )
                        }
                    }
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
            Text(profile.serverName ?: profile.serverId, style = MaterialTheme.typography.titleMedium)
            Text(
                "${profile.host}:${profile.port}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除服务器")
        }
    }
}

private fun toggleSource(
    current: Set<ChatSourceOption>,
    source: ChatSourceOption,
): Set<ChatSourceOption> {
    val next = if (source in current) current - source else current + source
    return next.ifEmpty { setOf(ChatSourceOption.POST_HANDLE) }
}

private fun ChatSourceOption.label(): String = when (this) {
    ChatSourceOption.PRE_HANDLE -> "PRE_HANDLE"
    ChatSourceOption.POST_HANDLE -> "POST_HANDLE"
    ChatSourceOption.STRUCTURED -> "STRUCTURED"
}

private fun MessageContentSource.label(): String = when (this) {
    MessageContentSource.TEXT -> "TEXT"
    MessageContentSource.ORIGINAL_TEXT -> "ORIGINAL_TEXT"
    MessageContentSource.RAW_MESSAGE -> "RAW_MESSAGE"
    MessageContentSource.ORIGINAL_RAW_MESSAGE -> "ORIGINAL_RAW"
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
