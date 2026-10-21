package io.github.freechat.android

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.freechat.android.core.data.chat.ActiveChat
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.chat.ChatController
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.service.FreeChatService
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Owns the current authenticated chat session across screens and the
 * foreground service.
 */
@Singleton
class SessionHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatController: ChatController,
) : ChatConnector {
    @Volatile
    var current: ActiveChat? = null
        private set

    override val active: ActiveChat?
        get() = current

    suspend fun connect(
        profile: ServerProfile,
        pairingCode: String?,
        onPairingRequired: suspend () -> Unit,
    ): ActiveChat = connectInternal(profile, pairingCode, onPairingRequired, startService = true)

    suspend fun connectForService(profile: ServerProfile): ActiveChat =
        connectInternal(profile, pairingCode = null, onPairingRequired = {}, startService = false)

    private suspend fun connectInternal(
        profile: ServerProfile,
        pairingCode: String?,
        onPairingRequired: suspend () -> Unit,
        startService: Boolean,
    ): ActiveChat {
        current?.close()
        current = null
        val active = chatController.connect(
            profile = profile,
            pairingCodeProvider = { pairingCode },
            onPairingRequired = { onPairingRequired() },
        )
        current = active
        if (startService) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FreeChatService::class.java),
            )
        }
        return active
    }

    override suspend fun connect(profile: ServerProfile, pairingCode: String?): ActiveChat =
        connect(profile, pairingCode, onPairingRequired = {})

    fun invalidate(active: ActiveChat?) {
        if (current === active) current = null
    }

    override suspend fun close() {
        current?.close()
        current = null
        context.stopService(Intent(context, FreeChatService::class.java))
    }
}
