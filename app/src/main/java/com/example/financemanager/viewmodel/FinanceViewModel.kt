package com.example.financemanager.viewmodel

import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.financemanager.data.local.TransactionItem
import com.example.financemanager.data.remote.RetrofitClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

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
        observeNetworkStatus()
        fetchRates()
    }

    private fun observeNetworkStatus() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _isOnline.value = true }
            override fun onLost(network: Network) { _isOnline.value = false }
        })
    }

    fun fetchCurrentLocation(context: Context, onResult: (String?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        viewModelScope.launch {
            try {
                val locationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addr = addresses?.firstOrNull()?.let {
                        val city = it.locality ?: ""
                        val street = it.thoroughfare ?: ""
                        val house = it.subThoroughfare ?: ""
                        val building = it.featureName
                        val sb = StringBuilder()
                        if (!building.isNullOrBlank() && building != house) sb.append("$building, ")
                        if (city.isNotBlank()) sb.append("$city, ")
                        if (street.isNotBlank()) sb.append("$street")
                        if (house.isNotBlank()) sb.append(" $house")
                        sb.toString().trim(',', ' ')
                    }
                    onResult(addr ?: "${location.latitude}, ${location.longitude}")
                } else { onResult(null) }
            } catch (e: Exception) { onResult(null) }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSortType(type: SortType) { _sortType.value = type }
    fun updateFilterType(type: String) { _filterType.value = type }

    fun processItems(items: List<TransactionItem>, query: String, sort: SortType, filter: String): List<TransactionItem> {
        var result = items
        if (query.isNotBlank()) result = result.filter { it.title.contains(query, ignoreCase = true) }
        result = when (filter) {
            "INCOME" -> result.filter { it.isIncome }
            "EXPENSE" -> result.filter { !it.isIncome }
            else -> result
        }
        result = when (sort) {
            SortType.DATE_DESC -> result.sortedByDescending { parseDate(it.date) }
            SortType.DATE_ASC -> result.sortedBy { parseDate(it.date) }
            SortType.AMOUNT_DESC -> result.sortedByDescending { it.amount }
            SortType.AMOUNT_ASC -> result.sortedBy { it.amount }
        }
        return result
    }

    private fun parseDate(dateString: String): LocalDate = try { LocalDate.parse(dateString, dateFormatter) } catch (e: Exception) { LocalDate.MIN }

    private fun fetchRates() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getRates("BYN")
                _rates.value = response.rates
                saveRatesToCache(response.rates)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveRatesToCache(rates: Map<String, Double>) {
        val json = JSONObject(rates).toString()
        prefs.edit().putString("saved_rates", json).apply()
    }

    private fun loadRatesFromCache(): Map<String, Double> {
        val jsonString = prefs.getString("saved_rates", null) ?: return mapOf("BYN" to 1.0)
        val json = JSONObject(jsonString)
        val map = mutableMapOf<String, Double>()
        json.keys().forEach { map[it] = json.getDouble(it) }
        return map
    }

    fun changeCurrency(newCurrency: String) { _selectedCurrency.value = newCurrency }
    fun convert(amountInByn: Double): Double {
        val currency = _selectedCurrency.value
        val rate = _rates.value[currency] ?: 1.0
        return amountInByn * rate
    }
    fun convertToByn(amount: Double, fromCurrency: String): Double {
        val rate = _rates.value[fromCurrency] ?: 1.0
        return amount / rate
    }
}