package com.example.financemanager

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.net.Network
import android.net.NetworkRequest

// Теперь наследуемся от AndroidViewModel, чтобы получить доступ к Context (для кэша и сети)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    val currencies = listOf("BYN", "USD", "EUR", "RUB", "CNY", "CHF", "GBP", "TRY", "KZT")

    private val _selectedCurrency = MutableStateFlow("BYN")
    val selectedCurrency = _selectedCurrency.asStateFlow()

    // ПУНКТ 3: Мониторинг интернета
    // 1. Изначально ставим false (на всякий случай)
    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    // Инструмент для сохранения кэша
    private val prefs = application.getSharedPreferences("RatesCache", Context.MODE_PRIVATE)

    // Изначально загружаем курсы из кэша
    private val _rates = MutableStateFlow<Map<String, Double>>(loadRatesFromCache())
    val rates = _rates.asStateFlow()

    init {
        // 2. СРАЗУ проверяем сеть при запуске
        _isOnline.value = checkInitialNetwork()

        // 3. Запускаем постоянный мониторинг
        startNetworkMonitoring()

        // 4. Если интернет был со старта - качаем курсы
        if (_isOnline.value) {
            fetchRates()
        }
    }

    // Одиночная проверка для момента запуска
    private fun checkInitialNetwork(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Слушаем интернет в реальном времени
    private fun startNetworkMonitoring() {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                fetchRates() // Интернет появился -> сразу качаем свежие курсы!
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    private fun fetchRates() {
        // Качаем только если мы уверены, что сеть есть
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

    // Сохранение в локальный кэш (SharedPreferences)
    private fun saveRatesToCache(rates: Map<String, Double>) {
        val json = JSONObject(rates).toString()
        prefs.edit().putString("saved_rates", json).apply()
    }

    // Загрузка из локального кэша
    private fun loadRatesFromCache(): Map<String, Double> {
        val jsonString = prefs.getString("saved_rates", null) ?: return mapOf(
            "BYN" to 1.0, "USD" to 0.31, "EUR" to 0.28, "RUB" to 28.5
        ) // Фолбэк, если приложение запущено впервые без интернета

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