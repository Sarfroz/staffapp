package com.asuliatech.fieldstaff

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseEvidence(onSubmit: () -> Unit) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var evidenceType by remember { mutableStateOf("work_photo") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var status by remember { mutableStateOf("Capture a photo or upload a receipt") }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) status = "Work photo ready" }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { selectedUri = uri; evidenceType = "travel_receipt"; status = "Receipt ready" } }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            latitude = loc?.latitude; longitude = loc?.longitude
            status = if (loc == null) "GPS unavailable — move outside and retry" else "GPS captured: ${loc.latitude}, ${loc.longitude}"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Evidence & Travel Expense", style = MaterialTheme.typography.titleLarge)
        Text("Photo/receipt, GPS and timestamp are saved with this visit.")
        Button(onClick = { val f=File(context.filesDir,"evidence").apply{mkdirs()}; val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",File(f,"photo_${System.currentTimeMillis()}.jpg")); selectedUri=uri; evidenceType="work_photo"; photo.launch(uri) }, modifier=Modifier.fillMaxWidth()) { Text("Take work photo") }
        OutlinedButton(onClick = { picker.launch("image/*") }, modifier=Modifier.fillMaxWidth()) { Text("Upload hotel / taxi receipt") }
        OutlinedButton(onClick = { permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)) }, modifier=Modifier.fillMaxWidth()) { Text("Capture GPS location") }
        Text(status)
        Text("Timestamp: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}")
        Button(onClick = { val uri=selectedUri; if(uri==null) status="Please add a photo or receipt first" else { status="Uploading to server..."; EvidenceUploader.upload(uri,evidenceType,latitude,longitude){ ok,msg-> status=msg; if(ok) onSubmit() } } }, modifier=Modifier.fillMaxWidth()) { Text("Upload & submit evidence") }
    }
}
