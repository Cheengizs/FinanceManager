package com.example.financemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.financemanager.R
import com.example.financemanager.data.local.FinanceDao
import com.example.financemanager.data.local.TransactionItem
import com.example.financemanager.viewmodel.FinanceViewModel

@Composable
fun DetailsScreen(
    itemId: String,
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