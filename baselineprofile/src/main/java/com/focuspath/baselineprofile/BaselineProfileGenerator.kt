package com.focuspath.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.focuspath.app",
        includeInStartupProfile = true
    ) {
        // Uygulama açılışını simüle et
        pressHome()
        startActivityAndWait()
        
        // Burada kritik akışları ekleyebiliriz (örn. ana ekranın yüklenmesini bekle)
        // device.waitForIdle()
    }
}