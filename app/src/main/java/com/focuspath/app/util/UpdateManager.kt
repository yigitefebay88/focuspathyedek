package com.focuspath.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Manages Google Play In-App Updates.
 */
class UpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val MY_REQUEST_CODE = 9001

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Flexible update downloaded, notify user to install
            showUpdateDownloadedToast()
        }
    }

    /**
     * Checks for updates and triggers the flow if available.
     * Use [AppUpdateType.IMMEDIATE] for critical updates and [AppUpdateType.FLEXIBLE] for others.
     */
    fun checkForUpdates(updateType: Int = AppUpdateType.FLEXIBLE) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                startUpdateFlow(appUpdateInfo, updateType)
            }
        }
        
        // Register listener for flexible updates
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedListener)
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(updateType).build(),
                MY_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to start update flow", e)
        }
    }

    /**
     * Call this in Activity.onResume() to ensure an ongoing update is handled.
     */
    fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an immediate update is in progress, resume it
                startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                // If a flexible update is downloaded, remind user
                showUpdateDownloadedToast()
            }
        }
    }

    /**
     * Unregisters the listener to prevent memory leaks.
     */
    fun onDestroy() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun showUpdateDownloadedToast() {
        Toast.makeText(
            activity,
            "Yeni sürüm indirildi. Uygulamayı yeniden başlatarak güncellemeyi tamamlayabilirsiniz.",
            Toast.LENGTH_LONG
        ).show()
        
        // Optionally trigger completeUpdate() immediately or via a button
        // appUpdateManager.completeUpdate()
    }

    /**
     * Completes the update and restarts the app.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
}
