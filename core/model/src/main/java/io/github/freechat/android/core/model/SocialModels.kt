package io.github.freechat.android.core.model

data class FriendInfo(
    val contentId: Long,
    val name: String,
    val homeWorldId: Int,
    val homeWorldName: String?,
    val currentWorldId: Int,
    val currentWorldName: String?,
    val homeDataCenterId: Int,
    val homeDataCenterName: String?,
    val currentDataCenterId: Int,
    val currentDataCenterName: String?,
    val freeCompanyTag: String?,
    val jobName: String?,
    val territoryName: String?,
    val statusBits: Long,
    val identityUnavailable: Boolean,
)

data class FriendListSnapshot(
    val players: List<FriendInfo>,
    val capturedAt: Long,
    val ownerName: String?,
    val ownerWorld: String?,
)
