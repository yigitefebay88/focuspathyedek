package com.focuspath.app.util

import androidx.compose.ui.graphics.Color

object FocusRank {
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

    fun getColor(score: Long): Color {
        return when {
            score <= 500 -> Color.Gray
            score <= 2000 -> Color(0xFF81C784) // Emerald
            score <= 7500 -> Color(0xFF64B5F6) // Blue
            score <= 20000 -> Color(0xFFBA68C8) // Purple
            score <= 50000 -> Color(0xFFFFB74D) // Amber
            score <= 150000 -> Color(0xFFE57373) // Red
            else -> Color(0xFFFFD700) // Gold
        }
    }
}
