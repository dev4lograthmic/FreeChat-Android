package io.github.freechat.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.service.FreeChatService
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import io.github.freechat.android.core.ui.theme.FreeChatTheme
import io.github.freechat.android.notify.NotificationDispatcher

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var serverRepository: ServerRepository

    private var deepLink by mutableStateOf<String?>(null)
    private var conversationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            FreeChatTheme {
                FreeChatApp(
                    deepLink = deepLink,
                    conversationId = conversationId,
                )
            }
        }

        lifecycleScope.launch {
            if (serverRepository.activeProfile.first() != null) {
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, FreeChatService::class.java),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        deepLink = intent?.dataString
        conversationId = intent?.getStringExtra(NotificationDispatcher.EXTRA_CONVERSATION_ID)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }
}
