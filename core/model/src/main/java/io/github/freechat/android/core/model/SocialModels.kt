package io.github.freechat.android.core.model

data class FriendInfo(
    val contentId: Long,
    val name: String,
    val homeWorldName: String?,
    val currentWorldName: String?,
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
