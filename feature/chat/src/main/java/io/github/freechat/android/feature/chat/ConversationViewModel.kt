package io.github.freechat.android.feature.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.model.SendOutcomeStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
    private val connector: ChatConnector,
) : ViewModel() {
    val conversationId: String = Uri.decode(checkNotNull(savedStateHandle["conversationId"]))

    private val _conversation = MutableStateFlow<ChatConversation?>(null)
    val conversation: StateFlow<ChatConversation?> = _conversation.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = repository.observeMessages(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    init {
        viewModelScope.launch {
            _conversation.value = repository.getConversation(conversationId)
            repository.markRead(conversationId)
        }
    }

    fun updateInput(value: String) {
        _input.value = value
    }

    fun send() {
        val conversation = _conversation.value ?: return
        if (conversation.type == ConversationType.SYSTEM) return
        val text = _input.value.trim()
        if (text.isBlank() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _status.value = null
            try {
                val active = connector.active
                if (active == null) {
                    _status.value = "未连接服务器"
                    return@launch
                }
                val result = active.send(conversation, text)
                _status.value = when (result.status) {
                    SendOutcomeStatus.SUBMITTED -> "已发送"
                    SendOutcomeStatus.BLOCKED -> "命令被服务器拦截"
                    SendOutcomeStatus.RATE_LIMITED -> "发送过快，请稍后再试"
                    SendOutcomeStatus.QUEUE_FULL -> "发送队列已满"
                    SendOutcomeStatus.FAILED -> result.message.ifBlank { "发送失败" }
                    else -> result.message.ifBlank { result.status.name }
                }
                if (result.status == SendOutcomeStatus.SUBMITTED) {
                    repository.unhideConversation(conversationId)
                    _input.value = ""
                }
            } catch (error: Exception) {
                _status.value = error.message ?: "发送失败"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
