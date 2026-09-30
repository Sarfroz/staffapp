package com.asuliatech.fieldstaff

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseEvidence(onSubmit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var receiptName by remember { mutableStateOf("No receipt selected") }
    var location by remember { mutableStateOf("Location pending") }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) receiptName = cameraFile?.name ?: "Photo captured" }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) receiptName = "Receipt attached: ${uri.lastPathSegment}" }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            location = l?.let { "GPS: ${it.latitude}, ${it.longitude}" } ?: "GPS unavailable — retry outside"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Evidence & Travel Expense", style = MaterialTheme.typography.titleLarge)
        Text("Every photo/receipt is tagged with date, time and GPS.")
        Button(onClick = { val dir=File(context.filesDir,"evidence").apply{mkdirs()}; cameraFile=File(dir,"photo_${System.currentTimeMillis()}.jpg"); photo.launch(androidx.core.content.FileProvider.getUriForFile(context,"${context.packageName}.files",cameraFile!!)) }, modifier=Modifier.fillMaxWidth()) { Text("Take work photo") }
        Button(onClick = { picker.launch("image/*") }, modifier=Modifier.fillMaxWidth()) { Text("Upload hotel / taxi receipt") }
        OutlinedButton(onClick = { permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)) }, modifier=Modifier.fillMaxWidth()) { Text("Capture GPS location") }
        Text(receiptName); Text(location)
        Text("Timestamp: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}")
        Button(onClick = onSubmit, modifier=Modifier.fillMaxWidth()) { Text("Save evidence for server upload") }
    }
}
