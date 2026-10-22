package io.github.freechat.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.model.FriendInfo
import io.github.freechat.android.core.ui.components.FreeChatAvatar
import io.github.freechat.android.core.ui.components.FreeChatEmptyState
import kotlinx.coroutines.launch

private enum class FriendFilter {
    ALL,
    ONLINE,
    OFFLINE,
    UNKNOWN,
}

private enum class FriendSort {
    DEFAULT,
    NAME,
    WORLD,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onOpenPairing: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FriendFilter.ALL) }
    var sort by remember { mutableStateOf(FriendSort.DEFAULT) }
    var sortMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    val visible = state.friends
        .filter { friend ->
            (query.isBlank() || friend.name.contains(query, ignoreCase = true)) &&
                when (filter) {
                    FriendFilter.ALL -> true
                    FriendFilter.ONLINE -> friend.presence() == FriendFilter.ONLINE
                    FriendFilter.OFFLINE -> friend.presence() == FriendFilter.OFFLINE
                    FriendFilter.UNKNOWN -> friend.presence() == FriendFilter.UNKNOWN
                }
        }
        .let { friends ->
            when (sort) {
                FriendSort.DEFAULT -> friends
                FriendSort.NAME -> friends.sortedBy { it.name.lowercase() }
                FriendSort.WORLD -> friends.sortedBy {
                    (it.currentWorldName ?: it.homeWorldName).orEmpty().lowercase()
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("搜索好友") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == FriendFilter.ONLINE,
                    onClick = { filter = FriendFilter.ONLINE },
                    label = { Text("在线") },
                )
                FilterChip(
                    selected = filter == FriendFilter.OFFLINE,
                    onClick = { filter = FriendFilter.OFFLINE },
                    label = { Text("离线") },
                )
                FilterChip(
                    selected = filter == FriendFilter.UNKNOWN,
                    onClick = { filter = FriendFilter.UNKNOWN },
                    label = { Text("未知") },
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "排序")
                    }
                    DropdownMenu(
                        expanded = sortMenu,
                        onDismissRequest = { sortMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("默认") },
                            onClick = { sort = FriendSort.DEFAULT; sortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("按首字母") },
                            onClick = { sort = FriendSort.NAME; sortMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("按服务器") },
                            onClick = { sort = FriendSort.WORLD; sortMenu = false },
                        )
                    }
                }
                IconButton(onClick = { viewModel.load(force = true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    FreeChatEmptyState(
                        icon = Icons.Default.Group,
                        title = "无法加载好友",
                        message = state.error.orEmpty(),
                    ) {
                        androidx.compose.material3.Button(onClick = onOpenPairing) {
                            Text("连接服务器")
                        }
                    }
                }

                visible.isEmpty() -> {
                    FreeChatEmptyState(
                        icon = Icons.Default.Group,
                        title = "暂无好友",
                        message = if (state.friends.isEmpty()) {
                            "连接服务器后显示好友列表。"
                        } else {
                            "没有符合当前筛选条件的好友。"
                        },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visible, key = { it.contentId }) { friend ->
                            FriendCard(
                                friend = friend,
                                modifier = Modifier.animateItem(),
                            ) {
                                scope.launch {
                                    val id = viewModel.openTell(friend)
                                    if (id != null) onOpenConversation(id)
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun FriendCard(
    friend: FriendInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val world = friend.currentWorldName ?: friend.homeWorldName
    val details = listOfNotNull(
        friend.freeCompanyTag?.let { "《$it》" },
        friend.jobName,
        friend.territoryName,
    ).joinToString(" · ")
    val presence = friend.presence()
    val statusColor = when (presence) {
        FriendFilter.ONLINE -> MaterialTheme.colorScheme.primary
        FriendFilter.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FreeChatAvatar(label = friend.name, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (world != null) "${friend.name}@$world" else friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (details.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = statusColor,
                    content = {},
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when (presence) {
                        FriendFilter.ONLINE -> "在线"
                        FriendFilter.OFFLINE -> "离线"
                        else -> "未知"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                )
            }
        }
    }
}

private fun FriendInfo.presence(): FriendFilter = when {
    identityUnavailable -> FriendFilter.UNKNOWN
    statusBits != 0L -> FriendFilter.ONLINE
    else -> FriendFilter.OFFLINE
}
