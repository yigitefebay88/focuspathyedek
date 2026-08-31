package com.focuspath.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.focuspath.app.R

@Composable
fun SplashScreen(terminalColor: Color, onStartClick: () -> Unit) {
    // 3 SANİYE SONRA OTOMATİK GEÇİŞ
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onStartClick()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        // GPT GÖRSELİ (Ekrana Tam Sığdırıldı)
        AsyncImage(
            model = R.drawable.gpt_image,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) // Görselin çok yayılmasını engellemek için yükseklik sınırı
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Fit, // Görseli kırpma, tamamını göster
            alpha = 0.85f
        )

        // KARARTMA VE GRADYAN (Metinlerin okunması için optimize edildi)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // EN ALTTAKİ YÜKLENİYOR VE SLOGAN BÖLÜMÜ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // SLOGANLAR (Yükleme çubuğunun tam üstüne çekildi)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "DAHA İYİ ODAKLAN",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 20.sp // Biraz büyütüldü (18 -> 20)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ZİHNİNİ GÜÇLENDİR",
                    color = terminalColor,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 20.sp // Biraz büyütüldü (18 -> 20)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "BİLİNÇLİ YAŞA",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 20.sp // Biraz büyütüldü (18 -> 20)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp)) // Metni biraz aşağı aldım (90 -> 40)

            Text(
                text = "Sistem Hazırlanıyor...",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            
            Spacer(Modifier.height(16.dp))
            
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.7f) // Ekranın %70'ini kaplasın
                    .height(8.dp),      // Daha kalın ve belirgin yapıldı
                color = terminalColor,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round // Köşeleri yuvarlatıldı
            )
        }
    }
}
