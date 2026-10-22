package io.github.freechat.android

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.freechat.android.core.data.chat.ActiveChat
import io.github.freechat.android.core.data.chat.ChatConnector
import io.github.freechat.android.core.data.chat.ChatController
import io.github.freechat.android.core.data.store.SettingsStore
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.service.FreeChatService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Owns the current authenticated chat session across screens and the
 * foreground service.
 */
@Singleton
class SessionHolder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chatController: ChatController,
    private val settingsStore: SettingsStore,
) : ChatConnector {
    @Volatile
    var current: ActiveChat? = null
        private set

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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
    ): ActiveChat = withContext(Dispatchers.IO) {
        current?.close()
        current = null
        _isConnected.value = false
        val settings = settingsStore.settings.first()
        val active = chatController.connect(
            profile = profile,
            includeBattle = settings.subscribeBattleLog,
            sources = settings.developerSources,
            contentSource = settings.developerMessageContent,
            pairingCodeProvider = { pairingCode },
            onPairingRequired = { onPairingRequired() },
        )
        current = active
        _isConnected.value = true
        if (startService) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FreeChatService::class.java),
            )
        }
        active
    }

    override suspend fun connect(profile: ServerProfile, pairingCode: String?): ActiveChat =
        connect(profile, pairingCode, onPairingRequired = {})

    fun invalidate(active: ActiveChat?) {
        if (current === active) {
            current = null
            _isConnected.value = false
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            current?.close()
            current = null
            _isConnected.value = false
            context.stopService(Intent(context, FreeChatService::class.java))
        }
    }

    override suspend fun requestReconnect() = withContext(Dispatchers.IO) {
        val active = current ?: return@withContext
        ContextCompat.startForegroundService(
            context,
            Intent(context, FreeChatService::class.java),
        )
        active.close()
        current = null
        _isConnected.value = false
    }
}
