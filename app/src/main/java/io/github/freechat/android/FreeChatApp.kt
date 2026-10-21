package io.github.freechat.android

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.freechat.android.feature.chat.ChatRoute
import io.github.freechat.android.feature.chat.ConversationRoute
import io.github.freechat.android.feature.chat.FriendsScreen
import io.github.freechat.android.feature.pairing.PairingRoute
import io.github.freechat.android.feature.settings.MeRoute
import io.github.freechat.android.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val CHAT = "chat"
    const val FRIENDS = "friends"
    const val ME = "me"
    const val SETTINGS = "settings"
    const val CONVERSATION = "conversation/{conversationId}"
    const val PAIRING = "pairing?link={link}"

    fun conversation(conversationId: String): String =
        "conversation/${Uri.encode(conversationId)}"

    fun pairing(link: String? = null): String =
        "pairing?link=${Uri.encode(link.orEmpty())}"
}

@Composable
fun FreeChatApp(deepLink: String? = null, conversationId: String? = null) {
    val navController = rememberNavController()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed,
    )
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBottomBar = route != null &&
        !route.startsWith("pairing") &&
        !route.startsWith("conversation")

    LaunchedEffect(deepLink) {
        if (!deepLink.isNullOrBlank()) {
            navController.navigate(Routes.pairing(deepLink)) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(conversationId) {
        if (!conversationId.isNullOrBlank()) {
            navController.navigate(Routes.conversation(conversationId)) {
                launchSingleTop = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = "FreeChat",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp),
                )
                NavigationDrawerItem(
                    label = { Text("连接服务器") },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.pairing())
                    },
                )
                NavigationDrawerItem(
                    label = { Text("设置") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = route == Routes.SETTINGS,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.SETTINGS)
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                    ) {
                        NavigationBarItem(
                            selected = route == Routes.CHAT,
                            onClick = { navController.navigateTab(Routes.CHAT) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                            label = { Text("聊天") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                        NavigationBarItem(
                            selected = route == Routes.FRIENDS,
                            onClick = { navController.navigateTab(Routes.FRIENDS) },
                            icon = { Icon(Icons.Default.Group, contentDescription = null) },
                            label = { Text("好友") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                        NavigationBarItem(
                            selected = route == Routes.ME || route == Routes.SETTINGS,
                            onClick = { navController.navigateTab(Routes.ME) },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("我") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.CHAT,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.CHAT) {
                    ChatRoute(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenPairing = { navController.navigate(Routes.pairing()) },
                        onOpenConversation = { navController.navigate(Routes.conversation(it)) },
                    )
                }
                composable(Routes.FRIENDS) {
                    FriendsScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenPairing = { navController.navigate(Routes.pairing()) },
                        onOpenConversation = { navController.navigate(Routes.conversation(it)) },
                    )
                }
                composable(Routes.ME) {
                    MeRoute(
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenPairing = { navController.navigate(Routes.pairing()) },
                    )
                }
                composable(
                    route = Routes.CONVERSATION,
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                    ),
                ) {
                    ConversationRoute(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPairing = { navController.navigate(Routes.pairing()) },
                    )
                }
                composable(
                    route = Routes.PAIRING,
                    arguments = listOf(
                        navArgument("link") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    PairingRoute(
                        initialLink = entry.arguments?.getString("link")?.let { Uri.decode(it) },
                        onBack = { navController.popBackStack() },
                        onConnected = {
                            navController.navigate(Routes.CHAT) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
