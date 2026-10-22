package io.github.freechat.android.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.freechat.android.core.model.ConnectionStatus
import io.github.freechat.android.core.ui.R

@Composable
fun ConnectionStatusPill(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.ONLINE -> MaterialTheme.colorScheme.primaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
            ConnectionStatus.OFFLINE -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "connectionContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.ONLINE -> MaterialTheme.colorScheme.onPrimaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
            ConnectionStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "connectionContent",
    )
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = stringResource(
                when (status) {
                    ConnectionStatus.ONLINE -> R.string.common_connected
                    ConnectionStatus.CONNECTING -> R.string.common_connecting
                    ConnectionStatus.OFFLINE -> R.string.common_not_connected
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
