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
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onStartClick()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        AsyncImage(
            model = R.drawable.gpt_image,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) 
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Fit, 
            alpha = 0.85f
        )

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
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
                        fontSize = 20.sp 
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ZİHNİNİ GÜÇLENDİR",
                    color = terminalColor,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 20.sp 
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "BİLİNÇLİ YAŞA",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 20.sp 
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp)) 

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
                    .fillMaxWidth(0.7f) 
                    .height(8.dp),      
                color = terminalColor,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round 
            )
        }
    }
}
