package com.focuspath.app.core.domain.usecase

class GetFocusRankUseCase {
    fun getTitle(score: Long, isEnglish: Boolean): String {
        return when {
            score <= 500 -> if (isEnglish) "Intern (Garage Startup)" else "Stajyer (Garaj Girişimi)"
            score <= 2000 -> if (isEnglish) "Entrepreneur (Freelancer)" else "Girişimci (Serbest Çalışan)"
            score <= 7500 -> if (isEnglish) "Co-Founder (Startup)" else "Kurucu Ortak (Startup)"
            score <= 20000 -> if (isEnglish) "Manager (Small Business)" else "Müdür (Küçük İşletme)"
            score <= 50000 -> if (isEnglish) "CEO (HQ)" else "CEO (Şirket Merkezi)"
            score <= 150000 -> if (isEnglish) "President (Skyscraper CEO)" else "Başkan (Gökdelen CEO'su)"
            else -> if (isEnglish) "Industry Giant (Global Empire)" else "Sektör Devi (Global İmparatorluk)"
        }
    }

    // Domain katmanı Color bilmez, bu yüzden renk kodunu veya seviyesini döneriz.
    fun getRankLevel(score: Long): Int {
        return when {
            score <= 500 -> 1
            score <= 2000 -> 2
            score <= 7500 -> 3
            score <= 20000 -> 4
            score <= 50000 -> 5
            score <= 150000 -> 6
            else -> 7
        }
    }
}
