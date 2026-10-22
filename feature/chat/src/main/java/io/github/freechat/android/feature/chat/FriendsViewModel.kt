package io.github.freechat.android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.conversation.ConversationIds
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.model.FriendInfo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val isLoading: Boolean = false,
    val friends: List<FriendInfo> = emptyList(),
    val error: String? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val connector: ChatConnector,
    private val chatRepository: ChatRepository,
    private val serverRepository: ServerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            connector.isConnected.collect { connected ->
                if (connected) {
                    load(force = true)
                } else if (_state.value.loaded) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            friends = emptyList(),
                            error = "连接已断开",
                            loaded = false,
                        )
                    }
                }
            }
        }
    }

    fun load(force: Boolean = false) {
        if (_state.value.isLoading) return
        if (_state.value.loaded && !force) return
        val active = connector.active
        if (active == null) {
            _state.update { it.copy(error = "未连接服务器", loaded = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val snapshot = active.loadFriends()
                _state.update {
                    it.copy(
                        isLoading = false,
                        friends = snapshot.players,
                        loaded = true,
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "好友列表加载失败",
                        loaded = true,
                    )
                }
            }
        }
    }

    suspend fun openTell(friend: FriendInfo): String? {
        val serverId = connector.active?.profile?.serverId
            ?: serverRepository.activeProfile.first()?.serverId
            ?: return null
        val world = friend.currentWorldName ?: friend.homeWorldName
        val conversationId = ConversationIds.privateChat(serverId, friend.name, world)
        chatRepository.ensureConversation(
            ChatConversation(
                id = conversationId,
                serverId = serverId,
                type = ConversationType.PRIVATE,
                channel = "TELL",
                peerName = friend.name,
                peerWorld = world,
                displayName = if (world != null) "${friend.name}@$world" else friend.name,
                lastMessageText = "",
                lastMessageAt = System.currentTimeMillis(),
                unreadCount = 0,
            ),
        )
        chatRepository.unhideConversation(conversationId)
        return conversationId
    }
}
