package io.github.freechat.android.core.network.security

/**
 * Platform abstraction for the device's P-256 signing key. Implementations
 * must keep the private key non-exportable.
 */
interface DeviceKey {
    fun publicKeyBytes(): ByteArray
    fun sign(data: ByteArray): ByteArray
}
