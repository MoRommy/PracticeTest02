package ro.pub.cs.systems.eim.practicetest02

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.pub.cs.systems.eim.practicetest02.communication.Server
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

class PracticeTest02MainActivity : ComponentActivity() {

    private lateinit var server: Server
    private lateinit var spinnerItem: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice_test_02_main)

        //UI elements binding
        val connectionAddressEditText: EditText = findViewById(R.id.client_addr_edit_text)
        val connectionPortEditText: EditText = findViewById(R.id.client_port_edit_text)
        val cityEditText: EditText = findViewById(R.id.city_edit_text)
        val spinner: Spinner = findViewById(R.id.spinner)
        val submitButton: Button = findViewById(R.id.submit_port_button)
        val responseTextView: TextView = findViewById(R.id.server_response_textview)
        val serverPortEditText: EditText = findViewById(R.id.server_port_edit_text)
        val startServerButton: Button = findViewById(R.id.server_button)

        //Spinner init
        ArrayAdapter.createFromResource(
            this,
            R.array.choices_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (parent != null) {
                    spinnerItem = parent.getItemAtPosition(position).toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //Client init
        submitButton.setOnClickListener {
            if (::server.isInitialized) {
                val host = connectionAddressEditText.text.toString()
                val port = connectionPortEditText.text.toString()
                val city = cityEditText.text.toString()
                val request = spinnerItem
                if (host != "" && port != "" && request != "" && city != "") {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = connectToServer(host, port.toInt(), request, city)
                            withContext(Dispatchers.Main) {
                                responseTextView.text = response
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                val err = "Error: ${e.message}"
                                responseTextView.text = err
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Completeaza host, port, city & request", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Porneste mai intai serverul", Toast.LENGTH_SHORT).show()
            }
        }

        //Server init
        startServerButton.setOnClickListener {
            if (::server.isInitialized) {
                Toast.makeText(this, "Serverul deja ruleaza", Toast.LENGTH_SHORT).show()
            } else {
                val serverPort = serverPortEditText.text.toString()
                if (serverPort != "") {
                    if (serverPort.toInt() < 1000 || serverPort.toInt() > 65000)
                        Toast.makeText(this, "Introdu un port intre 1k si 65k", Toast.LENGTH_SHORT).show()
                    else {
                        startServer(serverPort.toInt())
                    }
                } else {
                    Toast.makeText(this, "Introdu portul pe care sa porneasca serverul", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startServer(port: Int) {
        server = Server(port)
        CoroutineScope(Dispatchers.IO).launch {
            server.start()
        }
        Toast.makeText(this, "Serverul a pornit pe portul $port", Toast.LENGTH_SHORT).show()
    }

    private suspend fun connectToServer(host: String, port: Int, request: String, city: String): String {
        return withContext(Dispatchers.IO) {
            val clientSocket = Socket(host, port)
            val output = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream()), true)
            val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

            output.println("$request|$city")
            val response = input.readLine()

            input.close()
            output.close()
            clientSocket.close()

            response
        }
    }


}
