package com.example.util

import android.app.Activity
import android.view.WindowManager

object SecurityUtils {
    fun applyScreenshotProtection(activity: Activity) {
        try {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (_: Exception) {}
    }
}
