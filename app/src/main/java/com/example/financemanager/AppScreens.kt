package com.example.financemanager

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MainScreen(
    dao: FinanceDao,
    viewModel: FinanceViewModel,
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current

    val rawItems by dao.getAllItems().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val currentRates by viewModel.rates.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val filterType by viewModel.filterType.collectAsState()

    val items = viewModel.processItems(rawItems, searchQuery, sortType, filterType)

    val balanceByn = rawItems.sumOf { if (it.isIncome) it.amount else -it.amount }
    val displayBalance = viewModel.convert(balanceByn)
    val balanceColor = if (balanceByn >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

    val currentDate =
        remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) }

    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(currentDate) }
    var txCurrency by remember { mutableStateOf(selectedCurrency) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    var itemToDelete by remember { mutableStateOf<TransactionItem?>(null) }
    var itemToEdit by remember { mutableStateOf<TransactionItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? -> selectedImageUri = uri?.toString() }
    )

    fun resetFields() {
        title = ""
        amountStr = ""
        date = currentDate
        isIncome = false
        txCurrency = selectedCurrency
        selectedImageUri = null
        itemToEdit = null
        showDialog = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                resetFields()
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isOnline) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isOnline) stringResource(id = R.string.status_online) else stringResource(
                            id = R.string.status_offline
                        ),
                        color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(onClick = onNavigateToSettings) { Text(stringResource(id = R.string.btn_settings)) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(id = R.string.current_balance),
                    fontSize = 18.sp,
                    color = Color.Gray
                )
                Text(
                    text = String.format("%.2f %s", displayBalance, selectedCurrency),
                    fontSize = 40.sp, fontWeight = FontWeight.Bold, color = balanceColor
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text(stringResource(id = R.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { viewModel.updateSortType(SortType.DATE_DESC) }) {
                        Text(
                            stringResource(id = R.string.sort_new)
                        )
                    }
                    TextButton(onClick = { viewModel.updateSortType(SortType.DATE_ASC) }) {
                        Text(
                            stringResource(id = R.string.sort_old)
                        )
                    }
                    TextButton(onClick = { viewModel.updateSortType(SortType.AMOUNT_DESC) }) {
                        Text(
                            stringResource(id = R.string.sort_expensive)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { viewModel.updateFilterType("ALL") },
                        label = { Text(stringResource(id = R.string.filter_all)) }
                    )
                    FilterChip(
                        selected = filterType == "INCOME",
                        onClick = { viewModel.updateFilterType("INCOME") },
                        label = { Text(stringResource(id = R.string.filter_income)) }
                    )
                    FilterChip(
                        selected = filterType == "EXPENSE",
                        onClick = { viewModel.updateFilterType("EXPENSE") },
                        label = { Text(stringResource(id = R.string.filter_expense)) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    val itemColor = if (item.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val sign = if (item.isIncome) "+" else "-"
                    val convertedAmount = viewModel.convert(item.amount)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onNavigateToDetails(item.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = item.date, color = Color.Gray, fontSize = 14.sp)
                                if (item.imageUrl != null) {
                                    Text(
                                        stringResource(id = R.string.photo_attached),
                                        color = Color.Blue,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = String.format(
                                    "%s%.2f %s",
                                    sign,
                                    convertedAmount,
                                    selectedCurrency
                                ),
                                color = itemColor, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            IconButton(onClick = {
                                itemToEdit = item
                                title = item.title
                                amountStr = item.amount.toString()
                                date = item.date
                                isIncome = item.isIncome
                                selectedImageUri = item.imageUrl
                                txCurrency = "BYN"
                                showDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Gray
                                )
                            }
                            IconButton(onClick = { itemToDelete = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { resetFields() },
            title = {
                Text(
                    if (itemToEdit == null) stringResource(id = R.string.dialog_new_title) else stringResource(
                        id = R.string.dialog_edit_title
                    )
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(stringResource(id = R.string.title_hint)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = amountStr, onValueChange = { amountStr = it },
                            label = { Text(stringResource(id = R.string.amount_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        var txExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { txExpanded = true }) { Text(txCurrency) }
                            DropdownMenu(
                                expanded = txExpanded,
                                onDismissRequest = { txExpanded = false }) {
                                viewModel.currencies.forEach { currency ->
                                    DropdownMenuItem(
                                        text = { Text(currency) },
                                        onClick = { txCurrency = currency; txExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = date, onValueChange = { date = it },
                        label = { Text(stringResource(id = R.string.date_hint)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text(
                            if (selectedImageUri == null) stringResource(id = R.string.btn_attach_photo) else stringResource(
                                id = R.string.photo_ready
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isIncome) stringResource(id = R.string.income_plus) else stringResource(
                                id = R.string.expense_minus
                            ),
                            color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                        )
                        Switch(checked = isIncome, onCheckedChange = { isIncome = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val inputAmount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && inputAmount > 0) {
                            val amountByn = viewModel.convertToByn(inputAmount, txCurrency)

                            var finalImageUrl: String? = selectedImageUri

                            if (selectedImageUri != null && selectedImageUri!!.startsWith("content://")) {
                                try {
                                    val uri = Uri.parse(selectedImageUri)
                                    val bytes =
                                        context.contentResolver.openInputStream(uri)?.readBytes()
                                    val base64Image = android.util.Base64.encodeToString(
                                        bytes,
                                        android.util.Base64.NO_WRAP
                                    )

                                    val privateKey = "private_V2HUWL5M2TWjrJZqt3tLGASz8i4=:"
                                    val authHeader = "Basic " + android.util.Base64.encodeToString(
                                        privateKey.toByteArray(),
                                        android.util.Base64.NO_WRAP
                                    )

                                    val response = ImageKitClient.api.uploadImage(
                                        authHeader = authHeader,
                                        fileBase64 = base64Image!!,
                                        fileName = "receipt_${System.currentTimeMillis()}.jpg"
                                    )

                                    finalImageUrl = response.url
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            val newItem = TransactionItem(
                                id = itemToEdit?.id ?: 0,
                                title = title,
                                amount = amountByn,
                                isIncome = isIncome,
                                date = date,
                                imageUrl = finalImageUrl
                            )

                            if (itemToEdit == null) dao.insert(newItem) else dao.update(newItem)

                            try {
                                val db = FirebaseFirestore.getInstance()
                                val remoteData = hashMapOf(
                                    "title" to newItem.title,
                                    "amount" to newItem.amount,
                                    "isIncome" to newItem.isIncome,
                                    "date" to newItem.date,
                                    "imageUrl" to newItem.imageUrl
                                )
                                db.collection("transactions").add(remoteData)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            resetFields()
                        }
                    }
                }) { Text(stringResource(id = R.string.btn_save)) }
            },
            dismissButton = { TextButton(onClick = { resetFields() }) { Text(stringResource(id = R.string.btn_cancel)) } }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(id = R.string.delete_title)) },
            text = { Text(stringResource(id = R.string.delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = { coroutineScope.launch { dao.delete(item); itemToDelete = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text(stringResource(id = R.string.btn_delete), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemToDelete = null
                }) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun DetailsScreen(
    itemId: Int,
    dao: FinanceDao,
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val item by produceState<TransactionItem?>(initialValue = null) {
        value = dao.getItemById(itemId)
    }
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item?.let {
            val itemColor = if (it.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
            val typeText =
                if (it.isIncome) stringResource(id = R.string.type_income) else stringResource(id = R.string.type_expense)
            val convertedAmount = viewModel.convert(it.amount)

            Text(text = typeText, color = itemColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it.title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = String.format("%.2f %s", convertedAmount, selectedCurrency),
                fontSize = 48.sp, color = itemColor, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.operation_date, it.date),
                color = Color.Gray,
                fontSize = 18.sp
            )

            it.imageUrl?.let { uri ->
                Spacer(modifier = Modifier.height(24.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = "Receipt Image",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp)
                )
            }
        } ?: Text(stringResource(id = R.string.loading))

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack) { Text(stringResource(id = R.string.btn_back)) }
    }
}

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(id = R.string.settings_title), fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.dark_theme), fontSize = 18.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = isDarkTheme, onCheckedChange = { onThemeChange(it) })
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack) { Text(stringResource(id = R.string.btn_back)) }
        Spacer(modifier = Modifier.height(32.dp))

        Text(stringResource(id = R.string.display_currency), fontSize = 18.sp)
        val selectedCurrency by viewModel.selectedCurrency.collectAsState()
        var expanded by remember { mutableStateOf(false) }
        val currentRates by viewModel.rates.collectAsState()

        Box(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { expanded = true }) { Text(selectedCurrency) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                viewModel.currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = { viewModel.changeCurrency(currency); expanded = false }
                    )
                }
            }
        }
    }
}