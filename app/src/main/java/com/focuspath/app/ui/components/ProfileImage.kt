package com.focuspath.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileImage(
    photoUrl: String?,
    name: String?,
    email: String? = null,
    size: Dp = 40.dp,
    border: BorderStroke? = null
) {
    val initials = if (!name.isNullOrBlank()) {
        name.take(1).uppercase()
    } else if (!email.isNullOrBlank()) {
        email.take(1).uppercase()
    } else {
        "?"
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        var isError by remember(photoUrl) { mutableStateOf(false) }
        val finalPhotoUrl = if (photoUrl == "null") null else photoUrl
        
        if (!finalPhotoUrl.isNullOrBlank() && !isError) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(finalPhotoUrl)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = name,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = { 
                    android.util.Log.e("FocusPathImage", "FAILED loading profile image: $finalPhotoUrl for $name")
                    isError = true 
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (size.value * 0.4).sp
                    )
                )
            }
        }
    }
}
