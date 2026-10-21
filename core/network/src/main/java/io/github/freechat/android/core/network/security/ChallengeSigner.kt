package io.github.freechat.android.core.network.security

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * The exact challenge input defined by freechat.v1:
 *
 * "FreeChat/v1 challenge" || 0x00 || server_id || 0x00 || device_id ||
 * 0x00 || nonce
 */
object ChallengeSigner {
    private val PREFIX = "FreeChat/v1 challenge".toByteArray(StandardCharsets.UTF_8)

    fun buildInput(serverId: String, deviceId: String, nonce: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(PREFIX.size + serverId.length + deviceId.length + nonce.size + 3)
        output.write(PREFIX)
        output.write(0)
        output.write(serverId.toByteArray(StandardCharsets.UTF_8))
        output.write(0)
        output.write(deviceId.toByteArray(StandardCharsets.UTF_8))
        output.write(0)
        output.write(nonce)
        return output.toByteArray()
    }

    fun sign(
        deviceKey: DeviceKey,
        serverId: String,
        deviceId: String,
        nonce: ByteArray,
    ): ByteArray = deviceKey.sign(buildInput(serverId, deviceId, nonce))
}
