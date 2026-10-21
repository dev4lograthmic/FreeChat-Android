package io.github.freechat.android.core.network.auth

import build.buf.gen.freechat.v1.Challenge
import build.buf.gen.freechat.v1.Envelope
import build.buf.gen.freechat.v1.PairResult
import build.buf.gen.freechat.v1.PairingRequired
import build.buf.gen.freechat.v1.ServerInfo
import build.buf.gen.freechat.v1.Welcome
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import io.github.freechat.android.core.model.ClientInfo
import io.github.freechat.android.core.model.DeviceIdentity
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.security.DerSignature
import io.github.freechat.android.core.network.security.DeviceKey
import io.github.freechat.android.core.network.tls.RawPublicKeyPin
import io.github.freechat.android.core.network.transport.EnvelopeTransport
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthSessionTest {
    @Test
    fun `known device signs challenge and receives welcome`() = runTest {
        val fixture = Fixture()
        val transport = FakeTransport()
        transport.onSend = { envelope ->
            when {
                envelope.hasClientHello() -> transport.emit(
                    envelope { challenge = fixture.challenge() },
                )

                envelope.hasChallengeResponse() -> {
                    assertThat(envelope.challengeResponse.signature.size()).isEqualTo(64)
                    transport.emit(envelope { welcome = fixture.welcome() })
                }
            }
        }

        val welcome = fixture.session(transport).authenticate()

        assertThat(welcome.sessionId).isEqualTo("sess-1")
        assertThat(transport.sent.count { it.hasClientHello() }).isEqualTo(1)
        assertThat(transport.sent.count { it.hasChallengeResponse() }).isEqualTo(1)
    }

    @Test
    fun `pairing sends pair request and stores server device id`() = runTest {
        val fixture = Fixture()
        val transport = FakeTransport()
        var pairedDeviceId: String? = null
        transport.onSend = { envelope ->
            when {
                envelope.hasClientHello() -> transport.emit(
                    envelope { pairingRequired = fixture.pairingRequired() },
                )

                envelope.hasPairRequest() -> {
                    assertThat(envelope.pairRequest.code).isEqualTo("482913")
                    transport.emit(
                        envelope {
                            pairResult = PairResult.newBuilder()
                                .setOk(true)
                                .setDeviceId("server-device")
                                .build()
                        },
                    )
                    transport.emit(envelope { welcome = fixture.welcome() })
                }
            }
        }

        fixture.session(
            transport = transport,
            pairingCodeProvider = { "482913" },
            onDevicePaired = { pairedDeviceId = it },
        ).authenticate()

        assertThat(pairedDeviceId).isEqualTo("server-device")
        assertThat(transport.sent.count { it.hasPairRequest() }).isEqualTo(1)
    }

    @Test
    fun `rejects a mismatched server fingerprint`() = runTest {
        val fixture = Fixture()
        val otherKey = TestDeviceKey()
        val transport = FakeTransport()
        transport.onSend = { envelope ->
            if (envelope.hasClientHello()) {
                transport.emit(
                    envelope {
                        pairingRequired = PairingRequired.newBuilder()
                            .setServer(
                                fixture.serverInfo(
                                    RawPublicKeyPin.fingerprint(otherKey.publicKey()),
                                ),
                            )
                            .setCodeLength(6)
                            .build()
                    },
                )
            }
        }

        val error = runCatching {
            fixture.session(transport).authenticate()
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(AuthException.FingerprintMismatch::class.java)
    }

    private class Fixture {
        private val serverKey = TestDeviceKey()
        private val deviceKey = TestDeviceKey()
        val serverFingerprint = RawPublicKeyPin.fingerprint(serverKey.publicKey())

        fun serverInfo(fingerprint: ByteArray = serverFingerprint): ServerInfo =
            ServerInfo.newBuilder()
                .setServerId("srv-1")
                .setServerName("Test Server")
                .setProtocolVersion(1)
                .setPublicKeyFingerprint(ByteString.copyFrom(fingerprint))
                .build()

        fun challenge(): Challenge = Challenge.newBuilder()
            .setNonce(ByteString.copyFrom(ByteArray(32) { it.toByte() }))
            .setAlgorithm("ECDSA_P256_SHA256_P1363")
            .build()

        fun pairingRequired(): PairingRequired = PairingRequired.newBuilder()
            .setServer(serverInfo())
            .setCodeLength(6)
            .build()

        fun welcome(): Welcome = Welcome.newBuilder()
            .setServer(serverInfo())
            .setSessionId("sess-1")
            .setDeviceId("server-device")
            .build()

        fun session(
            transport: EnvelopeTransport,
            pairingCodeProvider: suspend (PairingRequired) -> String? = { null },
            onDevicePaired: suspend (String) -> Unit = {},
        ): AuthSession = AuthSession(
            transport = transport,
            profile = ServerProfile(
                serverId = "srv-1",
                host = "127.0.0.1",
                port = 14777,
                fingerprintHex = RawPublicKeyPin.toHex(serverFingerprint),
            ),
            deviceKey = deviceKey,
            identity = DeviceIdentity(
                deviceId = "dev-1",
                publicKey = deviceKey.publicKeyBytes(),
            ),
            clientInfo = ClientInfo(clientVersion = "test"),
            pairingCodeProvider = pairingCodeProvider,
            onDevicePaired = onDevicePaired,
            timeoutMillis = 2_000,
        )
    }

    private class TestDeviceKey : DeviceKey {
        private val pair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        fun publicKey(): ECPublicKey = pair.public as ECPublicKey

        override fun publicKeyBytes(): ByteArray =
            RawPublicKeyPin.encodeUncompressed(publicKey())

        override fun sign(data: ByteArray): ByteArray {
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(pair.private)
            signer.update(data)
            return DerSignature.toP1363(signer.sign())
        }
    }

    private class FakeTransport : EnvelopeTransport {
        private val channel = Channel<Envelope>(Channel.UNLIMITED)
        override val incoming: Flow<Envelope> = channel.receiveAsFlow()
        val sent = mutableListOf<Envelope>()
        var onSend: suspend (Envelope) -> Unit = {}

        override suspend fun open() = Unit

        override suspend fun send(envelope: Envelope) {
            sent += envelope
            onSend(envelope)
        }

        override suspend fun close(code: Int, reason: String?) {
            channel.close()
        }

        fun emit(envelope: Envelope) {
            channel.trySend(envelope)
        }
    }

    private fun envelope(block: Envelope.Builder.() -> Unit): Envelope =
        Envelope.newBuilder().setRequestId("test").apply(block).build()
}
