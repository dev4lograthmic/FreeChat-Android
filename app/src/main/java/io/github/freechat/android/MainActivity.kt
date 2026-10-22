package io.github.freechat.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.data.store.AppSettings
import io.github.freechat.android.core.data.store.SettingsStore
import io.github.freechat.android.core.model.ThemeMode
import io.github.freechat.android.service.FreeChatService
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import io.github.freechat.android.core.ui.theme.FreeChatTheme
import io.github.freechat.android.core.ui.locale.LocaleController
import io.github.freechat.android.notify.NotificationDispatcher

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var settingsStore: SettingsStore

    private var deepLink by mutableStateOf<String?>(null)
    private var conversationId by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        enableEdgeToEdge()
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FreeChatTheme(
                darkTheme = darkTheme,
            ) {
                FreeChatApp(
                    deepLink = deepLink,
                    conversationId = conversationId,
                )
            }
        }

        window.decorView.post { requestNotificationPermissionIfNeeded() }

        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                serverRepository.activeProfile.first()
            }
            if (profile != null) {
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
