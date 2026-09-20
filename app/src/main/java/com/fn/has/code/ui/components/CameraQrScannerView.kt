package com.fn.has.code.ui.components

import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraQrScannerView(
    modifier: Modifier = Modifier,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            if (!scanned) {
                                val result = scanImageForQr(imageProxy)
                                if (result != null) {
                                    scanned = true
                                    onQrScanned(result)
                                }
                            }
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun scanImageForQr(imageProxy: ImageProxy): String? {
    val mediaImage = imageProxy.image ?: return null
    val buffer = mediaImage.planes[0].buffer
    val bytes = byteBufferToByteArray(buffer)
    val width = mediaImage.width
    val height = mediaImage.height

    val source = PlanarYUVLuminanceSource(
        bytes, width, height, 0, 0, width, height, false
    )
    val bitmap = BinaryBitmap(HybridBinarizer(source))

    return try {
        val reader = MultiFormatReader()
        val result = reader.decode(bitmap)
        result.text
    } catch (e: Exception) {
        null
    }
}

private fun byteBufferToByteArray(buffer: ByteBuffer): ByteArray {
    buffer.rewind()
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    return data
}
