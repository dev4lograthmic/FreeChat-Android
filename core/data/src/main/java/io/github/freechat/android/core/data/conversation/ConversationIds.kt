package io.github.freechat.android.core.data.conversation

/**
 * Conversation ids are stable per local character, not per game session, so
 * local history survives a Dalamud session restart and two characters on the
 * same server keep separate histories.
 */
object ConversationIds {
    fun publicChannel(profileId: String, channel: String): String =
        "$profileId:public:$channel"

    fun privateChat(profileId: String, peerName: String, peerWorld: String?): String =
        "$profileId:tell:${peerWorld.orEmpty()}:$peerName"

    fun system(profileId: String, category: String): String =
        "$profileId:system:$category"
}
