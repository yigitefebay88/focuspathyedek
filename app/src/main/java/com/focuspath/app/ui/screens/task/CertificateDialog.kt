package com.focuspath.app.ui.screens.task

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.focuspath.app.util.FocusRank
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CertificateDialog(
    userName: String,
    totalXp: Long,
    totalFocusMinutes: Int,
    totalTasksCompleted: Int,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
    val title = FocusRank.getTitle(totalXp, isEnglish)
    val rankColor = FocusRank.getColor(totalXp)
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFCF8F3), // Klasik sertifika kağıdı rengi
            border = BorderStroke(12.dp, rankColor.copy(alpha = 0.8f))
        ) {
            Box(modifier = Modifier.padding(12.dp).border(2.dp, rankColor.copy(alpha = 0.3f))) {
                // Arka plan deseni veya logo (Opsiyonel filigran)
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.Center)
                        .alpha(0.03f),
                    tint = rankColor
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = if(isEnglish) "CERTIFICATE OF DISCIPLINE" else "ÜSTÜN DİSİPLİN SERTİFİKASI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    HorizontalDivider(
                        modifier = Modifier.width(100.dp),
                        thickness = 2.dp,
                        color = rankColor
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = if(isEnglish) "This is to certify that" else "İşbu belge ile",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = Color.DarkGray
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = userName.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = if(isEnglish) 
                            "has demonstrated exceptional focus and productivity, achieving the esteemed rank of"
                            else "olağanüstü odaklanma ve üretkenlik göstererek şu unvana layık görülmüştür:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        color = rankColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, rankColor)
                    ) {
                        Text(
                            text = title.uppercase(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = rankColor
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    // NEW: Detailed Stats Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalTasksCompleted",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if(isEnglish) "Tasks Done" else "Görev Tamam",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalXp",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "XP",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dateStr, fontWeight = FontWeight.Bold, color = Color.Black)
                            HorizontalDivider(modifier = Modifier.width(80.dp), thickness = 1.dp, color = Color.Black)
                            Text(if(isEnglish) "Issue Date" else "Veriliş Tarihi", fontSize = 10.sp, color = Color.Gray)
                        }

                        // Mühür / Logo + Placeholder for QR-like UI element
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = rankColor,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("FP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FocusPath AI", fontWeight = FontWeight.Bold, color = Color.Black, fontStyle = FontStyle.Italic)
                            HorizontalDivider(modifier = Modifier.width(80.dp), thickness = 1.dp, color = Color.Black)
                            Text(if(isEnglish) "Authorized By" else "Yetkili Onayı", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                saveCertificateAsPdf(context, userName, totalXp, totalFocusMinutes, totalTasksCompleted, isEnglish)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = rankColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if(isEnglish) "SAVE" else "KAYDET", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                shareCertificate(context, userName, totalXp, totalFocusMinutes, totalTasksCompleted, isEnglish)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if(isEnglish) "SHARE" else "PAYLAŞ", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if(isEnglish) "CLOSE" else "KAPAT", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    
                    Text(
                        text = if(isEnglish) "Verified By FocusPath AI Verification System" else "FocusPath AI Doğrulama Sistemi Tarafından Onaylıdır",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun saveCertificateAsPdf(
    context: android.content.Context, 
    name: String, 
    xp: Long, 
    focusMins: Int, 
    tasksDone: Int, 
    isEnglish: Boolean,
    isSharing: Boolean = false
): File? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    try {
        // Background
        paint.color = android.graphics.Color.parseColor("#FCF8F3")
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Border
        val rankColor = FocusRank.getColor(xp)
        val rankColorInt = android.graphics.Color.argb(
            255,
            (rankColor.red * 255).toInt(),
            (rankColor.green * 255).toInt(),
            (rankColor.blue * 255).toInt()
        )
        paint.color = rankColorInt
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 20f
        canvas.drawRect(20f, 20f, 575f, 822f, paint)

        // Content
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true

        // Header
        paint.textSize = 24f
        paint.color = android.graphics.Color.parseColor("#2C3E50")
        canvas.drawText(if(isEnglish) "CERTIFICATE OF DISCIPLINE" else "USTUN DISIPLIN SERTIFIKASI", 297f, 150f, paint)

        // Name
        val nameUpper = name.uppercase()
        if (nameUpper.length > 20) {
            paint.textSize = 24f
        } else {
            paint.textSize = 32f
        }
        paint.color = android.graphics.Color.BLACK
        canvas.drawText(nameUpper, 297f, 300f, paint)

        // Body
        paint.textSize = 16f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.DKGRAY
        val body1 = if(isEnglish) "has demonstrated exceptional focus and productivity," else "olaganustu odaklanma ve uretkenlik gostererek"
        val body2 = if(isEnglish) "achieving the esteemed rank of" else "su unvana layik gorulmustur:"
        canvas.drawText(body1, 297f, 380f, paint)
        canvas.drawText(body2, 297f, 410f, paint)

        // Rank
        paint.textSize = 28f
        paint.isFakeBoldText = true
        paint.color = rankColorInt
        canvas.drawText(FocusRank.getTitle(xp, isEnglish).uppercase(), 297f, 480f, paint)

        // Stats
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.BLACK
        
        val statsStr = if(isEnglish) 
            "Tasks Completed: $tasksDone | Lifetime XP: $xp" 
            else "Tamamlanan Gorev: $tasksDone | Toplam XP: $xp"
        canvas.drawText(statsStr, 297f, 550f, paint)

        // QR Code for Verification
        drawQrCode(canvas, 257f, 600f, 80f, rankColorInt)
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText(if(isEnglish) "VERIFICATION ID: ${xp}-${System.currentTimeMillis()/100000}" else "DOGRULAMA NO: ${xp}-${System.currentTimeMillis()/100000}", 297f, 695f, paint)

        // Seal and Footer
        paint.color = rankColorInt
        canvas.drawCircle(80f, 750f, 30f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("FP", 80f, 756f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Date: $dateStr", 297f, 760f, paint)
        canvas.drawText("Authorized By: FocusPath AI", 500f, 760f, paint)

        pdfDocument.finishPage(page)

        // Sanitize name for filename
        val safeName = name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "FocusPath_Certificate_${safeName}.pdf"
        
        val targetFile = if (isSharing) {
            File(context.cacheDir, fileName)
        } else {
            // Save to Downloads
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Toast.makeText(context, if(isEnglish) "PDF saved to Downloads" else "PDF İndirilenler klasörüne kaydedildi", Toast.LENGTH_LONG).show()
                }
                null
            } else {
                // Legacy saving for API < 29
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { pdfDocument.writeTo(it) }
                Toast.makeText(context, if(isEnglish) "PDF saved to Downloads" else "PDF İndirilenler klasörüne kaydedildi", Toast.LENGTH_LONG).show()
                null
            }
        }
        
        if (isSharing && targetFile != null) {
            FileOutputStream(targetFile).use { pdfDocument.writeTo(it) }
        }

        return targetFile
    } catch (e: Exception) {
        android.util.Log.e("Certificate", "Error saving PDF: ${e.message}")
        return null
    } finally {
        pdfDocument.close()
    }
}

private fun drawQrCode(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    val paint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val cellSize = size / 21f
    
    // Finder Patterns
    drawFinderPattern(canvas, x, y, cellSize, paint)
    drawFinderPattern(canvas, x + size - 7 * cellSize, y, cellSize, paint)
    drawFinderPattern(canvas, x, y + size - 7 * cellSize, cellSize, paint)
    
    val random = java.util.Random(123)
    for (i in 0 until 21) {
        for (j in 0 until 21) {
            val inTopLeft = i < 8 && j < 8
            val inTopRight = i > 12 && j < 8
            val inBottomLeft = i < 8 && j > 12
            if (!inTopLeft && !inTopRight && !inBottomLeft) {
                if (random.nextFloat() > 0.6f) {
                    canvas.drawRect(x + i * cellSize, y + j * cellSize, x + (i + 1) * cellSize, y + (j + 1) * cellSize, paint)
                }
            }
        }
    }
}

private fun drawFinderPattern(canvas: Canvas, x: Float, y: Float, cellSize: Float, paint: Paint) {
    val oldColor = paint.color
    canvas.drawRect(x, y, x + 7 * cellSize, y + 7 * cellSize, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(x + cellSize, y + cellSize, x + 6 * cellSize, y + 6 * cellSize, paint)
    paint.color = oldColor
    canvas.drawRect(x + 2 * cellSize, y + 2 * cellSize, x + 5 * cellSize, y + 5 * cellSize, paint)
}

private fun shareCertificate(context: Context, name: String, xp: Long, focusMins: Int, tasksDone: Int, isEnglish: Boolean) {
    try {
        val file = saveCertificateAsPdf(context, name, xp, focusMins, tasksDone, isEnglish, isSharing = true)
        if (file != null && file.exists()) {
            // Manifest'teki com.focuspath.app.fileprovider ile tam eşleşmeli
            val authority = "com.focuspath.app.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, if(isEnglish) "FocusPath Certificate" else "FocusPath Sertifikası")
                putExtra(Intent.EXTRA_TEXT, if(isEnglish) "Check out my focus progress!" else "Odaklanma ilerlememe göz at!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Yeni Android sürümleri için kritik: Yetkiyi ClipData ile de aktar
                clipData = ClipData.newRawUri(null, uri)
            }
            
            val chooser = Intent.createChooser(intent, if(isEnglish) "Share via" else "Şununla Paylaş")
            // Çökmeyi engellemek için flag ekle
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, if(isEnglish) "No app found to share PDF" else "PDF paylaşacak uygulama bulunamadı", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, if(isEnglish) "Error generating file" else "Dosya oluşturma hatası", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("Certificate", "Share error: ${e.message}")
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
