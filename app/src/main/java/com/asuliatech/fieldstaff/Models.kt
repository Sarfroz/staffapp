package com.asuliatech.fieldstaff

data class StaffProfile(
    val id: Long = 0,
    val username: String = "",
    val name: String = ""
)

data class SchoolItem(
    val schoolId: String,
    val schoolName: String,
    val address: String = ""
)

data class TerminalItem(
    val terminalId: String,
    val schoolId: String,
    val imeiLast6: String = "",
    val simLast6: String = "",
    val status: String = "active"
)

data class OfflineVisitReport(
    val id: String,
    val schoolId: String,
    val schoolName: String,
    val terminalId: String,
    val reportType: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val capturedAt: String,
    val evidenceUri: String?,
    val isCallTested: Boolean = false,
    val notes: String = ""
)
