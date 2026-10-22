package io.github.freechat.android.feature.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.export.ExportFormat
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.data.store.AppSettings
import io.github.freechat.android.core.data.store.SettingsStore
import io.github.freechat.android.core.model.ServerProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val settingsStore: SettingsStore,
    private val chatRepository: ChatRepository,
    private val connector: ChatConnector,
) : ViewModel() {
    val profiles: StateFlow<List<ServerProfile>> = serverRepository.profiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val activeProfile: StateFlow<ServerProfile?> = serverRepository.activeProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val activeChat get() = connector.active
    val isConnected = connector.isConnected

    fun setActive(serverId: String) {
        viewModelScope.launch { serverRepository.setActive(serverId) }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            serverRepository.delete(serverId)
            _message.value = "服务器已删除"
        }
    }

    fun updateSettings(
        transform: (AppSettings) -> AppSettings,
        reconnect: Boolean = false,
    ) {
        viewModelScope.launch {
            settingsStore.update(transform)
            if (reconnect) {
                connector.requestReconnect()
                _message.value = "设置已应用，正在重连"
            }
        }
    }

    fun export(
        uri: Uri,
        resolver: ContentResolver,
        format: ExportFormat,
        includeRaw: Boolean,
    ) {
        val serverId = activeChat?.profile?.serverId ?: activeProfile.value?.serverId
        if (serverId == null) {
            _message.value = "请先连接服务器"
            return
        }
        viewModelScope.launch {
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    chatRepository.export(
                        serverId = serverId,
                        format = format,
                        includeRaw = includeRaw,
                        output = output,
                    )
                } ?: error("无法写入文件")
                _message.value = "导出完成"
            } catch (error: Exception) {
                _message.value = error.message ?: "导出失败"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            chatRepository.clearAll()
            _message.value = "聊天记录已清空"
        }
    }
}
