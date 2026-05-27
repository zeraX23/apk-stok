package com.zerax23.stokbarcode.presentation.scan

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.zerax23.stokbarcode.domain.model.ScanResult
import com.zerax23.stokbarcode.util.CurrencyFormatter
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanToSellScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
            if (uiState.isSuccess) {
                viewModel.resetScan()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cameraPermission.status.isGranted) {
                // Camera Preview fullscreen
                CameraPreview(
                    onBarcodeDetected = viewModel::onBarcodeDetected,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay UI di atas kamera
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "Kembali",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Scan Barcode",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Scan frame di tengah
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .border(
                                    3.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Hint di bawah
                    if (uiState.isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Arahkan kamera ke barcode produk",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Loading indicator
                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                // Result Bottom Sheet
                uiState.scanResult?.let { result ->
                    ScanResultBottomSheet(
                        scanResult = result,
                        buyerName = uiState.buyerName,
                        soldPrice = uiState.soldPrice,
                        onBuyerNameChange = viewModel::onBuyerNameChange,
                        onSoldPriceChange = viewModel::onSoldPriceChange,
                        onConfirmSold = viewModel::confirmSold,
                        onDismiss = viewModel::resetScan,
                        isProcessing = uiState.isProcessing
                    )
                }

            } else {
                // Permission denied state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("📷", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Izin Kamera Diperlukan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aplikasi membutuhkan akses kamera untuk scan barcode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            cameraPermission.launchPermissionRequest()
                        }
                    ) {
                        Text("Izinkan Kamera")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue
                                            ?.let { onBarcodeDetected(it) }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
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
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    scanResult: ScanResult,
    buyerName: String,
    soldPrice: String,
    onBuyerNameChange: (String) -> Unit,
    onSoldPriceChange: (String) -> Unit,
    onConfirmSold: () -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (scanResult) {
                is ScanResult.Available -> {
                    // Header hijau
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Produk Ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                "Status: Tersedia",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Info produk
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                scanResult.product.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "SKU: ${scanResult.product.sku}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Serial: ${scanResult.serial.serialNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Sisa stok: ${scanResult.remainingStock} unit",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            scanResult.product.price?.let {
                                Text(
                                    "Harga: ${CurrencyFormatter.formatRupiah(it)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Input nama pembeli
                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = onBuyerNameChange,
                        label = { Text("Nama Pembeli (opsional)") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Input harga jual
                    OutlinedTextField(
                        value = soldPrice,
                        onValueChange = onSoldPriceChange,
                        label = { Text("Harga Jual") },
                        leadingIcon = {
                            Text(
                                "Rp",
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Tombol konfirmasi
                    Button(
                        onClick = onConfirmSold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.ShoppingCart, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "✅ Tandai Terjual",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batal")
                    }
                }

                is ScanResult.AlreadySold -> {
                    // Header merah
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Sudah Terjual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Produk ini sudah ditandai terjual",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error
                                .copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                scanResult.product.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Serial: ${scanResult.serial.serialNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Terjual: ${
                                    CurrencyFormatter.formatDateTime(
                                        scanResult.soldAt
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall
                            )
                            scanResult.serial.buyerName?.let {
                                Text(
                                    "Pembeli: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup")
                    }
                }

                is ScanResult.NotFound -> {
                    // Header abu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Barcode Tidak Ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                scanResult.scannedCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        "Barcode ini tidak terdaftar di sistem. " +
                        "Pastikan barcode sudah digenerate terlebih dahulu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Ulang")
                    }
                }

                is ScanResult.Returned -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Produk Diretur",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
