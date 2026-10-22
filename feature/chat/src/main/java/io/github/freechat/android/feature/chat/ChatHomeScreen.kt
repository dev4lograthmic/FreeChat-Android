package io.github.freechat.android.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.data.conversation.ConversationLabels
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.ui.R
import io.github.freechat.android.core.ui.components.FreeChatAvatar
import io.github.freechat.android.core.ui.components.FreeChatEmptyState
import io.github.freechat.android.core.ui.components.FreeChatSectionHeader
import io.github.freechat.android.core.ui.components.chatChannelLabel
import io.github.freechat.android.core.ui.theme.FreeChatTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ChatRoute(
    onOpenPairing: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: ChatHomeViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val collapsedSections by viewModel.collapsedSections.collectAsStateWithLifecycle()

    ChatScreen(
        conversations = conversations,
        searchResults = searchResults,
        isConnected = isConnected,
        collapsedSections = collapsedSections,
        onSearch = viewModel::search,
        onToggleSection = viewModel::toggleSection,
        onOpenConversation = { conversationId ->
            viewModel.unhideConversation(conversationId)
            viewModel.markRead(conversationId)
            onOpenConversation(conversationId)
        },
        onHideConversation = viewModel::hideConversation,
        onUnhideConversation = viewModel::unhideConversation,
        onCreatePrivateChat = viewModel::createPrivateChat,
        onOpenPairing = onOpenPairing,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversations: List<ChatConversation>,
    searchResults: List<ChatMessage>,
    isConnected: Boolean,
    collapsedSections: Set<ConversationType>,
    onSearch: (String) -> Unit,
    onToggleSection: (ConversationType) -> Unit,
    onOpenConversation: (String) -> Unit,
    onHideConversation: (String) -> Unit,
    onUnhideConversation: (String) -> Unit,
    onCreatePrivateChat: suspend (String, String) -> String?,
    onOpenPairing: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val hiddenMessage = stringResource(R.string.chat_hidden)
    val undoLabel = stringResource(R.string.common_undo)
    val hiddenIds = remember { mutableListOf<String>() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    val deleteWithUndo: (String) -> Unit = { id ->
        onHideConversation(id)
        if (id !in hiddenIds) hiddenIds.add(id)
        // Replace the current snackbar so deleting several rows keeps one
        // undo affordance instead of queueing stale snackbars.
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            val result = withTimeoutOrNull(3_000) {
                snackbarHostState.showSnackbar(
                    message = hiddenMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == SnackbarResult.ActionPerformed) {
                hiddenIds.toList().forEach(onUnhideConversation)
            }
            hiddenIds.clear()
        }
    }

    val system = conversations
        .filter { it.type == ConversationType.SYSTEM }
        .sortedBy { ConversationLabels.systemOrder(it.channel.orEmpty()) }
    val public = conversations
        .filter {
            it.type == ConversationType.PUBLIC_CHANNEL &&
                !ConversationLabels.isLinkshell(it.channel)
        }
        .sortedBy { ConversationLabels.publicOrder(it.channel.orEmpty()) }
    val linkshell = conversations
        // Group by channel so rows stored before linkshells had their own type
        // still move to the right section. Slots without a game-provided name
        // are intentionally kept out of the chat list.
        .filter {
            ConversationLabels.isLinkshell(it.channel) &&
                ConversationLabels.linkshellName(it.channel, it.displayName) != null
        }
        .sortedBy { ConversationLabels.linkshellOrder(it.channel.orEmpty()) }
    val private = conversations
        // Opening a friend creates an empty placeholder so the composer can
        // work before the first message exists. Only show it once it has one.
        .filter { it.type == ConversationType.PRIVATE && it.lastMessageText.isNotBlank() }
        .sortedByDescending { it.lastMessageAt }
    val systemUnread = system.sumOf { it.unreadCount }
    val publicUnread = public.sumOf { it.unreadCount }
    val linkshellUnread = linkshell.sumOf { it.unreadCount }
    val privateUnread = private.sumOf { it.unreadCount }
    val visibleConversations = system + public + linkshell + private

    val searchableConversations = visibleConversations.map { conversation ->
        conversation to chatChannelLabel(
            type = conversation.type,
            channel = conversation.channel,
            fallback = conversation.displayName.ifBlank { conversation.channel.orEmpty() },
        )
    }
    val conversationResults = if (query.isBlank()) {
        emptyList()
    } else {
        searchableConversations
            .filter { (conversation, label) ->
                conversation.displayName.contains(query, ignoreCase = true) ||
                    conversation.peerName.orEmpty().contains(query, ignoreCase = true) ||
                    conversation.channel.orEmpty().contains(query, ignoreCase = true) ||
                    label.contains(query, ignoreCase = true)
            }
            .map { it.first }
    }
    val namedLinkshellChannels = linkshell.mapNotNull { it.channel }.toSet()
    val visibleSearchResults = searchResults.filter { message ->
        !ConversationLabels.isLinkshell(message.channel) ||
            message.channel in namedLinkshellChannels
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chat_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = { ConnectionStatusPill(isConnected = isConnected) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.chat_create_private),
                    )
                }
            }
        },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(bottom = 112.dp),
        ) {
            item {
                ChatSearchField(
                    query = query,
                    onQueryChange = {
                        query = it
                        onSearch(it)
                    },
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp,
                    ),
                )
            }


            if (query.isNotBlank()) {
                if (conversationResults.isEmpty() && visibleSearchResults.isEmpty()) {
                    item {
                        FreeChatEmptyState(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = stringResource(R.string.chat_search_empty_title),
                            message = stringResource(R.string.chat_search_empty_message),
                        )
                    }
                } else {
                    if (conversationResults.isNotEmpty()) {
                        item {
                            FreeChatSectionHeader(
                                text = stringResource(R.string.chat_search_conversations),
                            )
                        }
                        items(
                            conversationResults,
                            key = { "conversation-${it.id}" },
                        ) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    query = ""
                                    onSearch("")
                                    onOpenConversation(conversation.id)
                                },
                            )
                        }
                    }
                    if (visibleSearchResults.isNotEmpty()) {
                        item {
                            FreeChatSectionHeader(
                                text = stringResource(R.string.chat_search_messages),
                            )
                        }
                        items(
                            visibleSearchResults,
                            key = { "message-${it.messageId}" },
                        ) { message ->
                            SearchResultRow(
                                message = message,
                                modifier = Modifier.animateItem(),
                            ) {
                                query = ""
                                onSearch("")
                                onOpenConversation(message.conversationId)
                            }
                        }
                    }
                }
            } else if (visibleConversations.isEmpty()) {
                item {
                    if (isConnected) {
                        FreeChatEmptyState(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = stringResource(R.string.chat_waiting_title),
                            message = stringResource(R.string.chat_waiting_message),
                        )
                    } else {
                        FreeChatEmptyState(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = stringResource(R.string.chat_empty_title),
                            message = stringResource(R.string.chat_empty_message),
                        ) {
                            Button(onClick = onOpenPairing) {
                                Text(stringResource(R.string.common_connect_server))
                            }
                        }
                    }
                }
            } else {
                if (system.isNotEmpty()) {
                    item {
                        FreeChatSectionHeader(
                            modifier = Modifier.animateItem(),
                            text = stringResource(R.string.chat_section_system),
                            expanded = ConversationType.SYSTEM !in collapsedSections,
                            unreadCount = systemUnread,
                            onClick = { onToggleSection(ConversationType.SYSTEM) },
                        )
                    }
                    if (ConversationType.SYSTEM !in collapsedSections) {
                        items(system, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenConversation(conversation.id) },
                            )
                        }
                    }
                }

                if (public.isNotEmpty()) {
                    item {
                        FreeChatSectionHeader(
                            modifier = Modifier.animateItem(),
                            text = stringResource(R.string.chat_section_public),
                            expanded = ConversationType.PUBLIC_CHANNEL !in collapsedSections,
                            unreadCount = publicUnread,
                            onClick = { onToggleSection(ConversationType.PUBLIC_CHANNEL) },
                        )
                    }
                    if (ConversationType.PUBLIC_CHANNEL !in collapsedSections) {
                        items(public, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenConversation(conversation.id) },
                            )
                        }
                    }
                }

                if (linkshell.isNotEmpty()) {
                    item {
                        FreeChatSectionHeader(
                            modifier = Modifier.animateItem(),
                            text = stringResource(R.string.chat_section_linkshell),
                            expanded = ConversationType.LINKSHELL !in collapsedSections,
                            unreadCount = linkshellUnread,
                            onClick = { onToggleSection(ConversationType.LINKSHELL) },
                        )
                    }
                    if (ConversationType.LINKSHELL !in collapsedSections) {
                        items(linkshell, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenConversation(conversation.id) },
                            )
                        }
                    }
                }

                if (private.isNotEmpty()) {
                    item {
                        FreeChatSectionHeader(
                            modifier = Modifier.animateItem(),
                            text = stringResource(R.string.chat_section_private),
                            expanded = ConversationType.PRIVATE !in collapsedSections,
                            unreadCount = privateUnread,
                            onClick = { onToggleSection(ConversationType.PRIVATE) },
                        )
                    }
                    if (ConversationType.PRIVATE !in collapsedSections) {
                        items(private, key = { it.id }) { conversation ->
                            SwipeToDeleteRow(
                                conversation = conversation,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenConversation(conversation.id) },
                                onDelete = { deleteWithUndo(conversation.id) },
                            )
                        }
                    }
                }
            }
        }

    }

    if (showCreateSheet) {
        CreatePrivateChatSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, world ->
                scope.launch {
                    val id = onCreatePrivateChat(name, world)
                    showCreateSheet = false
                    if (id != null) onOpenConversation(id)
                }
            },
        )
    }
}


