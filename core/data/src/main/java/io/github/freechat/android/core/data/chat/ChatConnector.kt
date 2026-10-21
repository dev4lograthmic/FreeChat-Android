package io.github.freechat.android.core.data.chat

import io.github.freechat.android.core.model.ServerProfile

/**
 * Abstraction used by feature modules to start a chat session without
 * depending on the app-level session holder.
 */
interface ChatConnector {
    val active: ActiveChat?

    suspend fun connect(profile: ServerProfile, pairingCode: String?): ActiveChat
    suspend fun close()
}
