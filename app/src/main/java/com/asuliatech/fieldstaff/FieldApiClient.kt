package com.asuliatech.fieldstaff

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FieldApiClient {
    private const val baseUrl = "https://asuliatech.com/api/field/v1"
    private const val preferencesName = "field_staff_session"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun login(context: Context, username: String, password: String, onResult: (Boolean, String) -> Unit) {
        request("POST", "/login", JSONObject()
            .put("username", username.trim())
            .put("password", password)
            .put("device_id", android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID))) { ok, body ->
            if (ok) {
                val token = JSONObject(body).optString("token")
                if (token.isNotBlank()) {
                    context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                        .edit().putString("token", token).apply()
                    onResult(true, "Login successful")
                } else onResult(false, "Server did not return a session token")
            } else onResult(false, message(body))
        }
    }

    fun token(context: Context): String? =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString("token", null)

    fun logout(context: Context, onDone: () -> Unit = {}) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().apply()
        onDone()
    }

    private fun request(method: String, path: String, payload: JSONObject, callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val body = BufferedReader(stream.reader()).use { it.readText() }
                mainHandler.post { callback(connection.responseCode in 200..299, body) }
            } catch (error: Exception) {
                mainHandler.post { callback(false, error.message ?: "Unable to contact server") }
            }
        }.start()
    }

    private fun message(body: String): String = try {
        JSONObject(body).optString("message", "Login failed")
    } catch (_: Exception) {
        "Unable to contact server"
    }
}
