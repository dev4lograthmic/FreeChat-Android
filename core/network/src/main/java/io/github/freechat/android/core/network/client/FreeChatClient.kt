package io.github.freechat.android.core.network.client

import build.buf.gen.freechat.v1.Envelope
import build.buf.gen.freechat.v1.PairingRequired
import build.buf.gen.freechat.v1.Welcome
import io.github.freechat.android.core.model.ClientInfo
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.auth.AuthSession
import io.github.freechat.android.core.network.security.DeviceIdentityProvider
import io.github.freechat.android.core.network.security.DeviceKey
import io.github.freechat.android.core.network.tls.PinnedOkHttpClientFactory
import io.github.freechat.android.core.network.transport.EnvelopeTransport
import io.github.freechat.android.core.network.transport.OkHttpEnvelopeTransport
import kotlinx.coroutines.flow.Flow

class FreeChatClient(
    private val clientFactory: PinnedOkHttpClientFactory,
    private val deviceKey: DeviceKey,
    private val identityProvider: DeviceIdentityProvider,
    private val clientInfo: ClientInfo,
) {
    suspend fun connect(
        profile: ServerProfile,
        pairingCodeProvider: suspend (PairingRequired) -> String? = { null },
        onPairingRequired: suspend (PairingRequired) -> Unit = {},
    ): FreeChatConnection {
        val identity = identityProvider.get(profile.serverId)
        val transport = OkHttpEnvelopeTransport(
            client = clientFactory.create(profile),
            profile = profile,
        )
        val auth = AuthSession(
            transport = transport,
            profile = profile,
            deviceKey = deviceKey,
            identity = identity,
            clientInfo = clientInfo,
            pairingCodeProvider = pairingCodeProvider,
            onPairingRequired = onPairingRequired,
            onDevicePaired = { deviceId ->
                identityProvider.update(profile.serverId, deviceId)
            },
        )
        val welcome = auth.authenticate()
        return FreeChatConnection(profile, transport, welcome)
    }
}

class FreeChatConnection(
    val profile: ServerProfile,
    private val transport: EnvelopeTransport,
    val welcome: Welcome,
) {
    val incoming: Flow<Envelope> = transport.incoming

    suspend fun send(envelope: Envelope) = transport.send(envelope)

    suspend fun close() = transport.close()
}
