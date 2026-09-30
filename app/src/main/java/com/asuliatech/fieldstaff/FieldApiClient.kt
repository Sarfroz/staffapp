package com.asuliatech.fieldstaff

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object FieldApiClient {
    private const val baseUrl = "https://asuliatech.com/api/field/v1"
    private const val terminalBaseUrl = "https://asuliatech.com"
    private const val preferencesName = "field_staff_session"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun login(
        context: Context,
        username: String,
        password: String,
        onResult: (Boolean, String, StaffProfile?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("username", username.trim())
            put("password", password)
            put(
                "device_id",
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            )
        }

        request("POST", "$baseUrl/login", payload, null) { ok, code, body ->
            if (ok) {
                try {
                    val json = JSONObject(body)
                    val token = json.optString("token")
                    if (token.isNotBlank()) {
                        val staffJson = json.optJSONObject("staff")
                        val staffProfile = StaffProfile(
                            id = staffJson?.optLong("id") ?: 0L,
                            username = staffJson?.optString("username", username) ?: username,
                            name = staffJson?.optString("name", username) ?: username
                        )

                        saveToken(context, token)
                        saveStaffProfile(context, staffProfile)

                        // Parse bootstrap data if available
                        val bootstrap = json.optJSONObject("bootstrap")
                        if (bootstrap != null) {
                            parseAndSaveBootstrap(context, bootstrap)
                        }

                        onResult(true, "Login successful", staffProfile)
                    } else {
                        onResult(false, "Server did not return a session token", null)
                    }
                } catch (e: Exception) {
                    onResult(false, "Failed to parse login response: ${e.message}", null)
                }
            } else {
                onResult(false, extractMessage(body, "Login failed"), null)
            }
        }
    }

    fun bootstrap(
        context: Context,
        onResult: (Boolean, List<SchoolItem>, List<TerminalItem>) -> Unit
    ) {
        val token = token(context)
        if (token.isNullOrBlank()) {
            onResult(false, getCachedSchools(context), getCachedTerminals(context))
            return
        }

        request("GET", "$baseUrl/bootstrap", null, token) { ok, _, body ->
            if (ok) {
                try {
                    val json = JSONObject(body)
                    val bootstrap = json.optJSONObject("bootstrap")
                    if (bootstrap != null) {
                        val (schools, terminals) = parseAndSaveBootstrap(context, bootstrap)
                        onResult(true, schools, terminals)
                    } else {
                        onResult(true, getCachedSchools(context), getCachedTerminals(context))
                    }
                } catch (e: Exception) {
                    onResult(false, getCachedSchools(context), getCachedTerminals(context))
                }
            } else {
                onResult(false, getCachedSchools(context), getCachedTerminals(context))
            }
        }
    }

    fun activateTerminalDirect(
        schoolId: String,
        terminalId: String,
        imei: String,
        sim: String,
        secretPin: String = "321123",
        onResult: (Boolean, String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$terminalBaseUrl/terminal/activate-post")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }

                val postData = buildString {
                    append("school_id=").append(URLEncoder.encode(schoolId.trim(), "UTF-8"))
                    append("&terminal_id=").append(URLEncoder.encode(terminalId.trim().uppercase(), "UTF-8"))
                    append("&imei=").append(URLEncoder.encode(imei.trim(), "UTF-8"))
                    append("&sim=").append(URLEncoder.encode(sim.trim(), "UTF-8"))
                    append("&pin=").append(URLEncoder.encode(secretPin.trim(), "UTF-8"))
                    append("&secret_pin=").append(URLEncoder.encode(secretPin.trim(), "UTF-8"))
                }

                OutputStreamWriter(conn.outputStream).use { it.write(postData) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = BufferedReader(stream.reader()).use { it.readText() }

                mainHandler.post {
                    try {
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false) || code in 200..299) {
                            val msg = json.optString("message", "Terminal activated successfully!")
                            onResult(true, msg)
                        } else {
                            val err = json.optString("error", json.optString("message", "Activation failed"))
                            onResult(false, err)
                        }
                    } catch (e: Exception) {
                        if (code in 200..299) {
                            onResult(true, "Terminal activated successfully!")
                        } else {
                            onResult(false, "Server error ($code): $body")
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onResult(false, "Network error: ${e.message ?: "Unable to contact server"}")
                }
            }
        }.start()
    }

    fun submitReport(
        context: Context,
        schoolId: String,
        terminalId: String?,
        reportType: String,
        status: String = "completed",
        payload: JSONObject? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        evidenceUrl: String? = null,
        evidenceType: String? = "installation_photo",
        onResult: (Boolean, String) -> Unit
    ) {
        val token = token(context)
        val bodyObj = JSONObject().apply {
            put("school_id", schoolId)
            if (!terminalId.isNullOrBlank()) put("terminal_id", terminalId)
            put("report_type", reportType)
            put("status", status)
            if (payload != null) put("payload", payload)
            if (latitude != null) put("latitude", latitude)
            if (longitude != null) put("longitude", longitude)
            if (!evidenceUrl.isNullOrBlank()) {
                put("evidence_url", evidenceUrl)
                put("evidence_type", evidenceType)
            }
        }

        request("POST", "$baseUrl/reports", bodyObj, token) { ok, _, body ->
            if (ok) {
                onResult(true, "Report submitted successfully")
            } else {
                onResult(false, extractMessage(body, "Failed to submit report"))
            }
        }
    }

    fun token(context: Context): String? =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString("token", null)

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit().putString("token", token).apply()
    }

    fun getStaffProfile(context: Context): StaffProfile {
        val sp = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        return StaffProfile(
            id = sp.getLong("staff_id", 0L),
            username = sp.getString("staff_username", "Staff") ?: "Staff",
            name = sp.getString("staff_name", "Field Staff") ?: "Field Staff"
        )
    }

    fun saveStaffProfile(context: Context, profile: StaffProfile) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
            .putLong("staff_id", profile.id)
            .putString("staff_username", profile.username)
            .putString("staff_name", profile.name)
            .apply()
    }

    fun getCachedSchools(context: Context): List<SchoolItem> {
        val sp = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val jsonStr = sp.getString("cached_schools", null) ?: return defaultSchools()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SchoolItem>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(
                    SchoolItem(
                        schoolId = item.optString("school_id"),
                        schoolName = item.optString("school_name"),
                        address = item.optString("address", "")
                    )
                )
            }
            if (list.isEmpty()) defaultSchools() else list
        } catch (_: Exception) {
            defaultSchools()
        }
    }

    fun getCachedTerminals(context: Context): List<TerminalItem> {
        val sp = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val jsonStr = sp.getString("cached_terminals", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<TerminalItem>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(
                    TerminalItem(
                        terminalId = item.optString("terminal_id"),
                        schoolId = item.optString("school_id"),
                        imeiLast6 = item.optString("imei_last_6"),
                        simLast6 = item.optString("sim_last_6"),
                        status = item.optString("status", "active")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseAndSaveBootstrap(context: Context, bootstrap: JSONObject): Pair<List<SchoolItem>, List<TerminalItem>> {
        val schools = mutableListOf<SchoolItem>()
        val terminals = mutableListOf<TerminalItem>()

        val schoolsArray = bootstrap.optJSONArray("schools")
        if (schoolsArray != null) {
            for (i in 0 until schoolsArray.length()) {
                val obj = schoolsArray.getJSONObject(i)
                schools.add(
                    SchoolItem(
                        schoolId = obj.optString("school_id"),
                        schoolName = obj.optString("school_name"),
                        address = obj.optString("address", "")
                    )
                )
            }
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
                .putString("cached_schools", schoolsArray.toString()).apply()
        }

        val terminalsArray = bootstrap.optJSONArray("terminals")
        if (terminalsArray != null) {
            for (i in 0 until terminalsArray.length()) {
                val obj = terminalsArray.getJSONObject(i)
                terminals.add(
                    TerminalItem(
                        terminalId = obj.optString("terminal_id"),
                        schoolId = obj.optString("school_id"),
                        imeiLast6 = obj.optString("imei_last_6"),
                        simLast6 = obj.optString("sim_last_6"),
                        status = obj.optString("status", "active")
                    )
                )
            }
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
                .putString("cached_terminals", terminalsArray.toString()).apply()
        }

        return Pair(schools, terminals)
    }

    private fun defaultSchools(): List<SchoolItem> = listOf(
        SchoolItem("SCH-001", "DAV Public School, Patna", "Boring Road, Patna"),
        SchoolItem("SCH-002", "Delhi Public School, Gaya", "Bodh Gaya Road"),
        SchoolItem("SCH-003", "St. Xavier's High School, Muzaffarpur", "Club Road"),
        SchoolItem("SCH-004", "Kendriya Vidyalaya, Danapur", "Cantt Area, Danapur")
    )

    fun logout(context: Context, onDone: () -> Unit = {}) {
        val token = token(context)
        if (!token.isNullOrBlank()) {
            request("POST", "$baseUrl/logout", JSONObject(), token) { _, _, _ -> }
        }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().apply()
        onDone()
    }

    private fun request(
        method: String,
        fullUrl: String,
        payload: JSONObject?,
        bearerToken: String?,
        callback: (Boolean, Int, String) -> Unit
    ) {
        Thread {
            try {
                val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("Accept", "application/json")
                    if (bearerToken != null) {
                        setRequestProperty("Authorization", "Bearer $bearerToken")
                    }
                    if (payload != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                    }
                }

                if (payload != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val body = BufferedReader(stream.reader()).use { it.readText() }
                mainHandler.post { callback(code in 200..299, code, body) }
            } catch (error: Exception) {
                mainHandler.post { callback(false, 0, error.message ?: "Unable to contact server") }
            }
        }.start()
    }

    private fun extractMessage(body: String, fallback: String): String = try {
        val json = JSONObject(body)
        json.optString("message", json.optString("error", fallback))
    } catch (_: Exception) {
        if (body.isNotBlank() && body.length < 120) body else fallback
    }
}
