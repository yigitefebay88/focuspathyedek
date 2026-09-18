package com.focuspath.app.ui.components

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobRewardedManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    private const val AD_UNIT_ID = "ca-app-pub-9916683255323941/1166087409"

    fun loadAd(context: Context) {
        if (rewardedAd != null) {
            android.util.Log.d("FocusPathAds", "Rewarded ad is already loaded, skipping reload.")
            return
        }
        if (isLoading) {
            android.util.Log.d("FocusPathAds", "Rewarded ad is currently loading, please wait...")
            return
        }
        
        isLoading = true
        android.util.Log.d("FocusPathAds", "Starting to load Rewarded Ad... Unit ID: $AD_UNIT_ID")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                android.util.Log.e("FocusPathAds", "Rewarded Ad Failed to Load! Error Code: ${adError.code}, Message: ${adError.message}")
                rewardedAd = null
                isLoading = false
                
            }

            override fun onAdLoaded(ad: RewardedAd) {
                android.util.Log.d("FocusPathAds", "Rewarded Ad Successfully Loaded and Ready to Show!")
                rewardedAd = ad
                isLoading = false
            }
        })
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(activity) { _ ->
                onRewardEarned()
                rewardedAd = null
                loadAd(activity)
            }
        } ?: run {
            Toast.makeText(activity, "Ödüllü video hazırlanıyor, lütfen birkaç saniye sonra tekrar deneyin.", Toast.LENGTH_SHORT).show()
            loadAd(activity)
        }
    }
}
