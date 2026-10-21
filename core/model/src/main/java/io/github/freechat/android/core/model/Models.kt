package io.github.freechat.android.core.model

/**
 * A persisted connection target. The fingerprint is the lowercase hex SHA-256
 * of the server's P-256 public key in X9.63 uncompressed form.
 */
data class ServerProfile(
    val serverId: String,
    val host: String,
    val port: Int,
    val fingerprintHex: String,
    val serverName: String? = null,
    val protocolVersion: Int = 1,
    val characterName: String? = null,
    val worldName: String? = null,
) {
    val webSocketUrl: String
        get() {
            val hostPart = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
            return "wss://$hostPart:$port/ws"
        }
}

data class PairingLink(
    val host: String,
    val port: Int,
    val serverId: String,
    val fingerprintHex: String,
    val code: String?,
    val protocolVersion: Int = 1,
) {
    fun toServerProfile(): ServerProfile = ServerProfile(
        serverId = serverId,
        host = host,
        port = port,
        fingerprintHex = fingerprintHex,
        protocolVersion = protocolVersion,
    )
}

data class DeviceIdentity(
    val deviceId: String,
    val publicKey: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceIdentity) return false
        return deviceId == other.deviceId && publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int = 31 * deviceId.hashCode() + publicKey.contentHashCode()
}

data class ClientInfo(
    val clientName: String = "FreeChat Android",
    val clientVersion: String,
    val platform: String = "android",
    val capabilities: List<String> = DEFAULT_CAPABILITIES,
) {
    companion object {
        val DEFAULT_CAPABILITIES = listOf(
            "chat.segments",
            "chat.chunks",
            "chat.raw",
            "chat.original",
            "chat.sources.post_handle",
            "filters.regex",
            "replay.sequence",
            "send.raw",
            "send.structured",
            "pairing.device_keys",
            "tls.pinning.sha256",
        )
    }
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data object Authenticating : ConnectionState
    data class Ready(
        val serverName: String?,
        val characterName: String?,
        val sessionId: String,
    ) : ConnectionState

    data class Reconnecting(val attempt: Int, val reason: String?) : ConnectionState
    data class Disconnected(val reason: String?) : ConnectionState
}
