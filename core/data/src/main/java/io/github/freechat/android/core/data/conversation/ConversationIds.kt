package io.github.freechat.android.core.data.conversation

/**
 * Conversation ids are stable per server, not per game session, so local
 * history survives a Dalamud session restart.
 */
object ConversationIds {
    fun publicChannel(serverId: String, channel: String): String =
        "$serverId:public:$channel"

    fun privateChat(serverId: String, peerName: String, peerWorld: String?): String =
        "$serverId:tell:${peerWorld.orEmpty()}:$peerName"

    fun system(serverId: String, category: String): String =
        "$serverId:system:$category"
}
