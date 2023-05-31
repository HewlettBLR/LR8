package com.example.lr8

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import okhttp3.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var amountEditText: EditText
    private lateinit var fromCurrencySpinner: Spinner
    private lateinit var toCurrencySpinner: Spinner
    private lateinit var convertButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var resultTextView1: TextView
    private lateinit var citySpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amountEditText = findViewById(R.id.amountEditText)
        fromCurrencySpinner = findViewById(R.id.fromCurrencySpinner)
        toCurrencySpinner = findViewById(R.id.toCurrencySpinner)
        convertButton = findViewById(R.id.convertButton)
        resultTextView = findViewById(R.id.resultTextView)
        resultTextView1 = findViewById(R.id.resultTextView1)
        citySpinner = findViewById(R.id.citySpinner)

        val cities = arrayOf("Brest", "Minsk", "Moscow", "Grodno")
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter

        val currencies = arrayOf("USD", "EUR", "BYN", "RUB")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fromCurrencySpinner.adapter = adapter
        toCurrencySpinner.adapter = adapter

        citySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fetchWeather()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        })

        convertButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            if (amount != null) {
                val fromCurrency = fromCurrencySpinner.selectedItem as String
                val toCurrency = toCurrencySpinner.selectedItem as String
                fetchExchangeRate(fromCurrency, toCurrency, amount)
            } else {
                resultTextView1.text = "Введите корректное значение суммы"
            }
        }
    }

    private fun fetchWeather() {
        GlobalScope.launch {
            try {
                val selectedCity = citySpinner.selectedItem as String
                val weatherData = fetchWeatherFromAPI(selectedCity)
                withContext(Dispatchers.Main) {
                    val weatherText = "Погода в $selectedCity:\n$weatherData"
                    resultTextView.text = weatherText
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorText = "Ошибка при выполнении запроса: ${e.message}"
                    resultTextView.text = errorText
                }
            }
        }
    }

    private suspend fun fetchWeatherFromAPI(city: String): String = withContext(Dispatchers.IO) {
        val apiKey = "842874aa24b6923e2ecd2fe43eb32331"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string()
        val jsonObject = JSONObject(responseString)

        if (jsonObject.has("main")) {
            val mainData = jsonObject.getJSONObject("main")
            val temperature = mainData.getDouble("temp")
            val temp_min = mainData.getDouble("temp_min")
            val temp_max = mainData.getDouble("temp_max")
            val pressure = mainData.getDouble("pressure")
            val humidity = mainData.getDouble("humidity")

            val weatherArray = jsonObject.getJSONArray("weather")
            val weatherObject = weatherArray.getJSONObject(0)
            val weatherDescription = weatherObject.getString("description")

            val weatherData = "Температура: $temperature °C\nОписание: $weatherDescription \nТемпература min: $temp_min °C\nТемпература max: $temp_max °C\nДавление: $pressure mm\n" +
                    "Влажность: $humidity %"
            weatherData
        } else {
            throw IOException("Invalid response format")
        }
    }

    private fun fetchExchangeRate(fromCurrency: String, toCurrency: String, amount: Double) {
        GlobalScope.launch {
            try {
                val exchangeRate = fetchExchangeRateFromAPI(fromCurrency, toCurrency)
                withContext(Dispatchers.Main) {
                    val convertedAmount = amount * exchangeRate
                    val resultText = "$amount $fromCurrency = $convertedAmount $toCurrency"
                    resultTextView1.text = resultText
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    val errorText = "Ошибка при выполнении запроса: ${e.message}"
                    resultTextView1.text = errorText
                }
            }
        }
    }

    private suspend fun fetchExchangeRateFromAPI(fromCurrency: String, toCurrency: String): Double = withContext(Dispatchers.IO) {
        val apiKey = "a0WLmf670pKhvicZch2AviUZ6eJnH97L"
        val url = "https://api.apilayer.com/exchangerates_data/convert?from=$fromCurrency&to=$toCurrency&amount=1"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("apikey", apiKey)
        connection.connect()

        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(response)
        val exchangeRate = jsonObject.getDouble("result")
        exchangeRate
    }
}