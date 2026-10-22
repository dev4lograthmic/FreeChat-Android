package io.github.freechat.android.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.ui.R

/**
 * Localised display name for a protocol channel. Falls back to the stored
 * display name when the channel is unknown.
 */
@Composable
fun chatChannelLabel(
    type: ConversationType,
    channel: String?,
    fallback: String,
): String = if (type == ConversationType.PRIVATE) {
    fallback
} else {
    chatChannelLabel(channel, fallback)
}

@Composable
fun chatChannelLabel(
    channel: String?,
    fallback: String,
): String {
    val key = channel.orEmpty().uppercase()
    val resId = when (key) {
        "SAY" -> R.string.channel_say
        "SHOUT" -> R.string.channel_shout
        "YELL" -> R.string.channel_yell
        "PARTY" -> R.string.channel_party
        "ALLIANCE" -> R.string.channel_alliance
        "FREE_COMPANY" -> R.string.channel_free_company
        "LINKSHELL_1" -> R.string.channel_linkshell_1
        "LINKSHELL_2" -> R.string.channel_linkshell_2
        "LINKSHELL_3" -> R.string.channel_linkshell_3
        "LINKSHELL_4" -> R.string.channel_linkshell_4
        "LINKSHELL_5" -> R.string.channel_linkshell_5
        "LINKSHELL_6" -> R.string.channel_linkshell_6
        "LINKSHELL_7" -> R.string.channel_linkshell_7
        "LINKSHELL_8" -> R.string.channel_linkshell_8
        "CROSS_LINKSHELL_1" -> R.string.channel_cross_linkshell_1
        "CROSS_LINKSHELL_2" -> R.string.channel_cross_linkshell_2
        "CROSS_LINKSHELL_3" -> R.string.channel_cross_linkshell_3
        "CROSS_LINKSHELL_4" -> R.string.channel_cross_linkshell_4
        "CROSS_LINKSHELL_5" -> R.string.channel_cross_linkshell_5
        "CROSS_LINKSHELL_6" -> R.string.channel_cross_linkshell_6
        "CROSS_LINKSHELL_7" -> R.string.channel_cross_linkshell_7
        "CROSS_LINKSHELL_8" -> R.string.channel_cross_linkshell_8
        "NOVICE_NETWORK" -> R.string.channel_novice_network
        "PVP_TEAM" -> R.string.channel_pvp_team
        "EMOTE" -> R.string.channel_emote
        "SYSTEM" -> R.string.system_system
        "ERROR" -> R.string.system_error
        "NPC" -> R.string.system_npc
        "BATTLE", "BATTLE_LOG", "LOOT", "PROGRESS" -> R.string.system_battle_log
        else -> null
    }
    return resId?.let { stringResource(it) } ?: fallback
}
