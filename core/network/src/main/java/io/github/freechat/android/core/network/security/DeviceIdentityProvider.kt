package io.github.freechat.android.core.network.security

import io.github.freechat.android.core.model.DeviceIdentity

class DeviceIdentityProvider(
    private val deviceKey: DeviceKey,
    private val deviceIdStore: ServerDeviceIdStore,
) {
    fun get(serverId: String): DeviceIdentity = DeviceIdentity(
        deviceId = deviceIdStore.getOrCreate(serverId),
        publicKey = deviceKey.publicKeyBytes(),
    )

    fun update(serverId: String, deviceId: String) {
        if (deviceId.isNotBlank()) deviceIdStore.save(serverId, deviceId)
    }
}
