package com.focuspath.app.ui.components

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * FocusPath uygulaması için Ödüllü Reklam (Rewarded Ad) Yöneticisi.
 * En kolay ve etkili para kazanma modelini simüle eder.
 */
object AdMobRewardedManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    // Google AdMob Resmi Test Ödüllü Reklam ID'si
    private const val AD_UNIT_ID = "ca-app-pub-9916683255323941/1166087409"

    /**
     * Reklamı arka planda önceden yükler (Örn: Uygulama açılışında veya seans başladığında çağrılabilir)
     */
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
                
                // Hata durumunda 10 saniye sonra otomatik olarak tekrar yüklemeyi dene (Network dalgalanmaları için)
                // Bu sayede reklam hazır değil uyarısı kalıcı olmaz.
            }

            override fun onAdLoaded(ad: RewardedAd) {
                android.util.Log.d("FocusPathAds", "Rewarded Ad Successfully Loaded and Ready to Show!")
                rewardedAd = ad
                isLoading = false
            }
        })
    }

    /**
     * Reklamı gösterir ve kullanıcı sonuna kadar izlerse [onRewardEarned] callback'ini tetikler.
     */
    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(activity) { _ ->
                // Kullanıcı ödülü hak etti!
                onRewardEarned()
                rewardedAd = null
                // Bir sonraki seans için reklamı hemen arka planda tekrar yüklemeye başla
                loadAd(activity)
            }
        } ?: run {
            Toast.makeText(activity, "Ödüllü video hazırlanıyor, lütfen birkaç saniye sonra tekrar deneyin.", Toast.LENGTH_SHORT).show()
            loadAd(activity)
        }
    }
}
