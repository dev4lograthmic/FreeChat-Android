package io.github.freechat.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.freechat.android.R
import io.github.freechat.android.core.ui.R as UiR
import io.github.freechat.android.SessionHolder
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.network.connection.ReconnectPolicy
import io.github.freechat.android.core.ui.locale.LocaleController
import io.github.freechat.android.notify.NotificationDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class FreeChatService : Service() {
    @Inject lateinit var sessionHolder: SessionHolder
    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var notifications: NotificationDispatcher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.wrap(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNow(getString(R.string.service_connecting))
        if (loopJob?.isActive != true) {
            loopJob = scope.launch { connectionLoop() }
        }
        return START_STICKY
    }

    private suspend fun connectionLoop() {
        val policy = ReconnectPolicy()
        var attempt = 0

        while (currentCoroutineContext().isActive) {
            val existing = sessionHolder.current
            val active = if (existing != null) {
                existing
            } else {
                val profile = serverRepository.activeProfile.first()
                if (profile == null) {
                    startForegroundNow(getString(R.string.service_no_server))
                    delay(2_000)
                    stopSelf()
                    return
                }
                try {
                    sessionHolder.connectForService(profile)
                } catch (error: Exception) {
                    startForegroundNow(
                        getString(
                            R.string.service_connect_failed,
                            error.message ?: getString(UiR.string.common_unknown_error),
                        ),
                    )
                    delay(policy.delayForAttempt(attempt++))
                    continue
                }
            }

            attempt = 0
            startForegroundNow(getString(R.string.service_connected))
            val notificationJob = scope.launch {
                active.events.collect { message -> notifications.notifyMessage(message) }
            }
            try {
                active.closed.await()
            } finally {
                notificationJob.cancel()
            }

            sessionHolder.invalidate(active)
            if (!currentCoroutineContext().isActive) break
            startForegroundNow(getString(R.string.service_reconnecting))
            delay(policy.delayForAttempt(attempt++))
        }
    }

    private fun startForegroundNow(text: String) {
        val notification = notifications.buildServiceNotification(text)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    override fun onDestroy() {
        loopJob?.cancel()
        sessionHolder.current?.let { active ->
            sessionHolder.invalidate(active)
            runBlocking { active.close() }
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "io.github.freechat.android.action.STOP"
        const val NOTIFICATION_ID = 1001
    }
}
