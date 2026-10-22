package io.github.freechat.android.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "servers",
    indices = [Index(value = ["serverId"])],
)
data class ServerEntity(
    @PrimaryKey val profileId: String,
    val serverId: String,
    val host: String,
    val port: Int,
    val fingerprintHex: String,
    val serverName: String?,
    val protocolVersion: Int,
    val characterId: Long,
    val characterName: String?,
    val worldName: String?,
    val createdAt: Long,
    val lastConnectedAt: Long?,
)
