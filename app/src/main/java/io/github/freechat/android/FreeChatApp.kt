package io.github.freechat.android

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.freechat.android.core.ui.R
import io.github.freechat.android.feature.chat.ChatRoute
import io.github.freechat.android.feature.chat.ConversationRoute
import io.github.freechat.android.feature.chat.FriendsRoute
import io.github.freechat.android.feature.pairing.PairingRoute
import io.github.freechat.android.feature.settings.AboutScreen
import io.github.freechat.android.feature.settings.DeveloperOptionsRoute
import io.github.freechat.android.feature.settings.MeRoute
import io.github.freechat.android.feature.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val CHAT = "chat"
    const val FRIENDS = "friends"
    const val ME = "me"
    const val SETTINGS = "settings"
    const val DEVELOPER = "developer"
    const val ABOUT = "about"
    const val CONVERSATION = "conversation/{conversationId}"
    const val PAIRING = "pairing?link={link}"

    fun conversation(conversationId: String): String =
        "conversation/${Uri.encode(conversationId)}"

    fun pairing(link: String? = null): String =
        "pairing?link=${Uri.encode(link.orEmpty())}"
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private fun secondaryEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(350, easing = EmphasizedDecelerate),
    )

private fun secondaryExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(300, easing = EmphasizedAccelerate),
    )

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.CHAT, R.string.nav_chat, Icons.AutoMirrored.Filled.Chat),
    TopLevelDestination(Routes.FRIENDS, R.string.nav_friends, Icons.Default.Group),
    TopLevelDestination(Routes.ME, R.string.nav_me, Icons.Default.Person),
)

/**
 * Root navigation: the three main tabs live in [MainTabs]. Conversation,
 * settings and pairing are full-screen layers above the tabs, so they cover
 * the bottom bar / rail while they slide in.
 */
@Composable
fun FreeChatApp(deepLink: String? = null, conversationId: String? = null) {
    val navController = rememberNavController()

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

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        enterTransition = { secondaryEnter() },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { secondaryExit() },
    ) {
        composable(
            route = Routes.MAIN,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            MainTabs(
                onOpenConversation = { conversation ->
                    navController.navigate(Routes.conversation(conversation)) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onOpenPairing = { link ->
                    navController.navigate(Routes.pairing(link)) {
                        launchSingleTop = true
                    }
                },
                onOpenAbout = {
                    navController.navigate(Routes.ABOUT) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
            ),
            enterTransition = { secondaryEnter() },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { secondaryExit() },
        ) {
            ConversationRoute(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SETTINGS,
            enterTransition = { secondaryEnter() },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { secondaryExit() },
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDeveloperOptions = {
                    navController.navigate(Routes.DEVELOPER) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.DEVELOPER,
            enterTransition = { secondaryEnter() },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { secondaryExit() },
        ) {
            DeveloperOptionsRoute(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.ABOUT,
            enterTransition = { secondaryEnter() },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { secondaryExit() },
        ) {
            AboutScreen(
                onBack = { navController.popBackStack() },
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
            enterTransition = { secondaryEnter() },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { secondaryExit() },
        ) { entry ->
            PairingRoute(
                initialLink = entry.arguments
                    ?.getString("link")
                    ?.let { Uri.decode(it) },
                onBack = { navController.popBackStack() },
                onConnected = {
                    navController.popBackStack(Routes.MAIN, inclusive = false)
                },
            )
        }
    }
}

@Composable
private fun MainTabs(
    onOpenConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPairing: (String?) -> Unit,
    onOpenAbout: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val selectedRoute = when (route) {
        Routes.FRIENDS -> Routes.FRIENDS
        Routes.ME -> Routes.ME
        else -> Routes.CHAT
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                    ) {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = selectedRoute == destination.route,
                                onClick = { tabNavController.navigateTab(destination.route) },
                                icon = {
                                    Icon(destination.icon, contentDescription = null)
                                },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Spacer(Modifier.height(8.dp))
                        topLevelDestinations.forEach { destination ->
                            NavigationRailItem(
                                selected = selectedRoute == destination.route,
                                onClick = { tabNavController.navigateTab(destination.route) },
                                icon = {
                                    Icon(destination.icon, contentDescription = null)
                                },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
                NavHost(
                    navController = tabNavController,
                    startDestination = Routes.CHAT,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    composable(Routes.CHAT) {
                        ChatRoute(
                            onOpenPairing = { onOpenPairing(null) },
                            onOpenConversation = onOpenConversation,
                        )
                    }
                    composable(Routes.FRIENDS) {
                        FriendsRoute(
                            onOpenPairing = { onOpenPairing(null) },
                            onOpenConversation = onOpenConversation,
                        )
                    }
                    composable(Routes.ME) {
                        MeRoute(
                            onOpenSettings = onOpenSettings,
                            onOpenPairing = { onOpenPairing(null) },
                            onOpenAbout = onOpenAbout,
                        )
                    }
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
