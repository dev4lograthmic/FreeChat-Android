package io.github.freechat.android.core.data.conversation

object ConversationLabels {
    val publicChannels: LinkedHashMap<String, String> = linkedMapOf(
        "SAY" to "说话",
        "SHOUT" to "喊话",
        "YELL" to "呼喊",
        "PARTY" to "小队",
        "ALLIANCE" to "团队",
        "FREE_COMPANY" to "部队",
        "LINKSHELL_1" to "通讯贝 1",
        "LINKSHELL_2" to "通讯贝 2",
        "LINKSHELL_3" to "通讯贝 3",
        "LINKSHELL_4" to "通讯贝 4",
        "LINKSHELL_5" to "通讯贝 5",
        "LINKSHELL_6" to "通讯贝 6",
        "LINKSHELL_7" to "通讯贝 7",
        "LINKSHELL_8" to "通讯贝 8",
        "CROSS_LINKSHELL_1" to "跨服通讯贝 1",
        "CROSS_LINKSHELL_2" to "跨服通讯贝 2",
        "CROSS_LINKSHELL_3" to "跨服通讯贝 3",
        "CROSS_LINKSHELL_4" to "跨服通讯贝 4",
        "CROSS_LINKSHELL_5" to "跨服通讯贝 5",
        "CROSS_LINKSHELL_6" to "跨服通讯贝 6",
        "CROSS_LINKSHELL_7" to "跨服通讯贝 7",
        "CROSS_LINKSHELL_8" to "跨服通讯贝 8",
        "NOVICE_NETWORK" to "新人频道",
        "PVP_TEAM" to "PVP 团队",
        "EMOTE" to "情感动作",
    )

    val systemChannels: LinkedHashMap<String, String> = linkedMapOf(
        "SYSTEM" to "系统",
        "ERROR" to "错误",
        "NPC" to "NPC",
        "BATTLE_LOG" to "战斗日志",
    )

    fun publicLabel(channel: String): String =
        publicChannels[channel] ?: channel

    fun systemLabel(key: String): String =
        systemChannels[key] ?: key

    fun publicOrder(channel: String): Int =
        publicChannels.keys.indexOf(channel).let { if (it < 0) Int.MAX_VALUE else it }

    fun systemOrder(key: String): Int =
        systemChannels.keys.indexOf(key).let { if (it < 0) Int.MAX_VALUE else it }

    fun systemKey(channel: String): String? = when (channel) {
        "SYSTEM" -> "SYSTEM"
        "ERROR" -> "ERROR"
        "NPC" -> "NPC"
        "BATTLE", "LOOT", "PROGRESS" -> "BATTLE_LOG"
        else -> null
    }
}
