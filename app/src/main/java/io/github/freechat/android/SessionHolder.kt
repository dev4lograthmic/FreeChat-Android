package io.github.freechat.android

import io.github.freechat.android.core.data.chat.ActiveChat
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.chat.ChatController
import io.github.freechat.android.core.model.ServerProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the current authenticated chat session across screens.
 */
@Singleton
class SessionHolder @Inject constructor(
    private val chatController: ChatController,
) : ChatConnector {
    @Volatile
    var current: ActiveChat? = null
        private set

    suspend fun connect(
        profile: ServerProfile,
        pairingCode: String?,
        onPairingRequired: suspend () -> Unit,
    ): ActiveChat {
        current?.close()
        current = null
        val active = chatController.connect(
            profile = profile,
            pairingCodeProvider = { pairingCode },
            onPairingRequired = { onPairingRequired() },
        )
        current = active
        return active
    }

    override suspend fun connect(profile: ServerProfile, pairingCode: String?): ActiveChat =
        connect(profile, pairingCode, onPairingRequired = {})

    override suspend fun close() {
        current?.close()
        current = null
    }
}
