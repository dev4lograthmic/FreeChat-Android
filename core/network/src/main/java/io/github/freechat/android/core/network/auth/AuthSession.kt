package io.github.freechat.android.core.network.auth

import build.buf.gen.freechat.v1.ChallengeResponse
import build.buf.gen.freechat.v1.ClientCapabilities
import build.buf.gen.freechat.v1.ClientHello
import build.buf.gen.freechat.v1.Envelope
import build.buf.gen.freechat.v1.PairRequest
import build.buf.gen.freechat.v1.PairingRequired
import build.buf.gen.freechat.v1.ServerInfo
import build.buf.gen.freechat.v1.Welcome
import com.google.protobuf.ByteString
import io.github.freechat.android.core.model.ClientInfo
import io.github.freechat.android.core.model.DeviceIdentity
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.security.ChallengeSigner
import io.github.freechat.android.core.network.security.DeviceKey
import io.github.freechat.android.core.network.tls.RawPublicKeyPin
import io.github.freechat.android.core.network.transport.EnvelopeTransport
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Performs the freechat.v1 handshake on an already opened transport:
 * ClientHello -> PairingRequired/PairRequest or Challenge/ChallengeResponse
 * -> Welcome.
 */
class AuthSession(
    private val transport: EnvelopeTransport,
    private val profile: ServerProfile,
    private val deviceKey: DeviceKey,
    private val identity: DeviceIdentity,
    private val clientInfo: ClientInfo,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val pairingCodeProvider: suspend (PairingRequired) -> String? = { null },
    private val onPairingRequired: suspend (PairingRequired) -> Unit = {},
    private val onDevicePaired: suspend (String) -> Unit = {},
    private val timeoutMillis: Long = 60_000,
) {
    suspend fun authenticate(): Welcome = coroutineScope {
        transport.open()
        val result = CompletableDeferred<Welcome>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            transport.incoming
                .catch { error -> result.completeExceptionally(error) }
                .onCompletion { cause ->
                    if (!result.isCompleted) {
                        result.completeExceptionally(
                            cause ?: IllegalStateException("Connection closed during authentication"),
                        )
                    }
                }
                .collect { envelope ->
                    try {
                        handle(envelope, result)
                    } catch (error: Throwable) {
                        result.completeExceptionally(error)
                    }
                }
        }

        try {
            transport.send(clientHello())
            withTimeout(timeoutMillis) { result.await() }
        } catch (timeout: TimeoutCancellationException) {
            throw AuthException.Timeout()
        } catch (error: Throwable) {
            transport.close()
            throw error
        } finally {
            collector.cancel()
        }
    }

    private suspend fun handle(envelope: Envelope, result: CompletableDeferred<Welcome>) {
        when {
            envelope.hasPairingRequired() -> {
                val message = envelope.pairingRequired
                verifyServer(message.server)
                onPairingRequired(message)
                val code = pairingCodeProvider(message)
                if (code.isNullOrBlank()) {
                    result.completeExceptionally(
                        AuthException.PairingCodeRequired(
                            pairingRequired = message,
                            server = message.server,
                            expiresAt = message.expiresAt.takeIf { message.hasExpiresAt() },
                            codeLength = message.codeLength,
                        ),
                    )
                    return
                }
                transport.send(pairRequest(code))
            }

            envelope.hasChallenge() -> {
                val signature = ChallengeSigner.sign(
                    deviceKey = deviceKey,
                    serverId = profile.serverId,
                    deviceId = identity.deviceId,
                    nonce = envelope.challenge.nonce.toByteArray(),
                )
                transport.send(
                    envelope {
                        challengeResponse = ChallengeResponse.newBuilder()
                            .setSignature(ByteString.copyFrom(signature))
                            .build()
                    },
                )
            }

            envelope.hasPairResult() -> {
                val pairResult = envelope.pairResult
                if (!pairResult.ok) {
                    result.completeExceptionally(
                        AuthException.PairingRejected(pairResult.error, pairResult.message),
                    )
                    return
                }
                if (pairResult.deviceId.isNotBlank()) {
                    onDevicePaired(pairResult.deviceId)
                }
            }

            envelope.hasWelcome() -> {
                verifyServer(envelope.welcome.server)
                result.complete(envelope.welcome)
            }

            envelope.hasAuthError() -> {
                result.completeExceptionally(
                    AuthException.Rejected(envelope.authError.code, envelope.authError.message),
                )
            }

            envelope.hasError() -> {
                result.completeExceptionally(
                    AuthException.Rejected(envelope.error.code, envelope.error.message),
                )
            }
        }
    }

    private fun verifyServer(server: ServerInfo) {
        if (server.serverId != profile.serverId) {
            throw AuthException.ServerMismatch(profile.serverId, server.serverId)
        }
        val expected = RawPublicKeyPin.fromHex(profile.fingerprintHex)
        if (!server.publicKeyFingerprint.toByteArray().contentEquals(expected)) {
            throw AuthException.FingerprintMismatch()
        }
    }

    private fun clientHello(): Envelope = envelope {
        clientHello = ClientHello.newBuilder()
            .setProtocolVersion(profile.protocolVersion)
            .setClientName(clientInfo.clientName)
            .setClientVersion(clientInfo.clientVersion)
            .setPlatform(clientInfo.platform)
            .setDeviceId(identity.deviceId)
            .setDevicePublicKey(ByteString.copyFrom(identity.publicKey))
            .addAllCapabilities(clientInfo.capabilities)
            .build()
    }

    private fun pairRequest(code: String): Envelope = envelope {
        pairRequest = PairRequest.newBuilder()
            .setCode(code)
            .setDeviceName(clientInfo.clientName)
            .setPlatform(clientInfo.platform)
            .setDevicePublicKey(ByteString.copyFrom(identity.publicKey))
            .setCapabilities(
                ClientCapabilities.newBuilder()
                    .addAllFeatures(clientInfo.capabilities)
                    .build(),
            )
            .build()
    }

    private fun envelope(block: Envelope.Builder.() -> Unit): Envelope =
        Envelope.newBuilder()
            .setRequestId(idGenerator())
            .apply(block)
            .build()
}
