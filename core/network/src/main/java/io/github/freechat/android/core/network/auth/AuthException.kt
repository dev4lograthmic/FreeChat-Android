package io.github.freechat.android.core.network.auth

import build.buf.gen.freechat.v1.ErrorCode
import build.buf.gen.freechat.v1.PairingRequired
import build.buf.gen.freechat.v1.ServerInfo
import com.google.protobuf.Timestamp

sealed class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PairingCodeRequired(
        val pairingRequired: PairingRequired,
        val server: ServerInfo,
        val expiresAt: Timestamp?,
        val codeLength: Int,
    ) : AuthException("Pairing code required")

    class PairingRejected(
        val code: ErrorCode,
        val detail: String,
    ) : AuthException("Pairing rejected: $detail")

    class Rejected(
        val code: ErrorCode,
        val detail: String,
    ) : AuthException("Server rejected authentication: $detail")

    class ServerMismatch(expected: String, actual: String) :
        AuthException("Server id mismatch: expected $expected, got $actual")

    class FingerprintMismatch : AuthException("Server public key fingerprint mismatch")

    class Timeout : AuthException("Authentication timed out")
}
