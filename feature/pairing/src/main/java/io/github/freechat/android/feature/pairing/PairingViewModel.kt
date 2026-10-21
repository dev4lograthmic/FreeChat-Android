package io.github.freechat.android.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.pairing.PairingLinkParser
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val host: String = "",
    val port: String = "14777",
    val serverId: String = "",
    val fingerprint: String = "",
    val code: String = "",
    val link: String = "",
    val isConnecting: Boolean = false,
    val error: String? = null,
    val connected: Boolean = false,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val connector: ChatConnector,
    private val serverRepository: ServerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    fun updateHost(value: String) = _state.update { it.copy(host = value, error = null) }
    fun updatePort(value: String) = _state.update { it.copy(port = value, error = null) }
    fun updateServerId(value: String) = _state.update { it.copy(serverId = value, error = null) }
    fun updateFingerprint(value: String) =
        _state.update { it.copy(fingerprint = value, error = null) }

    fun updateCode(value: String) = _state.update { it.copy(code = value, error = null) }
    fun updateLink(value: String) = _state.update { it.copy(link = value, error = null) }

    fun parseLink(value: String) {
        runCatching { PairingLinkParser.parse(value) }
            .onSuccess { link ->
                _state.update {
                    it.copy(
                        host = link.host,
                        port = link.port.toString(),
                        serverId = link.serverId,
                        fingerprint = link.fingerprintHex,
                        code = link.code.orEmpty(),
                        link = value,
                        error = null,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.message ?: "深链格式不正确") }
            }
    }

    fun connect() {
        val current = _state.value
        val port = current.port.toIntOrNull()
        val fingerprint = current.fingerprint.trim().lowercase()

        val validationError = when {
            current.host.isBlank() -> "请填写服务器地址"
            port == null || port !in 1..65535 -> "端口不正确"
            current.serverId.isBlank() -> "请填写服务器 ID（深链中的 sid）"
            !FINGERPRINT.matches(fingerprint) -> "指纹必须是 64 位十六进制"
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            try {
                val profile = ServerProfile(
                    serverId = current.serverId.trim(),
                    host = current.host.trim(),
                    port = port!!,
                    fingerprintHex = fingerprint,
                )
                connector.connect(profile, current.code.trim().ifBlank { null })
                serverRepository.save(profile, makeActive = true)
                _state.update { it.copy(isConnecting = false, connected = true) }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        error = error.message ?: error::class.simpleName ?: "连接失败",
                    )
                }
            }
        }
    }

    fun consumeConnected() {
        _state.update { it.copy(connected = false) }
    }

    private companion object {
        val FINGERPRINT = Regex("^[0-9a-f]{64}$")
    }
}
