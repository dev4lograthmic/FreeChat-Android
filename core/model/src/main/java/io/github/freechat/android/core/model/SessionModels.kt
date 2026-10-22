package io.github.freechat.android.core.model

enum class ChatSourceOption {
    PRE_HANDLE,
    POST_HANDLE,
    STRUCTURED,
}

enum class MessageContentSource {
    TEXT,
    ORIGINAL_TEXT,
    RAW_MESSAGE,
    ORIGINAL_RAW_MESSAGE,
    ;

    val needsOriginal: Boolean
        get() = this == ORIGINAL_TEXT || this == ORIGINAL_RAW_MESSAGE
}

data class SessionInfo(
    val serverId: String,
    val serverName: String,
    val characterName: String,
    val worldName: String,
    val sessionId: String,
    val enabledFeatures: List<String>,
)

enum class SendOutcomeStatus {
    SUBMITTED,
    FAILED,
    BLOCKED,
    RATE_LIMITED,
    QUEUE_FULL,
    CANCELLED,
    UNKNOWN,
}

data class SendOutcome(
    val status: SendOutcomeStatus,
    val errorCode: String?,
    val message: String,
    val partIndex: Int,
    val partCount: Int,
    val originMessageId: String?,
)
