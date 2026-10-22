package io.github.freechat.android.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.model.ConnectionStatus
import io.github.freechat.android.core.model.FriendInfo
import io.github.freechat.android.core.ui.R
import io.github.freechat.android.core.ui.components.ConnectionStatusPill
import io.github.freechat.android.core.ui.components.FreeChatAvatar
import io.github.freechat.android.core.ui.components.FreeChatEmptyState
import io.github.freechat.android.core.ui.theme.FreeChatTheme
import kotlinx.coroutines.launch

private enum class FriendFilter {
    ALL,
    ONLINE,
    OFFLINE,
}

private enum class FriendPresence {
    ONLINE,
    OFFLINE,
}

private enum class FriendSort {
    DEFAULT,
    NAME,
    WORLD,
}

@Composable
fun FriendsRoute(
    onOpenPairing: () -> Unit,
    onOpenConversation: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    FriendsScreen(
        state = state,
        connectionStatus = connectionStatus,
        onRefresh = { viewModel.load(force = true) },
        onOpenPairing = onOpenPairing,
        onOpenFriend = { friend ->
            scope.launch {
                viewModel.openTell(friend)?.let(onOpenConversation)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    state: FriendsUiState,
    connectionStatus: ConnectionStatus,
    onRefresh: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenFriend: (FriendInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FriendFilter.ALL) }
    var sort by remember { mutableStateOf(FriendSort.DEFAULT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val visible = state.friends
        .filter { friend ->
            (query.isBlank() || friend.name.contains(query, ignoreCase = true)) &&
                when (filter) {
                    FriendFilter.ALL -> true
                    FriendFilter.ONLINE -> friend.presence() == FriendPresence.ONLINE
                    FriendFilter.OFFLINE -> friend.presence() == FriendPresence.OFFLINE
                }
        }
        .let { friends ->
            when (sort) {
                FriendSort.DEFAULT -> friends
                FriendSort.NAME -> friends.sortedBy { it.name.lowercase() }
                FriendSort.WORLD -> friends.sortedBy {
                    it.homeWorldName.orEmpty().lowercase()
                }
            }
        }

    val error = state.error
    val errorMessage = when (error) {
        FriendsError.NOT_CONNECTED -> stringResource(R.string.friends_not_connected_message)
        FriendsError.CONNECTING -> stringResource(R.string.friends_connecting_message)
        FriendsError.DISCONNECTED -> stringResource(R.string.friends_disconnected_message)
        FriendsError.LOAD_FAILED -> stringResource(R.string.friends_error_message)
        null -> null
    }

    LaunchedEffect(error, state.friends.isEmpty()) {
        if (error != null && state.friends.isNotEmpty()) {
            snackbarHostState.showSnackbar(errorMessage.orEmpty())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.friends_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    ConnectionStatusPill(
                        status = connectionStatus,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isLoading,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.friends_refresh),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                FriendsSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp,
                    ),
                )
            }

            if (state.friends.isNotEmpty()) {
                item {
                    FriendsFilterRow(
                        filter = filter,
                        onFilterChange = { filter = it },
                        sort = sort,
                        onSortChange = { sort = it },
                        friendCount = visible.size,
                    )
                }
            }

            when {
                error != null && state.friends.isEmpty() -> {
                    item {
                        FriendsErrorState(
                            error = error,
                            onRefresh = onRefresh,
                            onOpenPairing = onOpenPairing,
                        )
                    }
                }

                !state.loaded || (state.isLoading && state.friends.isEmpty()) -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                visible.isEmpty() -> {
                    item {
                        val noMatch = state.friends.isNotEmpty()
                        FreeChatEmptyState(
                            icon = Icons.Default.Group,
                            title = stringResource(
                                if (noMatch) {
                                    R.string.friends_no_match_title
                                } else {
                                    R.string.friends_empty_title
                                },
                            ),
                            message = stringResource(
                                if (noMatch) {
                                    R.string.friends_no_match_message
                                } else {
                                    R.string.friends_empty_message
                                },
                            ),
                        )
                    }
                }

                else -> {
                    items(visible, key = { it.contentId }) { friend ->
                        FriendRow(
                            friend = friend,
                            modifier = Modifier.animateItem(),
                            onClick = { onOpenFriend(friend) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsErrorState(
    error: FriendsError,
    onRefresh: () -> Unit,
    onOpenPairing: () -> Unit,
) {
    val title = when (error) {
        FriendsError.NOT_CONNECTED -> R.string.friends_not_connected_title
        FriendsError.CONNECTING -> R.string.friends_connecting_title
        FriendsError.DISCONNECTED -> R.string.friends_disconnected_title
        FriendsError.LOAD_FAILED -> R.string.friends_error_title
    }
    val message = when (error) {
        FriendsError.NOT_CONNECTED -> R.string.friends_not_connected_message
        FriendsError.CONNECTING -> R.string.friends_connecting_message
        FriendsError.DISCONNECTED -> R.string.friends_disconnected_message
        FriendsError.LOAD_FAILED -> R.string.friends_error_message
    }

    FreeChatEmptyState(
        icon = Icons.Default.Group,
        title = stringResource(title),
        message = stringResource(message),
    ) {
        if (error != FriendsError.CONNECTING) {
            Button(
                onClick = if (error == FriendsError.LOAD_FAILED) onRefresh else onOpenPairing,
            ) {
                Text(
                    stringResource(
                        if (error == FriendsError.LOAD_FAILED) {
                            R.string.common_retry
                        } else {
                            R.string.common_connect_server
                        },
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsSearchField(
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
                placeholder = { Text(stringResource(R.string.friends_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.friends_clear_search),
                            )
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
private fun FriendsFilterRow(
    filter: FriendFilter,
    onFilterChange: (FriendFilter) -> Unit,
    sort: FriendSort,
    onSortChange: (FriendSort) -> Unit,
    friendCount: Int,
) {
    var sortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FriendFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { onFilterChange(option) },
                    label = { Text(stringResource(option.labelRes())) },
                )
            }
        }

        Text(
            text = friendCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        Box {
            IconButton(onClick = { sortMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.friends_sort),
                )
            }
            DropdownMenu(
                expanded = sortMenu,
                onDismissRequest = { sortMenu = false },
            ) {
                FriendSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes())) },
                        onClick = {
                            onSortChange(option)
                            sortMenu = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: FriendInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val world = friend.homeWorldName
        ?: stringResource(R.string.friends_unknown_world)

    ListItem(
        headlineContent = {
            Text(
                text = friend.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = world,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            FreeChatAvatar(label = friend.name)
        },
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        tonalElevation = 0.dp,
    )
}

private fun FriendInfo.presence(): FriendPresence =
    if (statusBits != 0L) FriendPresence.ONLINE else FriendPresence.OFFLINE

private fun FriendFilter.labelRes(): Int = when (this) {
    FriendFilter.ALL -> R.string.friends_filter_all
    FriendFilter.ONLINE -> R.string.friends_filter_online
    FriendFilter.OFFLINE -> R.string.friends_filter_offline
}

private fun FriendSort.labelRes(): Int = when (this) {
    FriendSort.DEFAULT -> R.string.friends_sort_default
    FriendSort.NAME -> R.string.friends_sort_name
    FriendSort.WORLD -> R.string.friends_sort_world
}

@Preview(name = "Friends list", showBackground = true)
@Composable
private fun FriendsScreenPreview() {
    FreeChatTheme {
        FriendsScreen(
            state = FriendsUiState(
                friends = previewFriends(),
                loaded = true,
            ),
            connectionStatus = ConnectionStatus.ONLINE,
            onRefresh = {},
            onOpenPairing = {},
            onOpenFriend = {},
        )
    }
}

@Preview(name = "Friends empty", showBackground = true)
@Composable
private fun FriendsScreenEmptyPreview() {
    FreeChatTheme {
        FriendsScreen(
            state = FriendsUiState(loaded = true),
            connectionStatus = ConnectionStatus.OFFLINE,
            onRefresh = {},
            onOpenPairing = {},
            onOpenFriend = {},
        )
    }
}

@Preview(name = "Friends error", showBackground = true)
@Composable
private fun FriendsScreenErrorPreview() {
    FreeChatTheme {
        FriendsScreen(
            state = FriendsUiState(
                error = FriendsError.NOT_CONNECTED,
                loaded = true,
            ),
            connectionStatus = ConnectionStatus.OFFLINE,
            onRefresh = {},
            onOpenPairing = {},
            onOpenFriend = {},
        )
    }
}

private fun previewFriends(): List<FriendInfo> = listOf(
    FriendInfo(
        contentId = 1,
        name = "Y'shtola",
        homeWorldId = 1,
        homeWorldName = "Mateus",
        currentWorldId = 1,
        currentWorldName = "Mateus",
        homeDataCenterId = 1,
        homeDataCenterName = "Crystal",
        currentDataCenterId = 1,
        currentDataCenterName = "Crystal",
        freeCompanyTag = "SCIONS",
        jobName = "White Mage",
        territoryName = "Limsa Lominsa",
        statusBits = 1L,
        identityUnavailable = false,
    ),
    FriendInfo(
        contentId = 2,
        name = "Alphinaud",
        homeWorldId = 1,
        homeWorldName = "Mateus",
        currentWorldId = 2,
        currentWorldName = "Adamantoise",
        homeDataCenterId = 1,
        homeDataCenterName = "Crystal",
        currentDataCenterId = 2,
        currentDataCenterName = "Aether",
        freeCompanyTag = "SCIONS",
        jobName = "Arcanist",
        territoryName = "Gridania",
        statusBits = 1L,
        identityUnavailable = false,
    ),
    FriendInfo(
        contentId = 3,
        name = "Thancred",
        homeWorldId = 1,
        homeWorldName = "Mateus",
        currentWorldId = 0,
        currentWorldName = null,
        homeDataCenterId = 1,
        homeDataCenterName = "Crystal",
        currentDataCenterId = 0,
        currentDataCenterName = null,
        freeCompanyTag = null,
        jobName = "Gunbreaker",
        territoryName = "Ul'dah",
        statusBits = 0L,
        identityUnavailable = false,
    ),
)
