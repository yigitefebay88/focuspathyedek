package com.focuspath.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.focuspath.app.MainActivity

class FocusBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            val prefs = getSharedPreferences("focuspath_prefs", MODE_PRIVATE)
            val isFocusActive = prefs.getBoolean("is_focus_active", false)
            
            if (isFocusActive) {
                val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
                
                if (blockedApps.contains(packageName)) {
                    // Block the app
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    
                    // Optionally show a toast or redirect to our app
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    
                    Toast.makeText(this, "Odaklanma modundasın! Bu uygulama şu an engelli.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onInterrupt() {
        // Handle interrupt
    }
}
