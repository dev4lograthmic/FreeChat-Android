package io.github.freechat.android.core.model

/**
 * Stable local identity for one character on one FreeChat server.
 *
 * The server id alone is not enough because one server can be used by
 * multiple characters. The game-provided content id is the only identity
 * used here.
 */
object ProfileIds {
    fun of(serverId: String, characterId: Long): String =
        "$serverId:id:$characterId"
}
