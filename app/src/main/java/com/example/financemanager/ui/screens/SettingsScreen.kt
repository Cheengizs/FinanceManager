package com.example.financemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financemanager.R
import com.example.financemanager.data.local.FinanceDao
import com.example.financemanager.viewmodel.FinanceViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    viewModel: FinanceViewModel,
    dao: FinanceDao,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

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

        Text(stringResource(id = R.string.display_currency), fontSize = 18.sp)
        val selectedCurrency by viewModel.selectedCurrency.collectAsState()
        var expanded by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onNavigateBack) { Text(stringResource(id = R.string.btn_back)) }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    dao.clearAll()
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
        ) {
            Text(stringResource(id = R.string.btn_logout), color = Color.White)
        }
    }
}