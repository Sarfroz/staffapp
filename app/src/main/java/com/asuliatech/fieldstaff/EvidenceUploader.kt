package com.asuliatech.fieldstaff

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object EvidenceUploader {
    fun upload(uri: Uri, type: String, latitude: Double?, longitude: Double?, onDone: (Boolean, String) -> Unit) {
        val id = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference.child("field_evidence/$id")
        ref.putFile(uri).continueWithTask { ref.downloadUrl }.addOnSuccessListener { url ->
            FirebaseFirestore.getInstance().collection("field_evidence").document(id).set(mapOf(
                "url" to url.toString(), "type" to type, "latitude" to latitude,
                "longitude" to longitude, "createdAt" to System.currentTimeMillis(), "status" to "uploaded"
            )).addOnSuccessListener { onDone(true, "Saved to server") }.addOnFailureListener { onDone(false, it.message ?: "Server record failed") }
        }.addOnFailureListener { onDone(false, it.message ?: "Upload failed") }
    }
}
