package com.asuliatech.fieldstaff

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object OfflineManager {
    private const val PREFS_NAME = "field_staff_offline"
    private const val KEY_PENDING_REPORTS = "pending_reports"

    fun saveReport(
        context: Context,
        schoolId: String,
        schoolName: String,
        terminalId: String,
        reportType: String = "new_installation",
        latitude: Double? = null,
        longitude: Double? = null,
        capturedAt: String = "",
        evidenceUri: String? = null,
        isCallTested: Boolean = true,
        notes: String = ""
    ): String {
        val id = UUID.randomUUID().toString()
        val report = OfflineVisitReport(
            id = id,
            schoolId = schoolId,
            schoolName = schoolName,
            terminalId = terminalId,
            reportType = reportType,
            status = "pending_sync",
            latitude = latitude,
            longitude = longitude,
            capturedAt = capturedAt,
            evidenceUri = evidenceUri,
            isCallTested = isCallTested,
            notes = notes
        )

        val list = getReports(context).toMutableList()
        list.add(report)
        saveList(context, list)
        return id
    }

    fun getReports(context: Context): List<OfflineVisitReport> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_PENDING_REPORTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<OfflineVisitReport>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OfflineVisitReport(
                        id = obj.optString("id"),
                        schoolId = obj.optString("school_id"),
                        schoolName = obj.optString("school_name"),
                        terminalId = obj.optString("terminal_id"),
                        reportType = obj.optString("report_type", "new_installation"),
                        status = obj.optString("status", "pending_sync"),
                        latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.optDouble("latitude") else null,
                        longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.optDouble("longitude") else null,
                        capturedAt = obj.optString("captured_at"),
                        evidenceUri = if (obj.has("evidence_uri") && !obj.isNull("evidence_uri")) obj.optString("evidence_uri") else null,
                        isCallTested = obj.optBoolean("is_call_tested", true),
                        notes = obj.optString("notes")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun removeReport(context: Context, id: String) {
        val list = getReports(context).filter { it.id != id }
        saveList(context, list)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun pendingCount(context: Context): Int {
        return getReports(context).size
    }

    private fun saveList(context: Context, list: List<OfflineVisitReport>) {
        val array = JSONArray()
        list.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("school_id", r.schoolId)
                put("school_name", r.schoolName)
                put("terminal_id", r.terminalId)
                put("report_type", r.reportType)
                put("status", r.status)
                if (r.latitude != null) put("latitude", r.latitude)
                if (r.longitude != null) put("longitude", r.longitude)
                put("captured_at", r.capturedAt)
                put("evidence_uri", r.evidenceUri)
                put("is_call_tested", r.isCallTested)
                put("notes", r.notes)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PENDING_REPORTS, array.toString()).apply()
    }
}
