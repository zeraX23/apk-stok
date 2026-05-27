package com.zerax23.stokbarcode.presentation.generate

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerax23.stokbarcode.data.local.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateBarcodeScreen(
    onNavigateBack: () -> Unit,
    viewModel: GenerateBarcodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showProductDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Barcode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.generatedBitmaps.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.saveAllAsPng(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.SaveAlt,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Simpan Semua")
                        }
                        Button(
                            onClick = {
                                viewModel.shareBarcode(context, 0)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Form section (scrollable)
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pilih Produk
                ExposedDropdownMenuBox(
                    expanded = showProductDropdown,
                    onExpandedChange = { showProductDropdown = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedProduct?.name
                            ?: "Pilih produk...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produk") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults
                                .TrailingIcon(showProductDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showProductDropdown,
                        onDismissRequest = { showProductDropdown = false }
                    ) {
                        if (products.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Belum ada produk") },
                                onClick = {}
                            )
                        }
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            product.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            product.sku,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectProduct(product)
                                    showProductDropdown = false
                                }
                            )
                        }
                    }
                }

                // Jumlah
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Jumlah:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = {
                            viewModel.setQuantity(uiState.quantity - 1)
                        }
                    ) {
                        Icon(Icons.Default.Remove, "Kurang")
                    }
                    Text(
                        text = uiState.quantity.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 48.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            viewModel.setQuantity(uiState.quantity + 1)
                        }
                    ) {
                        Icon(Icons.Default.Add, "Tambah")
                    }
                    Slider(
                        value = uiState.quantity.toFloat(),
                        onValueChange = {
                            viewModel.setQuantity(it.toInt())
                        },
                        valueRange = 1f..100f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Format Barcode
                Text(
                    "Format Barcode:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CODE_128", "QR_CODE").forEach { format ->
                        FilterChip(
                            selected = uiState.barcodeFormat == format,
                            onClick = { viewModel.setBarcodeFormat(format) },
                            label = {
                                Text(
                                    if (format == "CODE_128")
                                        "CODE 128"
                                    else "QR Code"
                                )
                            },
                            leadingIcon = {
                                if (uiState.barcodeFormat == format) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Tombol Generate
                Button(
                    onClick = viewModel::generateBatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = uiState.selectedProduct != null
                            && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Membuat barcode...")
                    } else {
                        Icon(Icons.Default.QrCode2, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Generate ${uiState.quantity} Barcode",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            // Preview Grid
            if (uiState.generatedBitmaps.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "${uiState.generatedBitmaps.size} barcode dibuat",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.generatedBitmaps) { index, bitmap ->
                        BarcodePreviewCard(
                            bitmap = bitmap,
                            serialNumber = uiState.generatedSerials
                                .getOrNull(index)?.serialNumber ?: "",
                            onSave = {
                                viewModel.saveSingleAsPng(context, index)
                            },
                            onShare = {
                                viewModel.shareBarcode(context, index)
                            }
                        )
                    }
                }
            } else if (!uiState.isLoading) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏷️", fontSize = 48.sp)
                        Text(
                            "Pilih produk dan klik Generate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                                .copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodePreviewCard(
    bitmap: Bitmap,
    serialNumber: String,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = serialNumber,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = serialNumber,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.SaveAlt,
                        "Simpan",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        "Bagikan",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
