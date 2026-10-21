package io.github.freechat.android.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val serverId: String,
    val host: String,
    val port: Int,
    val fingerprintHex: String,
    val serverName: String?,
    val protocolVersion: Int,
    val characterName: String?,
    val worldName: String?,
    val createdAt: Long,
    val lastConnectedAt: Long?,
)
