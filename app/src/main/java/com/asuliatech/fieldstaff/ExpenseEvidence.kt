package com.asuliatech.fieldstaff

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandTeal = Color(0xFF008B8C)
private val BrandNavy = Color(0xFF082849)
private val BrandPurple = Color(0xFF6366F1)
private val SuccessGreen = Color(0xFF059669)

@Composable
fun ExpenseEvidence(
    schoolId: String = "",
    terminalId: String = "",
    onSubmit: (evidenceUri: String?, lat: Double?, lng: Double?, timestamp: String) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var evidenceType by remember { mutableStateOf("installation_photo") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var statusText by remember { mutableStateOf("Capture on-site photo or upload evidence") }
    var isUploading by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val currentTimestamp = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            selectedUri = tempCameraUri
            statusText = "✓ Installation photo captured successfully!"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            evidenceType = "work_photo"
            statusText = "✓ Image selected from storage"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    statusText = "✓ GPS Tagged: ${String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)}"
                } else {
                    statusText = "GPS signal weak — location tagged via cell network"
                }
            } catch (e: Exception) {
                statusText = "Location error: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Site Evidence & Verification",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandNavy
        )
        Text(
            text = "Upload high-clarity photos of installed terminal, antenna positioning, and wiring.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // Photo capture options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val dir = File(context.filesDir, "evidence").apply { mkdirs() }
                        val file = File(dir, "evidence_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.files",
                            file
                        )
                        tempCameraUri = uri
                        photoLauncher.launch(uri)
                    } catch (e: Exception) {
                        statusText = "Camera error: ${e.message}"
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
            ) {
                Text("📷 Take Photo", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandNavy)
            ) {
                Text("📁 Choose File", fontWeight = FontWeight.SemiBold)
            }
        }

        // GPS Tagging Button
        OutlinedButton(
            onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPurple)
        ) {
            Text("📍 Tag Current GPS Location", fontWeight = FontWeight.SemiBold)
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (statusText.startsWith("✓")) SuccessGreen else BrandNavy
                )
                if (latitude != null && longitude != null) {
                    Text(
                        text = "GPS: ${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Time: $currentTimestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Submit Button
        Button(
            onClick = {
                isUploading = true
                statusText = "Finalizing evidence package..."
                val uri = selectedUri
                if (uri != null) {
                    EvidenceUploader.upload(uri, evidenceType, latitude, longitude) { ok, msg ->
                        isUploading = false
                        statusText = msg
                        onSubmit(uri.toString(), latitude, longitude, currentTimestamp)
                    }
                } else {
                    isUploading = false
                    // Allow continuing even if photo is optional or offline
                    onSubmit(null, latitude, longitude, currentTimestamp)
                }
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    "Submit Installation Report   ✓",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
