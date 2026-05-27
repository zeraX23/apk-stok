package com.zerax23.stokbarcode.presentation.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerax23.stokbarcode.data.local.entity.BarcodeSerial
import com.zerax23.stokbarcode.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serials by viewModel.serials.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Barcode") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Cari serial number atau produk...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchChange("") }
                        ) {
                            Icon(Icons.Default.Clear, "Hapus")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StatusFilter.values()) { filter ->
                    FilterChip(
                        selected = uiState.statusFilter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text(filter.label) },
                        leadingIcon = {
                            if (uiState.statusFilter == filter) {
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

            Spacer(modifier = Modifier.height(8.dp))

            // Jumlah hasil
            Text(
                text = "${serials.size} barcode ditemukan",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (serials.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = serials,
                        key = { it.id }
                    ) { serial ->
                        SerialCard(
                            serial = serial,
                            onLongPress = {
                                if (serial.status == "SOLD") {
                                    viewModel.showReturnDialog(serial)
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Return Dialog
    if (uiState.showReturnDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReturnDialog,
            icon = {
                Icon(
                    Icons.Default.Undo,
                    null,
                    tint = Color(0xFFE65100)
                )
            },
            title = { Text("Proses Retur") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Yakin ingin meretur produk ini?")
                    uiState.selectedSerial?.let { serial ->
                        Text(
                            serial.serialNumber,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        serial.buyerName?.let {
                            Text(
                                "Pembeli: $it",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        "Status akan berubah menjadi DIRETUR dan stok akan bertambah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::processReturn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE65100)
                    )
                ) {
                    Text("Proses Retur")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReturnDialog) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SerialCard(
    serial: BarcodeSerial,
    onLongPress: () -> Unit
) {
    val statusColor = when (serial.status) {
        "AVAILABLE" -> Color(0xFF2E7D32)
        "SOLD" -> Color(0xFFC62828)
        "RETURNED" -> Color(0xFFE65100)
        else -> Color.Gray
    }

    val statusLabel = when (serial.status) {
        "AVAILABLE" -> "Tersedia"
        "SOLD" -> "Terjual"
        "RETURNED" -> "Diretur"
        else -> serial.status
    }

    val statusIcon = when (serial.status) {
        "AVAILABLE" -> Icons.Default.CheckCircle
        "SOLD" -> Icons.Default.ShoppingCart
        "RETURNED" -> Icons.Default.Undo
        else -> Icons.Default.Help
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Icon
            Icon(
                statusIcon,
                null,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = serial.serialNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Produk #${serial.productId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                        .copy(alpha = 0.6f)
                )
                Text(
                    text = "Dibuat: ${
                        CurrencyFormatter.formatDateTime(serial.generatedAt)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                        .copy(alpha = 0.5f)
                )
                serial.soldAt?.let {
                    Text(
                        text = "Terjual: ${
                            CurrencyFormatter.formatDateTime(it)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
                }
                serial.buyerName?.let {
                    Text(
                        text = "Pembeli: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                serial.soldPrice?.let {
                    Text(
                        text = CurrencyFormatter.formatRupiah(it),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(
                        horizontal = 10.dp, vertical = 4.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔍", fontSize = 64.sp)
            Text(
                "Tidak ada riwayat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Belum ada barcode yang dibuat atau tidak sesuai filter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
