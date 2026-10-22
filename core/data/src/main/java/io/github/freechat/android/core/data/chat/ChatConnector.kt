package io.github.freechat.android.core.data.chat

import io.github.freechat.android.core.model.ServerProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction used by feature modules to start a chat session without
 * depending on the app-level session holder.
 */
interface ChatConnector {
    val active: ActiveChat?
    val isConnected: StateFlow<Boolean>

    suspend fun connect(profile: ServerProfile, pairingCode: String?): ActiveChat
    suspend fun close()
    suspend fun requestReconnect()
}
