package ro.pub.cs.systems.eim.practicetest02.communication

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.*
import ro.pub.cs.systems.eim.practicetest02.WeatherResponse
import java.util.HashMap
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

class Server(private val port: Int) {

    private var isRunning = false
    private var weatherData = HashMap<String, WeatherResponse>()

    fun start() {
        isRunning = true
        GlobalScope.launch(Dispatchers.IO) {
            val serverSocket = ServerSocket(port)
            Log.d("Server", "Server is running on port $port")
            while (isRunning) {
                try {
                    val clientSocket = serverSocket.accept()
                    Log.d("Server", "Client connected: ${clientSocket.inetAddress.hostAddress}")
                    launch { ClientHandler(clientSocket, weatherData).run() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            serverSocket.close()
        }
    }

    fun stop() {
        isRunning = false
    }
}

class ClientHandler(private val clientSocket: Socket, private val weatherData: HashMap<String, WeatherResponse>) {

    suspend fun run() {
        withContext(Dispatchers.IO) {
            try {
                val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val output = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream()), true)

                // Read a message from the client
                val clientMessage = input.readLine()
                Log.d("Server", "Received from client: $clientMessage")
                val parts = clientMessage.split("|")

                // Access the parts
                val request = parts[0]
                val city = parts[1]
                if (!weatherData.containsKey(city)) {
                    Log.d("Server", "Object not found, calling API")
                    weatherData[city] = getWeather(city)
                }
                // Send a response back to the client
                val responseMessage = processResponse(request, weatherData[city]!!)
                output.println(responseMessage)

                // Close the streams and the socket
                input.close()
                output.close()
                clientSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processResponse(request: String, weather: WeatherResponse): String {
        when (request) {
            "temperatura" -> return weather.main.temp.toString()
            "viteza vantului" -> return weather.wind.speed.toString()
            "starea generala" -> return weather.weather[0].description
            "presiune" -> return weather.main.pressure.toString()
            "umiditate" -> return weather.main.humidity.toString()
        }
        return weather.toString()
    }

    private fun getWeather(city: String): WeatherResponse {
        val apiKey = "e03c3b32cfb5a6f7069f2ef29237d87e"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw RuntimeException("Response body is null")

        // Parse JSON using Gson
        val gson = Gson()
        return gson.fromJson(responseBody, WeatherResponse::class.java)
    }

}