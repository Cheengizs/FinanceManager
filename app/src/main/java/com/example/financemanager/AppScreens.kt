package com.example.financemanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit // Добавили иконку карандаша
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(id = R.string.app_name), fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MainScreen(
    dao: FinanceDao,
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val items by dao.getAllItems().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val balance = items.sumOf { if (it.isIncome) it.amount else -it.amount }
    val balanceColor = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

    val currentDate = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) }

    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(currentDate) }

    // Переменные для отслеживания действий
    var itemToDelete by remember { mutableStateOf<TransactionItem?>(null) }
    var itemToEdit by remember { mutableStateOf<TransactionItem?>(null) } // Для редактирования

    // Функция для очистки полей
    fun resetFields() {
        title = ""
        amountStr = ""
        date = currentDate
        isIncome = false
        itemToEdit = null
        showDialog = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                resetFields() // Очищаем поля перед добавлением новой
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onNavigateToSettings) { Text(stringResource(id = R.string.btn_settings)) }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(id = R.string.current_balance), fontSize = 18.sp, color = Color.Gray)
                Text(
                    text = String.format("%.2f ₽", balance),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(stringResource(id = R.string.transaction_history), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    val itemColor = if (item.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val sign = if (item.isIncome) "+" else "-"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onNavigateToDetails(item.id) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(text = item.date, color = Color.Gray, fontSize = 14.sp)
                            }
                            Text(
                                text = "$sign${item.amount} ₽",
                                color = itemColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            // Кнопка РЕДАКТИРОВАТЬ
                            IconButton(
                                onClick = {
                                    itemToEdit = item // Запоминаем, что редактируем
                                    // Заполняем поля старыми данными
                                    title = item.title
                                    amountStr = item.amount.toString()
                                    date = item.date
                                    isIncome = item.isIncome
                                    showDialog = true // Открываем окно
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                            }

                            // Кнопка УДАЛИТЬ
                            IconButton(onClick = { itemToDelete = item }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // ДИАЛОГ ДОБАВЛЕНИЯ / РЕДАКТИРОВАНИЯ
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { resetFields() },
            // Меняем заголовок в зависимости от режима
            title = {
                Text(if (itemToEdit == null) stringResource(id = R.string.new_transaction)
                else stringResource(id = R.string.edit_transaction))
            },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(id = R.string.title_hint)) })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(stringResource(id = R.string.amount_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(stringResource(id = R.string.date_hint)) })
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isIncome) stringResource(id = R.string.income_plus) else stringResource(id = R.string.expense_minus),
                            color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = isIncome, onCheckedChange = { isIncome = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amount > 0) {
                            if (itemToEdit == null) {
                                // Если создаем новую
                                dao.insert(TransactionItem(title = title, amount = amount, isIncome = isIncome, date = date))
                            } else {
                                // Если редактируем старую (копируем ID из старой записи)
                                dao.update(itemToEdit!!.copy(title = title, amount = amount, isIncome = isIncome, date = date))
                            }
                            resetFields()
                        }
                    }
                }) { Text(stringResource(id = R.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { resetFields() }) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }

    // ДИАЛОГ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(id = R.string.delete_title)) },
            text = { Text(stringResource(id = R.string.delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            dao.delete(item)
                            itemToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text(stringResource(id = R.string.btn_delete), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }
}

// ... Экраны DetailsScreen и SettingsScreen остаются без изменений (они в порядке) ...
@Composable
fun DetailsScreen(itemId: Int, dao: FinanceDao, onNavigateBack: () -> Unit) {
    val item by produceState<TransactionItem?>(initialValue = null) {
        value = dao.getItemById(itemId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item?.let {
            val itemColor = if (it.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
            val typeText = if (it.isIncome) stringResource(id = R.string.type_income) else stringResource(id = R.string.type_expense)

            Text(text = typeText, color = itemColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it.title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "${it.amount} ₽", fontSize = 48.sp, color = itemColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = stringResource(id = R.string.operation_date, it.date), color = Color.Gray, fontSize = 18.sp)
        } ?: Text(stringResource(id = R.string.loading))

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack) { Text(stringResource(id = R.string.btn_back)) }
    }
}

@Composable
fun SettingsScreen(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit, onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = stringResource(id = R.string.settings_title), fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.dark_theme), fontSize = 18.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = isDarkTheme, onCheckedChange = { onThemeChange(it) })
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack) { Text(stringResource(id = R.string.btn_back)) }
    }
}