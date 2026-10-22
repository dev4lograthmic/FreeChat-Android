package io.github.freechat.android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.conversation.ConversationIds
import io.github.freechat.android.core.data.conversation.ConversationLabels
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.data.store.SettingsStore
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConnectionStatus
import io.github.freechat.android.core.model.ConversationType
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatHomeViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val connector: ChatConnector,
    private val serverRepository: ServerRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val activeProfileId = serverRepository.activeProfile
        .map { it?.profileId }
        .distinctUntilChanged()

    val conversations: StateFlow<List<ChatConversation>> =
        activeProfileId.flatMapLatest { profileId ->
            if (profileId == null) {
                flowOf(emptyList())
            } else {
                repository.observeAllConversations(profileId)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val linkshellNames: StateFlow<Map<String, String>> =
        activeProfileId.flatMapLatest { profileId ->
            if (profileId == null) {
                flowOf(emptyMap())
            } else {
                repository.observeLinkshellNames(profileId)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    val totalUnread: StateFlow<Int> =
        activeProfileId.flatMapLatest { profileId ->
            if (profileId == null) flowOf(0) else repository.observeTotalUnread(profileId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    val collapsedSections: StateFlow<Set<ConversationType>> = settingsStore.settings
        .map { it.collapsedSections }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    private val _searchResults = MutableStateFlow<List<ChatMessage>>(emptyList())
    val searchResults: StateFlow<List<ChatMessage>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    val connectionStatus: StateFlow<ConnectionStatus> = connector.connectionStatus

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(200)
            val profileId = currentProfileId() ?: return@launch
            _searchResults.value = repository.search(query, profileId)
        }
    }

    fun toggleSection(type: ConversationType) {
        viewModelScope.launch {
            settingsStore.update { settings ->
                val next = if (type in settings.collapsedSections) {
                    settings.collapsedSections - type
                } else {
                    settings.collapsedSections + type
                }
                settings.copy(collapsedSections = next)
            }
        }
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch { repository.markRead(conversationId) }
    }

    fun hideConversation(conversationId: String) {
        viewModelScope.launch { repository.hideConversation(conversationId) }
    }

    fun unhideConversation(conversationId: String) {
        viewModelScope.launch { repository.unhideConversation(conversationId) }
    }

    suspend fun createPrivateChat(peerName: String, peerWorld: String): String? {
        val name = peerName.trim()
        val world = peerWorld.trim()
        if (name.isBlank()) return null
        val profile = serverRepository.activeProfile.first()
        val serverId = connector.active?.profile?.serverId
            ?: profile?.serverId
            ?: return null
        val profileId = connector.active?.sessionInfo?.profileId
            ?: profile?.profileId
            ?: return null
        val conversationId = ConversationIds.privateChat(
            profileId = profileId,
            peerName = name,
            peerWorld = world.ifBlank { null },
        )
        val displayName = if (world.isBlank()) name else "$name@$world"
        repository.ensureConversation(
            ChatConversation(
                id = conversationId,
                profileId = profileId,
                serverId = serverId,
                type = ConversationType.PRIVATE,
                channel = "TELL",
                peerName = name,
                peerWorld = world.ifBlank { null },
                displayName = displayName,
                lastMessageText = "",
                lastMessageAt = 0,
                unreadCount = 0,
            ),
        )
        return conversationId
    }

    suspend fun createChannelChat(channel: String): String? {
        val isLinkshell = ConversationLabels.isLinkshell(channel)
        if (!isLinkshell && channel !in ConversationLabels.publicChannels) return null
        val profile = serverRepository.activeProfile.first()
        val serverId = connector.active?.profile?.serverId
            ?: profile?.serverId
            ?: return null
        val profileId = connector.active?.sessionInfo?.profileId
            ?: profile?.profileId
            ?: return null
        val conversationId = ConversationIds.publicChannel(profileId, channel)
        repository.getConversation(conversationId)?.let { return conversationId }
        repository.ensureConversation(
            ChatConversation(
                id = conversationId,
                profileId = profileId,
                serverId = serverId,
                type = if (isLinkshell) ConversationType.LINKSHELL else ConversationType.PUBLIC_CHANNEL,
                channel = channel,
                peerName = null,
                peerWorld = null,
                displayName = if (isLinkshell) {
                    ConversationLabels.linkshellLabel(channel)
                } else {
                    ConversationLabels.publicLabel(channel)
                },
                lastMessageText = "",
                lastMessageAt = 0,
                unreadCount = 0,
            ),
        )
        return conversationId
    }

    private suspend fun currentProfileId(): String? =
        connector.active?.sessionInfo?.profileId
            ?: serverRepository.activeProfile.first()?.profileId
}
