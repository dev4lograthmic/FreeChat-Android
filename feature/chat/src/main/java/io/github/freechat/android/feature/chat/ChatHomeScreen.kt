package io.github.freechat.android.feature.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.data.conversation.ConversationLabels
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.ui.components.FreeChatAvatar
import io.github.freechat.android.core.ui.components.FreeChatCard
import io.github.freechat.android.core.ui.components.FreeChatEmptyState
import io.github.freechat.android.core.ui.components.FreeChatSearchBar
import io.github.freechat.android.core.ui.components.FreeChatSectionHeader
import io.github.freechat.android.core.ui.components.ScreenBackground
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    onOpenDrawer: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: ChatHomeViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val collapsedSections by viewModel.collapsedSections.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FreeChatSearchBar(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.search(it)
                    },
                    placeholder = "搜索频道、角色或消息",
                    modifier = Modifier.padding(top = 8.dp),
                    onMenuClick = onOpenDrawer,
                )
            }

            if (query.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    item {
                        FreeChatEmptyState(
                            icon = Icons.Default.Chat,
                            title = "没有找到消息",
                            message = "试试其他关键词，或检查是否已连接服务器。",
                        )
                    }
                } else {
                    items(searchResults, key = { it.messageId }) { message ->
                        SearchResultCard(message) {
                            query = ""
                            viewModel.unhideConversation(message.conversationId)
                            onOpenConversation(message.conversationId)
                        }
                    }
                }
            } else {
                val system = conversations
                    .filter { it.type == ConversationType.SYSTEM }
                    .sortedBy { ConversationLabels.systemOrder(it.channel.orEmpty()) }
                val public = conversations
                    .filter { it.type == ConversationType.PUBLIC_CHANNEL }
                    .sortedBy { ConversationLabels.publicOrder(it.channel.orEmpty()) }
                val private = conversations
                    .filter { it.type == ConversationType.PRIVATE }
                    .sortedByDescending { it.lastMessageAt }

                if (conversations.isEmpty()) {
                    item {
                        if (isConnected) {
                            FreeChatEmptyState(
                                icon = Icons.Default.Chat,
                                title = "已连接，等待消息",
                                message = "收到新消息后会显示在这里。",
                            )
                        } else {
                            FreeChatEmptyState(
                                icon = Icons.Default.Chat,
                                title = "还没有连接服务器",
                                message = "连接 Dalamud 服务端后，系统通知、公共频道和私聊会出现在这里。",
                            ) {
                                Button(onClick = onOpenPairing) {
                                    Text("连接服务器")
                                }
                            }
                        }
                    }
                } else {
                    if (system.isNotEmpty()) {
                        item {
                            FreeChatSectionHeader(
                                text = "系统通知",
                                expanded = ConversationType.SYSTEM !in collapsedSections,
                                onClick = { viewModel.toggleSection(ConversationType.SYSTEM) },
                            )
                        }
                        if (ConversationType.SYSTEM !in collapsedSections) {
                            items(system, key = { it.id }) { conversation ->
                                ConversationCard(
                                    conversation = conversation,
                                    onOpenConversation = onOpenConversation,
                                    onMarkRead = { viewModel.markRead(conversation.id) },
                                )
                            }
                        }
                    }
                    if (public.isNotEmpty()) {
                        item {
                            FreeChatSectionHeader(
                                text = "公共频道",
                                expanded = ConversationType.PUBLIC_CHANNEL !in collapsedSections,
                                onClick = { viewModel.toggleSection(ConversationType.PUBLIC_CHANNEL) },
                            )
                        }
                        if (ConversationType.PUBLIC_CHANNEL !in collapsedSections) {
                            items(public, key = { it.id }) { conversation ->
                                ConversationCard(
                                    conversation = conversation,
                                    onOpenConversation = onOpenConversation,
                                    onMarkRead = { viewModel.markRead(conversation.id) },
                                )
                            }
                        }
                    }
                    if (private.isNotEmpty()) {
                        item {
                            FreeChatSectionHeader(
                                text = "私聊",
                                expanded = ConversationType.PRIVATE !in collapsedSections,
                                onClick = { viewModel.toggleSection(ConversationType.PRIVATE) },
                            )
                        }
                        if (ConversationType.PRIVATE !in collapsedSections) {
                            items(private, key = { it.id }) { conversation ->
                                SwipeRevealRow(
                                    onDelete = { viewModel.hideConversation(conversation.id) },
                                ) {
                                    ConversationCard(
                                        conversation = conversation,
                                        onOpenConversation = onOpenConversation,
                                        onMarkRead = { viewModel.markRead(conversation.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(Icons.Default.Add, contentDescription = "创建私聊")
        }
    }

    if (showCreateDialog) {
        CreatePrivateChatDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, world ->
                scope.launch {
                    val id = viewModel.createPrivateChat(name, world)
                    showCreateDialog = false
                    if (id != null) onOpenConversation(id)
                }
            },
        )
    }
}

@Composable
private fun ConversationCard(
    conversation: ChatConversation,
    onOpenConversation: (String) -> Unit,
    onMarkRead: () -> Unit,
) {
    Surface(
        onClick = { onOpenConversation(conversation.id) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FreeChatAvatar(
                label = conversation.displayName.ifBlank { conversation.channel.orEmpty() },
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    conversation.displayName.ifBlank { conversation.channel.orEmpty() },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    conversation.lastMessageText.ifBlank { "暂无消息" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTime(conversation.lastMessageAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (conversation.unreadCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    DraggableUnreadBadge(
                        count = conversation.unreadCount,
                        onClear = onMarkRead,
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableUnreadBadge(count: Int, onClear: () -> Unit) {
    var offset by remember(count) { mutableStateOf(Offset.Zero) }
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }

    Badge(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(count) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    },
                    onDragEnd = {
                        if (offset.getDistance() >= threshold) onClear()
                        offset = Offset.Zero
                    },
                    onDragCancel = { offset = Offset.Zero },
                )
            },
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    ) {
        Text(count.toString())
    }
}

@Composable
private fun SwipeRevealRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val revealWidth = with(LocalDensity.current) { 72.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.CenterStart,
        ) {
            IconButton(
                onClick = {
                    onDelete()
                    scope.launch { offset.snapTo(0f) }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "从列表隐藏",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(0f, revealWidth))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                offset.animateTo(
                                    if (offset.value > revealWidth / 2f) revealWidth else 0f,
                                )
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    )
                },
        ) {
            content()
        }
    }
}

@Composable
private fun SearchResultCard(message: ChatMessage, onClick: () -> Unit) {
    FreeChatCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FreeChatAvatar(label = message.senderName, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message.senderName, style = MaterialTheme.typography.titleMedium)
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                message.channel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CreatePrivateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var world by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("创建私聊", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色名") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = world,
                    onValueChange = { world = it },
                    label = { Text("所在服务器") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, world) },
                enabled = name.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd")

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val instant = Instant.ofEpochMilli(timestamp)
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone).toLocalDate()
    return if (date == LocalDate.now()) {
        TIME_FORMAT.format(instant.atZone(zone))
    } else {
        DATE_FORMAT.format(instant.atZone(zone))
    }
}
