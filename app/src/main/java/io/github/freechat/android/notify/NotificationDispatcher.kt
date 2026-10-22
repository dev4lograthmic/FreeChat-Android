package io.github.freechat.android.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.freechat.android.MainActivity
import io.github.freechat.android.R
import io.github.freechat.android.service.FreeChatService
import io.github.freechat.android.core.data.store.SettingsStore
import io.github.freechat.android.core.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class NotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TELL,
                context.getString(R.string.notif_channel_tell),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.notif_channel_tell_desc) },
        )
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PARTY,
                context.getString(R.string.notif_channel_party),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_party_desc) },
        )
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LINKSHELL,
                context.getString(R.string.notif_channel_linkshell),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_linkshell_desc) },
        )
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SYSTEM,
                context.getString(R.string.notif_channel_system),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.notif_channel_system_desc) },
        )
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONNECTION,
                context.getString(R.string.notif_channel_connection),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.notif_channel_connection_desc) },
        )
        system.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.notif_channel_service),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_service_desc)
                setShowBadge(false)
            },
        )
    }

    suspend fun notifyMessage(message: ChatMessage) {
        if (message.outgoing) return
        val settings = settingsStore.settings.first()
        val channelId = when {
            message.channel == "TELL" && settings.notifyPrivate -> CHANNEL_TELL
            message.channel == "PARTY" && settings.notifyParty -> CHANNEL_PARTY
            else -> return
        }
        if (!manager.areNotificationsEnabled()) return

        val hide = settings.lockScreenHideContent
        val title = if (hide) context.getString(R.string.notif_new_message_title) else message.senderName
        val body = if (hide) context.getString(R.string.notif_new_message_body) else message.text
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CONVERSATION_ID, message.conversationId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            message.conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_CHAT)
            .apply {
                if (hide) setVisibility(NotificationCompat.VISIBILITY_SECRET)
            }
            .build()

        manager.notify(message.conversationId.hashCode(), notification)
    }

    fun buildServiceNotification(text: String): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(context, FreeChatService::class.java).apply {
            action = FreeChatService.ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(0, context.getString(R.string.service_stop), stopPending)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val CHANNEL_TELL = "chat_tell"
        const val CHANNEL_PARTY = "chat_party"
        const val CHANNEL_LINKSHELL = "chat_linkshell"
        const val CHANNEL_SYSTEM = "chat_system"
        const val CHANNEL_CONNECTION = "connection"
        const val CHANNEL_SERVICE = "service"
        private const val GROUP_CHAT = "chat"
    }
}
