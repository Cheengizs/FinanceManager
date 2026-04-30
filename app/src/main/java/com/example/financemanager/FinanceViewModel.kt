package com.example.financemanager

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SortType { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    val currencies = listOf("BYN", "USD", "EUR", "RUB", "CNY", "CHF", "GBP", "TRY", "KZT")

    private val _selectedCurrency = MutableStateFlow("BYN")
    val selectedCurrency = _selectedCurrency.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    private val prefs = application.getSharedPreferences("RatesCache", Context.MODE_PRIVATE)

    private val _rates = MutableStateFlow<Map<String, Double>>(loadRatesFromCache())
    val rates = _rates.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE_DESC)
    val sortType = _sortType.asStateFlow()

    private val _filterType = MutableStateFlow("ALL")
    val filterType = _filterType.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    init {
        _isOnline.value = checkInitialNetwork()
        startNetworkMonitoring()
        if (_isOnline.value) {
            fetchRates()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortType(type: SortType) {
        _sortType.value = type
    }

    fun updateFilterType(type: String) {
        _filterType.value = type
    }

    fun processItems(
        items: List<TransactionItem>,
        query: String,
        sort: SortType,
        filter: String
    ): List<TransactionItem> {
        var result = items

        if (query.isNotBlank()) {
            result = result.filter { it.title.contains(query, ignoreCase = true) }
        }

        result = when (filter) {
            "INCOME" -> result.filter { it.isIncome }
            "EXPENSE" -> result.filter { !it.isIncome }
            else -> result
        }

        result = when (sort) {
            SortType.DATE_DESC -> result.sortedByDescending {
                parseDate(it.date)
            }
            SortType.DATE_ASC -> result.sortedBy {
                parseDate(it.date)
            }
            SortType.AMOUNT_DESC -> result.sortedByDescending { it.amount }
            SortType.AMOUNT_ASC -> result.sortedBy { it.amount }
        }

        return result
    }

    private fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, dateFormatter)
        } catch (e: Exception) {
            LocalDate.MIN
        }
    }

    private fun checkInitialNetwork(): Boolean {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startNetworkMonitoring() {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    fetchRates()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
    }

    private fun fetchRates() {
        if (_isOnline.value) {
            viewModelScope.launch {
                try {
                    val response = RetrofitClient.api.getRates("BYN")
                    _rates.value = response.rates
                    saveRatesToCache(response.rates)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveRatesToCache(rates: Map<String, Double>) {
        val json = JSONObject(rates).toString()
        prefs.edit().putString("saved_rates", json).apply()
    }

    private fun loadRatesFromCache(): Map<String, Double> {
        val jsonString = prefs.getString("saved_rates", null) ?: return mapOf(
            "BYN" to 1.0, "USD" to 0.31, "EUR" to 0.28, "RUB" to 28.5
        )

        val json = JSONObject(jsonString)
        val map = mutableMapOf<String, Double>()
        json.keys().forEach { key ->
            map[key] = json.getDouble(key)
        }
        return map
    }

    fun changeCurrency(newCurrency: String) {
        _selectedCurrency.value = newCurrency
    }

    fun convert(amountInByn: Double): Double {
        val currency = _selectedCurrency.value
        if (currency == "BYN") return amountInByn
        val rate = _rates.value[currency] ?: 1.0
        return amountInByn * rate
    }

    fun convertToByn(amount: Double, fromCurrency: String): Double {
        if (fromCurrency == "BYN") return amount
        val rate = _rates.value[fromCurrency] ?: 1.0
        return amount / rate
    }
}