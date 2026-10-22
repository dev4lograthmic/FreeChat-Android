package io.github.freechat.android.feature.pairing

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("需要相机权限才能扫描二维码", color = Color.White)
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("授权相机")
                }
            }
        } else {
            CameraPreview(onResult = onResult)
            QrViewfinderOverlay()
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
        }
        Text(
            "将二维码放入取景框",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun QrViewfinderOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameSize = min(size.width, size.height) * 0.68f
        val left = (size.width - frameSize) / 2f
        val top = (size.height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize
        val cornerRadius = CornerRadius(24.dp.toPx())
        val maskColor = Color.White.copy(alpha = 0.35f)

        val maskPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                ),
            )
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                    ),
                    cornerRadius = cornerRadius,
                ),
            )
        }
        drawPath(path = maskPath, color = maskColor)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = GeometrySize(frameSize, frameSize),
            cornerRadius = cornerRadius,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun CameraPreview(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val scanned = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        var analysis: ImageAnalysis? = null
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)
                    .build()
                analysis = imageAnalysis
                val reader = MultiFormatReader()
                imageAnalysis.setAnalyzer(analysisExecutor) { image ->
                    try {
                        if (!scanned.get()) {
                            val text = decodeQr(image, reader)
                            if (text != null && scanned.compareAndSet(false, true)) {
                                mainExecutor.execute { onResult(text) }
                            }
                        }
                    } finally {
                        image.close()
                    }
                }
                provider?.unbindAll()
                provider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            },
            mainExecutor,
        )

        onDispose {
            analysis?.clearAnalyzer()
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )
}

private val QR_HINTS = mapOf(
    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    DecodeHintType.TRY_HARDER to true,
    DecodeHintType.ALSO_INVERTED to true,
    DecodeHintType.CHARACTER_SET to "UTF-8",
)

private fun decodeQr(image: ImageProxy, reader: MultiFormatReader): String? {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val width = image.width
    val height = image.height
    if (width <= 0 || height <= 0) return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val luminance = ByteArray(width * height)
    val rowStride = plane.rowStride
    for (row in 0 until height) {
        val source = row * rowStride
        if (source + width > bytes.size) break
        System.arraycopy(bytes, source, luminance, row * width, width)
    }
    return runCatching {
        reader.setHints(QR_HINTS)
        val source = PlanarYUVLuminanceSource(
            luminance,
            width,
            height,
            0,
            0,
            width,
            height,
            false,
        )
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    }.getOrNull().also {
        reader.reset()
    }
}
