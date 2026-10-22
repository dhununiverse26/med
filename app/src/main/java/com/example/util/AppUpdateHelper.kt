package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

object AppUpdateHelper {
    fun checkForAppUpdate(context: Context, onUpdateAvailable: () -> Unit = {}) {}
    fun checkForAppUpdate(activity: Activity) {}
    fun openPlayStore(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}"))
            activity.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun AppUpdateBlockingDialog(
    showDialog: Boolean,
    onUpdateClick: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Update Available", fontWeight = FontWeight.Bold) },
            text = { Text("A new version of MEDXMED is available. Please update to continue using the app.") },
            confirmButton = {
                Button(
                    onClick = onUpdateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Update Now", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
