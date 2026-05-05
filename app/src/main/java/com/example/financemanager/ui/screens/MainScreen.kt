package com.example.financemanager.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financemanager.R
import com.example.financemanager.data.local.FinanceDao
import com.example.financemanager.data.local.TransactionItem
import com.example.financemanager.data.remote.ImageKitClient
import com.example.financemanager.viewmodel.FinanceViewModel
import com.example.financemanager.viewmodel.SortType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dao: FinanceDao,
    viewModel: FinanceViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("transactions")
                .whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    coroutineScope.launch {
                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val item = TransactionItem(
                                id = doc.getString("id") ?: "",
                                userId = currentUser.uid,
                                title = doc.getString("title") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                isIncome = doc.getBoolean("isIncome") ?: false,
                                date = doc.getString("date") ?: "",
                                imageUrl = doc.getString("imageUrl"),
                                location = doc.getString("location")
                            )
                            when (dc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> dao.insert(item)
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> dao.delete(item)
                            }
                        }
                    }
                }
        }
    }

    val rawItems by dao.getAllItems().collectAsState(initial = emptyList())
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val filterType by viewModel.filterType.collectAsState()

    val items = viewModel.processItems(rawItems, searchQuery, sortType, filterType)
    val balanceByn = rawItems.sumOf { if (it.isIncome) it.amount else -it.amount }
    val displayBalance = viewModel.convert(balanceByn)
    val balanceColor = if (balanceByn >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val currentDate = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) }

    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(currentDate) }
    var txCurrency by remember { mutableStateOf(selectedCurrency) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var currentLocationText by remember { mutableStateOf<String?>(null) }
    var itemToEdit by remember { mutableStateOf<TransactionItem?>(null) }
    var itemToDelete by remember { mutableStateOf<TransactionItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? -> selectedImageUri = uri?.toString() }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                viewModel.fetchCurrentLocation(context) { currentLocationText = it }
            }
        }
    )

    fun resetFields() {
        title = ""; amountStr = ""; date = currentDate; isIncome = false
        txCurrency = selectedCurrency; selectedImageUri = null
        currentLocationText = null; itemToEdit = null; showDialog = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isOnline) { resetFields(); showDialog = true }
                else Toast.makeText(context, context.getString(R.string.error_no_internet_read_only), Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isOnline) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isOnline) stringResource(id = R.string.status_online) else stringResource(id = R.string.status_offline), color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.Bold)
                }
                Button(onClick = onNavigateToSettings) { Text(stringResource(id = R.string.btn_settings)) }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(id = R.string.current_balance), fontSize = 18.sp, color = Color.Gray)
                Text(text = String.format("%.2f %s", displayBalance, selectedCurrency), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = balanceColor)
            }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) }, label = { Text(stringResource(id = R.string.search_hint)) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { viewModel.updateSortType(SortType.DATE_DESC) }) { Text(stringResource(id = R.string.sort_new)) }
                    TextButton(onClick = { viewModel.updateSortType(SortType.DATE_ASC) }) { Text(stringResource(id = R.string.sort_old)) }
                    TextButton(onClick = { viewModel.updateSortType(SortType.AMOUNT_DESC) }) { Text(stringResource(id = R.string.sort_expensive)) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = filterType == "ALL", onClick = { viewModel.updateFilterType("ALL") }, label = { Text(stringResource(id = R.string.filter_all)) })
                    FilterChip(selected = filterType == "INCOME", onClick = { viewModel.updateFilterType("INCOME") }, label = { Text(stringResource(id = R.string.filter_income)) })
                    FilterChip(selected = filterType == "EXPENSE", onClick = { viewModel.updateFilterType("EXPENSE") }, label = { Text(stringResource(id = R.string.filter_expense)) })
                }
            }

            HorizontalDivider()

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    val itemColor = if (item.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val convertedAmount = viewModel.convert(item.amount)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onNavigateToDetails(item.id) }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(text = item.date, color = Color.Gray, fontSize = 14.sp)
                                if (!item.location.isNullOrBlank()) Text(text = "📍 ${item.location}", color = Color.Gray, fontSize = 12.sp)
                            }
                            Text(text = String.format("%s%.2f %s", if (item.isIncome) "+" else "-", convertedAmount, selectedCurrency), color = itemColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                            IconButton(onClick = {
                                val shareText = """
                                    FinanceManager Report:
                                    Item: ${item.title}
                                    Amount: ${if(item.isIncome) "+" else "-"}${String.format("%.2f", convertedAmount)} $selectedCurrency
                                    Date: ${item.date}
                                    ${if(!item.location.isNullOrBlank()) "Location: ${item.location}" else ""}
                                """.trimIndent()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share transaction via"))
                            }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray) }

                            IconButton(onClick = { if(isOnline) { itemToEdit = item; title = item.title; amountStr = item.amount.toString(); date = item.date; isIncome = item.isIncome; currentLocationText = item.location; showDialog = true } }) { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                            IconButton(onClick = { if(isOnline) itemToDelete = item }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { resetFields() },
            title = { Text(if (itemToEdit == null) stringResource(id = R.string.dialog_new_title) else stringResource(id = R.string.dialog_edit_title)) },
            text = {
                Column {
                    if (currentLocationText != null) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(text = currentLocationText!!, fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(id = R.string.title_hint)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text(stringResource(id = R.string.amount_hint)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        var txExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { txExpanded = true }) { Text(txCurrency) }
                            DropdownMenu(expanded = txExpanded, onDismissRequest = { txExpanded = false }) {
                                viewModel.currencies.forEach { curr -> DropdownMenuItem(text = { Text(curr) }, onClick = { txCurrency = curr; txExpanded = false }) }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(stringResource(id = R.string.date_hint)) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (selectedImageUri == null) "Photo" else "Ready", fontSize = 12.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (currentLocationText == null) "Location" else "Defined", fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isIncome) stringResource(id = R.string.income_plus) else stringResource(id = R.string.expense_minus), color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = isIncome, onCheckedChange = { isIncome = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val inputAmount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && inputAmount > 0 && currentUser != null) {
                            val amountByn = viewModel.convertToByn(inputAmount, txCurrency)
                            var finalImageUrl = selectedImageUri
                            if (selectedImageUri?.startsWith("content://") == true) {
                                try {
                                    val bytes = context.contentResolver.openInputStream(Uri.parse(selectedImageUri))?.readBytes()
                                    val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    val authHeader = "Basic " + Base64.encodeToString("private_V2HUWL5M2TWjrJZqt3tLGASz8i4=:".toByteArray(), Base64.NO_WRAP)
                                    val response = ImageKitClient.api.uploadImage(authHeader, base64Image!!, "receipt_${System.currentTimeMillis()}.jpg")
                                    finalImageUrl = response.url
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            val txId = itemToEdit?.id ?: UUID.randomUUID().toString()
                            val db = FirebaseFirestore.getInstance()
                            val remoteData = hashMapOf("id" to txId, "userId" to currentUser.uid, "title" to title, "amount" to amountByn, "isIncome" to isIncome, "date" to date, "imageUrl" to finalImageUrl, "location" to currentLocationText)
                            db.collection("transactions").document(txId).set(remoteData).addOnSuccessListener {
                                coroutineScope.launch {
                                    val newItem = TransactionItem(txId, currentUser.uid, title, amountByn, isIncome, date, finalImageUrl, currentLocationText)
                                    if (itemToEdit == null) dao.insert(newItem) else dao.update(newItem)
                                    resetFields()
                                }
                            }
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
                Button(onClick = {
                    FirebaseFirestore.getInstance().collection("transactions").document(item.id).delete().addOnSuccessListener {
                        coroutineScope.launch { dao.delete(item); itemToDelete = null }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { Text(stringResource(id = R.string.btn_delete), color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text(stringResource(id = R.string.btn_cancel)) } }
        )
    }
}