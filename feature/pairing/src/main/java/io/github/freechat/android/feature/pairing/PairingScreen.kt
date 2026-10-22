package io.github.freechat.android.feature.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.freechat.android.core.ui.components.FreeChatSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingRoute(
    onConnected: () -> Unit,
    onBack: () -> Unit,
    initialLink: String? = null,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(initialLink) {
        if (!initialLink.isNullOrBlank()) viewModel.parseLink(initialLink)
    }
    LaunchedEffect(state.connected) {
        if (state.connected) {
            viewModel.consumeConnected()
            onConnected()
        }
    }

    if (showScanner) {
        QrScannerScreen(
            onResult = { value ->
                viewModel.parseLink(value)
                showScanner = false
            },
            onClose = { showScanner = false },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("连接服务器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "使用 Dalamud 插件生成的二维码/深链，或手动填写连接信息。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item { FreeChatSectionHeader("快速配对", Modifier.padding(top = 8.dp)) }
                item {
                    OutlinedTextField(
                        value = state.link,
                        onValueChange = viewModel::updateLink,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("配对深链") },
                        placeholder = { Text("freechat://pair?...") },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.parseLink(state.link) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text("解析深链")
                        }
                        OutlinedButton(
                            onClick = { showScanner = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("扫码")
                        }
                    }
                }

                item { FreeChatSectionHeader("手动连接") }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = state.host,
                                onValueChange = viewModel::updateHost,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("服务器地址") },
                                placeholder = { Text("192.168.1.10") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                            )
                            OutlinedTextField(
                                value = state.port,
                                onValueChange = viewModel::updatePort,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("端口") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                            )
                            OutlinedTextField(
                                value = state.serverId,
                                onValueChange = viewModel::updateServerId,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("服务器 ID（sid）") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                            )
                            OutlinedTextField(
                                value = state.fingerprint,
                                onValueChange = viewModel::updateFingerprint,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("公钥指纹（fp）") },
                                placeholder = { Text("64 位十六进制") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                            )
                            OutlinedTextField(
                                value = state.code,
                                onValueChange = viewModel::updateCode,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("配对码（首次配对）") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                ),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                            )
                        }
                    }
                }

                state.error?.let { error ->
                    item {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                item {
                    Button(
                        onClick = viewModel::connect,
                        enabled = !state.isConnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        if (state.isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("连接")
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
    }
}