@Composable
private fun ConnectionStatusPill(isConnected: Boolean) {
    val containerColor by animateColorAsState(
        targetValue = if (isConnected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "connectionContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isConnected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "connectionContent",
    )
    Surface(
        modifier = Modifier.padding(end = 12.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = stringResource(
                if (isConnected) R.string.common_connected else R.string.common_not_connected,
            ),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.chat_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                } else {
                    null
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = modifier.fillMaxWidth(),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {}
}

@Composable
private fun ConversationRow(
    conversation: ChatConversation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = chatChannelLabel(
        type = conversation.type,
        channel = conversation.channel,
        fallback = conversation.displayName.ifBlank { conversation.channel.orEmpty() },
    )
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = conversation.lastMessageText.ifBlank {
                    stringResource(R.string.chat_no_preview)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            BadgedBox(
                badge = {
                    AnimatedVisibility(
                        visible = conversation.unreadCount > 0,
                        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            AnimatedContent(
                                targetState = conversation.unreadCount,
                                transitionSpec = {
                                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                                },
                                label = "unreadCount",
                            ) { count ->
                                Text(if (count > 99) "99+" else count.toString())
                            }
                        }
                    }
                },
            ) {
                FreeChatAvatar(label = label)
            }
        },
        trailingContent = {
            Text(
                text = formatTime(conversation.lastMessageAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        tonalElevation = 0.dp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    conversation: ChatConversation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            // Always snap back. The row disappears through the data update;
            // keeping the dismissed state would leave a restored row stuck in
            // the EndToStart anchor and make it impossible to swipe again.
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.chat_hide_conversation),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        ConversationRow(conversation = conversation, onClick = onClick)
    }
}

@Composable
private fun SearchResultRow(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        overlineContent = {
            Text(
                text = chatChannelLabel(message.channel, message.channel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        supportingContent = {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { FreeChatAvatar(message.senderName) },
        trailingContent = {
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        tonalElevation = 0.dp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePrivateChatSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var world by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_create_private_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.chat_create_private_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = world,
                onValueChange = { world = it },
                label = { Text(stringResource(R.string.chat_create_private_world)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onCreate(name, world) },
                    enabled = name.isNotBlank(),
                ) {
                    Text(stringResource(R.string.chat_create_private_action))
                }
            }
        }
    }
}

@Composable
private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val instant = Instant.ofEpochMilli(timestamp)
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone).toLocalDate()
    return when {
        date == LocalDate.now() -> {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale)
                .format(instant.atZone(zone))
        }

        date == LocalDate.now().minusDays(1) -> {
            context.getString(R.string.chat_time_yesterday)
        }

        else -> {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(locale)
                .format(date)
        }
    }
}

@Preview(name="formatTime")
@Composable
private fun FormatTimePreview() {

}

@Preview(name = "Chat list", showBackground = true)
@Composable
private fun ChatScreenPreview() {
    FreeChatTheme {
        ChatScreen(
            conversations = previewConversations(),
            searchResults = emptyList(),
            isConnected = true,
            collapsedSections = emptySet(),
            onSearch = {},
            onToggleSection = {},
            onOpenConversation = {},
            onHideConversation = {},
            onUnhideConversation = {},
            onCreatePrivateChat = { _, _ -> null },
            onOpenPairing = {},
        )
    }
}

@Preview(name = "Chat empty", showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    FreeChatTheme {
        ChatScreen(
            conversations = emptyList(),
            searchResults = emptyList(),
            isConnected = false,
            collapsedSections = emptySet(),
            onSearch = {},
            onToggleSection = {},
            onOpenConversation = {},
            onHideConversation = {},
            onUnhideConversation = {},
            onCreatePrivateChat = { _, _ -> null },
            onOpenPairing = {},
        )
    }
}

@Preview(name = "Chat collapsed", showBackground = true)
@Composable
private fun ChatScreenCollapsedPreview() {
    FreeChatTheme {
        ChatScreen(
            conversations = previewConversations(),
            searchResults = emptyList(),
            isConnected = true,
            collapsedSections = ConversationType.entries.toSet(),
            onSearch = {},
            onToggleSection = {},
            onOpenConversation = {},
            onHideConversation = {},
            onUnhideConversation = {},
            onCreatePrivateChat = { _, _ -> null },
            onOpenPairing = {},
        )
    }
}

private fun previewConversations(): List<ChatConversation> {
    val now = System.currentTimeMillis()
    return listOf(
        ChatConversation(
            id = "preview-system",
            profileId = "preview:1",
            serverId = "preview",
            type = ConversationType.SYSTEM,
            channel = "SYSTEM",
            peerName = null,
            peerWorld = null,
            displayName = "System",
            lastMessageText = "Connected to FreeChat server",
            lastMessageAt = now - 60_000,
            unreadCount = 0,
        ),
        ChatConversation(
            id = "preview-say",
            profileId = "preview:1",
            serverId = "preview",
            type = ConversationType.PUBLIC_CHANNEL,
            channel = "SAY",
            peerName = null,
            peerWorld = null,
            displayName = "Say",
            lastMessageText = "Y'shtola: The realm is safe.",
            lastMessageAt = now - 120_000,
            unreadCount = 3,
        ),
        ChatConversation(
            id = "preview-party",
            profileId = "preview:1",
            serverId = "preview",
            type = ConversationType.PUBLIC_CHANNEL,
            channel = "PARTY",
            peerName = null,
            peerWorld = null,
            displayName = "Party",
            lastMessageText = "Alphinaud: Ready when you are.",
            lastMessageAt = now - 600_000,
            unreadCount = 1,
        ),
        ChatConversation(
            id = "preview-linkshell",
            profileId = "preview:1",
            serverId = "preview",
            type = ConversationType.LINKSHELL,
            channel = "LINKSHELL_1",
            peerName = null,
            peerWorld = null,
            displayName = "Crystal Guild",
            lastMessageText = "Y'shtola: Gathering at the aetheryte.",
            lastMessageAt = now - 240_000,
            unreadCount = 1,
        ),
        ChatConversation(
            id = "preview-tell",
            profileId = "preview:1",
            serverId = "preview",
            type = ConversationType.PRIVATE,
            channel = "TELL",
            peerName = "Y'shtola",
            peerWorld = "Mateus",
            displayName = "Y'shtola@Mateus",
            lastMessageText = "Let us speak privately.",
            lastMessageAt = now - 300_000,
            unreadCount = 2,
        ),
    )
}
