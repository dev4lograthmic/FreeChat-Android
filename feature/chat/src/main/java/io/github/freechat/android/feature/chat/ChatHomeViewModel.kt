package io.github.freechat.android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.conversation.ConversationIds
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatHomeViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val connector: ChatConnector,
    private val serverRepository: ServerRepository,
) : ViewModel() {
    val conversations: StateFlow<List<ChatConversation>> =
        repository.observeAllConversations().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val totalUnread: StateFlow<Int> = repository.observeTotalUnread().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
    )

    private val _searchResults = MutableStateFlow<List<ChatMessage>>(emptyList())
    val searchResults: StateFlow<List<ChatMessage>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    val isConnected: Boolean
        get() = connector.active != null

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(200)
            _searchResults.value = repository.search(query)
        }
    }

    suspend fun createPrivateChat(peerName: String, peerWorld: String): String? {
        val name = peerName.trim()
        val world = peerWorld.trim()
        if (name.isBlank()) return null
        val serverId = connector.active?.profile?.serverId
            ?: serverRepository.activeProfile.first()?.serverId
            ?: return null
        val conversationId = ConversationIds.privateChat(
            serverId = serverId,
            peerName = name,
            peerWorld = world.ifBlank { null },
        )
        val displayName = if (world.isBlank()) name else "$name@$world"
        repository.ensureConversation(
            ChatConversation(
                id = conversationId,
                serverId = serverId,
                type = ConversationType.PRIVATE,
                channel = "TELL",
                peerName = name,
                peerWorld = world.ifBlank { null },
                displayName = displayName,
                lastMessageText = "",
                lastMessageAt = System.currentTimeMillis(),
                unreadCount = 0,
            ),
        )
        return conversationId
    }
}
