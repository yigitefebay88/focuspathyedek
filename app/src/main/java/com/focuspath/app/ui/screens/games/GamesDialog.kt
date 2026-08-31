package com.focuspath.app.ui.screens.games

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.focuspath.app.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.airbnb.lottie.compose.*
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun GamesDialog(vm: TaskViewModel, onDismiss: () -> Unit) {
    var activeGame by remember { mutableStateOf<String?>(null) }
    val terminalColor = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = if (activeGame == null) onDismiss else { {} }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(650.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, terminalColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ARKA PLAN GÖRSELİ (Oyun/Teknoloji Temalı)
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1511512578047-dfb367046420?q=80&w=1000&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.15f),
                    contentScale = ContentScale.Crop
                )

                // Karartma Gradyanı
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    // HEADER
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsEsports, null, tint = terminalColor, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (activeGame == null) "DİKKAT OYUNLARI" else activeGame!!.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = terminalColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (activeGame == null) {
                            GameSelectionMenu(vm) { activeGame = it }
                        } else {
                            when (activeGame) {
                                "Stroop Testi" -> StroopGame(vm) { activeGame = null }
                                "Hafıza Kartları" -> MemoryGame(vm) { activeGame = null }
                                "Hızlı Tepki" -> ReactionGame(vm) { activeGame = null }
                                "Küp Takibi" -> CubeCountGame(vm) { activeGame = null }
                                "Okçu Terminali" -> ArcherGame(vm) { activeGame = null }
                                "Sayı Bulmacası" -> NumberPuzzleGame(vm) { activeGame = null }
                                "Görsel Yapboz" -> SlidingPuzzleGame(vm) { activeGame = null }
                                "Desen Tekrarı" -> PatternRepeatGame(vm) { activeGame = null }
                                "Halka ve Çivi" -> RingNailGame(vm) { activeGame = null }
                                "Küp Kulesi" -> CubeTowerGame(vm) { activeGame = null }
                                "Labirent Serüveni" -> MazeGame(vm) { activeGame = null }
                                "Farkı Bul" -> SpotDifferenceGame(vm) { activeGame = null }
                                "Hafıza Matrisi" -> MemoryMatrixGame(vm) { activeGame = null }
                                "Bardak Bulmacası" -> FindBallGame(vm) { activeGame = null }
                                "IQ Testi" -> IQTestGame(vm) { activeGame = null }
                                "Sudoku" -> SudokuGame(vm) { activeGame = null }
                                "Kelime Avcısı" -> WordScrambleGame(vm) { activeGame = null }
                                "Matematik Fırtınası" -> MathBallGame(vm) { activeGame = null }
                                "Görsel Puzzle" -> ShadowMatchGame(vm) { activeGame = null }
                                "Ne Eksik?" -> WhatIsMissingGame(vm) { activeGame = null }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { if (activeGame == null) onDismiss() else activeGame = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (activeGame == null) "Kapat" else "Geri Dön", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GameSelectionMenu(vm: TaskViewModel, onGameSelect: (String) -> Unit) {
    val isPremium = vm.isPremium.value
    val games = listOf(
        Triple("Stroop Testi", "Odaklanma ve Renk Algısı", Icons.Default.Palette),
        Triple("Labirent Serüveni", "Planlama ve Stratejik Dikkat", Icons.Default.Directions),
        Triple("Görsel Yapboz", "Mekansal Zeka ve Odak", Icons.Default.Extension),
        Triple("Küp Takibi", "Görsel Sayım ve Dikkat", Icons.Default.ViewInAr),
        Triple("Okçu Terminali", "Zamanlama ve Hedef Odaklama", Icons.Default.Adjust),
        Triple("Sudoku", "Sayısal Mantık ve Odak", Icons.Default.GridOn),
        Triple("Kelime Avcısı", "Sözel Zeka ve Hız", Icons.Default.Spellcheck),
        Triple("Matematik Fırtınası", "Dört İşlem ve Hız", Icons.Default.Calculate),
        // --- Buradan Sonrası Premium ---
        Triple("Farkı Bul", "Görsel Tarama ve Dikkat", Icons.Default.Search),
        Triple("Hafıza Matrisi", "Görsel-Mekansal Bellek", Icons.Default.GridView),
        Triple("Bardak Bulmacası", "Görsel Takip ve Odak", Icons.Default.SportsBasketball),
        Triple("IQ Testi", "Mantıksal Akıl Yürütme", Icons.Default.Psychology),
        Triple("Desen Tekrarı", "Hafıza ve Sıralı Dikkat", Icons.Default.Pattern),
        Triple("Halka ve Çivi", "Zamanlama ve Yerçekimi", Icons.Default.Download),
        Triple("Küp Kulesi", "Denge ve Hassas Zamanlama", Icons.Default.StackedBarChart),
        Triple("Hafıza Kartları", "Görsel Hafıza Güçlendirme", Icons.Default.Apps),
        Triple("Hızlı Tepki", "Refleks ve Dikkat Takibi", Icons.Default.Bolt),
        Triple("Sayı Bulmacası", "Sayısal Takip ve Hızlı Karar", Icons.Default.Grid4x4),
        Triple("Görsel Puzzle", "Şekil Tanıma ve Dikkat", Icons.Default.FilterCenterFocus),
        Triple("Ne Eksik?", "Görsel Hafıza ve Takip", Icons.Default.VisibilityOff)
    )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        games.forEachIndexed { index, (name, desc, icon) ->
            val isLocked = index >= 8 && !isPremium
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (isLocked) {
                            vm.buyPremium()
                        } else {
                            onGameSelect(name)
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocked) 
                        Color.Gray.copy(alpha = 0.1f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else icon, 
                        contentDescription = null, 
                        tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name + if (isLocked) " (Premium)" else "", 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (isLocked) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HintButton(vm: TaskViewModel, onHint: () -> Unit) {
    val coins = vm.userCoins.value
    val canAfford = coins >= 20

    Button(
        onClick = {
            if (canAfford) {
                vm.addCoins(-20)
                onHint()
            }
        },
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD600).copy(alpha = if (canAfford) 1f else 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.Default.Lightbulb, null, tint = Color.Black, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("İpucu (-20 🪙)", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// --- 1. STROOP GAME ---
@Composable
fun StroopGame(vm: TaskViewModel, onExit: () -> Unit) {
    val allColors = listOf(
        "KIRMIZI" to Color.Red, 
        "MAVİ" to Color.Blue, 
        "YEŞİL" to Color.Green, 
        "SARI" to Color(0xFFFFD600), 
        "MOR" to Color(0xFF9C27B0),
        "TURUNCU" to Color(0xFFFF9800),
        "PEMBE" to Color(0xFFE91E63),
        "TURKUAZ" to Color(0xFF00BCD4)
    )
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    val currentColors = remember(level) { allColors.take((4 + level).coerceAtMost(allColors.size)) }
    var currentPair by remember { mutableStateOf(currentColors.random()) }
    var textColor by remember { mutableStateOf(currentColors.random().second) }
    var isFinished by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(15) }

    LaunchedEffect(score) {
        if (score > 0 && score % 100 == 0) level++
    }

    LaunchedEffect(currentPair, isFinished) {
        if (!isFinished) {
            timeLeft = (15 - (level / 2)).coerceAtLeast(3)
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            // Süre biterse otomatik değiştir ve puan kır
            score = (score - 5).coerceAtLeast(0)
            currentPair = currentColors.random()
            textColor = currentColors.random().second
        }
    }

    if (isFinished) { 
        GameResult(vm, score, "Renk Algısı (LVL $level)") { vm.addCoins(score / 2); onExit() } 
    }
    else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Skor: $score | Seviye: $level", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Süre: $timeLeft", color = if (timeLeft < 5) Color.Red else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }
                TextButton(onClick = { isFinished = true }) { Text("BİTİR VE ÖDÜLÜ AL", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(20.dp)); Text("YAZININ RENGİNİ SEÇ!", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp)); Text(text = currentPair.first, color = textColor, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(currentColors) { (name, color) -> 
                    Button(
                        onClick = { 
                            if (color == textColor) score += 10 else score = (score - 5).coerceAtLeast(0)
                            currentPair = currentColors.random()
                            textColor = currentColors.random().second 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) { Text(name, color = Color.White, fontSize = 10.sp) } 
                }
            }
        }
    }
}

// --- 2. MEMORY GAME ---
@Composable
fun MemoryGame(vm: TaskViewModel, onExit: () -> Unit) {
    val icons = listOf(Icons.Default.Favorite, Icons.Default.Star, Icons.Default.WbSunny, Icons.Default.Bolt, Icons.Default.Settings, Icons.Default.Timer)
    var gameIcons by remember { mutableStateOf((icons + icons).shuffled()) }
    val revealed = remember { mutableStateListOf<Int>() }
    val matched = remember { mutableStateListOf<Int>() }
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(matched.size) { 
        if (matched.size == gameIcons.size && matched.size > 0) {
            delay(1000L)
            score += 50 // Level bonus
            level++
            matched.clear()
            revealed.clear()
            gameIcons = (icons + icons).shuffled()
        }
    }

    LaunchedEffect(revealed.size) { if (revealed.size == 2) { delay(800L); if (gameIcons[revealed[0]] == gameIcons[revealed[1]]) { matched.addAll(revealed); score += 20 }; revealed.clear() } }
    
    if (isFinished) { 
        GameResult(vm, score, "Görsel Hafıza (LVL $level)") { vm.addCoins(score / 5); onExit() } 
    }
    else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { 
                    Text("Puan: $score", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Seviye: $level", style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    HintButton(vm) { val oldRevealed = revealed.toList(); revealed.clear(); revealed.addAll(gameIcons.indices); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ revealed.clear(); revealed.addAll(oldRevealed) }, 1000) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = false) {
                items(gameIcons.size) { index -> val isRevealed = revealed.contains(index) || matched.contains(index); Box(modifier = Modifier.aspectRatio(1f).background(if (isRevealed) MaterialTheme.colorScheme.primary.copy(0.2f) else Color.DarkGray, RoundedCornerShape(8.dp)).clickable(enabled = !isRevealed && revealed.size < 2) { revealed.add(index) }, contentAlignment = Alignment.Center) { if (isRevealed) Icon(gameIcons[index], null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) else Text("?", color = Color.White, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

// --- 3. REACTION GAME ---
@Composable
fun ReactionGame(vm: TaskViewModel, onExit: () -> Unit) {
    var gameState by remember { mutableStateOf("WAIT") }
    var startTime by remember { mutableLongStateOf(0L) }
    var reactionTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(gameState) { if (gameState == "READY") { delay(Random.nextLong(2000, 5000)); if (gameState == "READY") { gameState = "NOW"; startTime = System.currentTimeMillis() } } }
    Box(modifier = Modifier.fillMaxSize().background(when (gameState) { "READY" -> Color(0xFFD32F2F); "NOW" -> Color(0xFF388E3C); else -> Color.Transparent }, RoundedCornerShape(12.dp)).clickable { when (gameState) { "WAIT" -> gameState = "READY"; "READY" -> { gameState = "WAIT" }; "NOW" -> { reactionTime = System.currentTimeMillis() - startTime; gameState = "RESULT" }; "RESULT" -> gameState = "WAIT" } }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = when (gameState) { "WAIT" -> "BAŞLAMAK İÇİN DOKUN"; "READY" -> "BEKLE..."; "NOW" -> "DOKUN!!!"; "RESULT" -> "TEPKİ SÜREN:\n$reactionTime ms"; else -> "" }, color = if (gameState == "READY" || gameState == "NOW") Color.White else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (gameState == "RESULT") { Spacer(Modifier.height(20.dp)); Button(onClick = { vm.addCoins(if (reactionTime < 300) 15 else 5); onExit() }) { Text("Ödülü Al ve Çık") } }
        }
    }
}

// --- 4. CUBE COUNT GAME ---
@Composable
fun CubeCountGame(vm: TaskViewModel, onExit: () -> Unit) {
    var gameState by remember { mutableStateOf("PREVIEW") }
    val cubeColors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)
    val counts = remember { mutableStateMapOf<Int, Int>() }
    var activeCubeIndex by remember { mutableIntStateOf(-1) }
    var targetColorIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(gameState) {
        if (gameState == "FLASHING") {
            counts.clear()
            selectedOption = null
            repeat(10 + level) {
                val r = Random.nextInt(4)
                counts[r] = (counts[r] ?: 0) + 1
                activeCubeIndex = r
                delay(700L - (level * 20L).coerceAtMost(400L))
                activeCubeIndex = -1
                delay(200L)
            }
            targetColorIndex = Random.nextInt(4)
            gameState = "QUESTION"
        }
    }

    if (isFinished) {
        GameResult(vm, score, "Küp Takibi (LVL $level)") { vm.addCoins(score / 2); onExit() }
    } else {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Skor: $score | Seviye: $level", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
            }

            when (gameState) {
                "PREVIEW" -> {
                    Spacer(Modifier.height(40.dp))
                    Text("DİKKATLE İZLE!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Hangi renk küpün kaç kez yandığını soracağım.", textAlign = TextAlign.Center, fontSize = 12.sp)
                    Spacer(Modifier.height(40.dp))
                    Button(onClick = { gameState = "FLASHING" }) { Text("BAŞLAT") }
                }
                "FLASHING" -> {
                    Spacer(Modifier.height(40.dp))
                    Text("SAYIYOR MUSUN?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(40.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        cubeColors.forEachIndexed { index, color ->
                            Box(modifier = Modifier.size(50.dp).background(if (activeCubeIndex == index) color else color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).border(2.dp, if (activeCubeIndex == index) color else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)))
                        }
                    }
                }
                "QUESTION" -> {
                    val targetColorName = when(targetColorIndex) { 0 -> "KIRMIZI" ; 1 -> "MAVİ" ; 2 -> "YEŞİL" ; 3 -> "SARI" ; else -> "" }
                    Text("SORU VAKTİ!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "$targetColorName küp kaç kez yandı?", color = cubeColors[targetColorIndex], fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Spacer(Modifier.height(24.dp))
                    val correctAnswer = counts[targetColorIndex] ?: 0
                    val options = remember(correctAnswer) {
                        val list = mutableListOf(correctAnswer)
                        while (list.size < 4) {
                            val fake = (correctAnswer + Random.nextInt(-3, 4)).coerceAtLeast(0)
                            if (!list.contains(fake)) list.add(fake)
                        }
                        list.shuffled()
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(options) { opt ->
                            val isCorrect = opt == correctAnswer
                            val color = if (selectedOption != null) {
                                if (isCorrect) Color.Green else if (opt == selectedOption) Color.Red else Color.Gray
                            } else MaterialTheme.colorScheme.primary

                            Button(
                                onClick = {
                                    if (selectedOption == null) {
                                        selectedOption = opt
                                        if (isCorrect) {
                                            score += 25
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                level++
                                                gameState = "PREVIEW"
                                            }, 1000)
                                        } else {
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                gameState = "PREVIEW"
                                            }, 1000)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = color)
                            ) { Text("$opt", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

// --- 5. ARCHER GAME ---
@Composable
fun ArcherGame(vm: TaskViewModel, onExit: () -> Unit) {
    var targetX by remember { mutableFloatStateOf(0f) } ; var direction by remember { mutableIntStateOf(1) } ; var arrowY by remember { mutableFloatStateOf(0f) } ; var isFiring by remember { mutableStateOf(false) } ; var score by remember { mutableIntStateOf(0) } ; var shotsLeft by remember { mutableIntStateOf(5) } ; var gameMessage by remember { mutableStateOf("HEDEFE ODAKLAN VE DOKUN!") }
    var isFinished by remember { mutableStateOf(false) }
    val terminalColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(shotsLeft) { 
        if (shotsLeft > 0) { 
            while (!isFiring) { 
                delay(16)
                // Hız artışını dengeledik (0.005f -> 0.0004f) ve maksimum hız sınırı koyduk (0.12f)
                val currentSpeed = (0.02f + (score * 0.0004f)).coerceAtMost(0.12f)
                targetX += currentSpeed * direction; 
                if (targetX > 0.8f || targetX < -0.8f) direction *= -1 
            } 
        } else {
            // Refill arrows automatically
            delay(1000)
            shotsLeft = 5
            gameMessage = "OKLAR YENİLENDİ!"
        }
    }

    LaunchedEffect(isFiring) { 
        if (isFiring) { 
            while (arrowY > -0.92f) { 
                delay(10)
                arrowY -= 0.04f 
            }
            val hitThreshold = 0.15f
            if (kotlin.math.abs(0f - targetX) < hitThreshold) { 
                score += 50; gameMessage = "TAM İSABET! 🎯" 
            } else { 
                gameMessage = "ISKALADIN! 💨"
                while (arrowY > -1.2f) { delay(10); arrowY -= 0.05f }
            }
            delay(800); isFiring = false; arrowY = 0f; shotsLeft--
            if (shotsLeft > 0) gameMessage = "SIRADAKİ ATIŞ..." 
        } 
    }

    if (isFinished) { GameResult(vm, score, "Okçu Terminali") { vm.addCoins(score / 10); onExit() } }
    else {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Skor: $score", fontWeight = FontWeight.Bold, color = terminalColor)
                TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
            }
            Text("Kalan Ok: $shotsLeft", fontWeight = FontWeight.Bold); Text(gameMessage, style = MaterialTheme.typography.labelSmall, color = terminalColor)
            Box(modifier = Modifier.height(350.dp).fillMaxWidth().padding(20.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).border(1.dp, terminalColor.copy(0.3f), RoundedCornerShape(12.dp)).clickable(enabled = !isFiring) { isFiring = true }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.align(Alignment.TopCenter).offset(x = (targetX * 150).dp, y = 20.dp).size(40.dp).background(Color.Red, CircleShape).border(4.dp, Color.White, CircleShape)) { Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape).align(Alignment.Center)) }
                Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (arrowY * 300).dp).size(4.dp, 40.dp).background(if (isFiring) terminalColor else Color.Gray)) { Box(modifier = Modifier.align(Alignment.TopCenter).size(10.dp).graphicsLayer { rotationZ = 45f }.border(2.dp, if (isFiring) terminalColor else Color.Gray)) }
            }
            Spacer(Modifier.weight(1f))
            Text("Ateş Etmek İçin Ekrana Dokun!", fontSize = 10.sp, color = Color.Gray); Spacer(Modifier.height(10.dp))
        }
    }
}

// --- 6. SUDOKU GAME ---
@Composable
fun SudokuGame(vm: TaskViewModel, onExit: () -> Unit) {
    val fixedValues = remember { listOf(listOf(1, 0, 3, 0), listOf(0, 0, 0, 2), listOf(0, 1, 0, 0), listOf(0, 0, 2, 0)) }
    val userBoard = remember { listOf(mutableStateListOf(1, 0, 3, 0), mutableStateListOf(0, 0, 0, 2), mutableStateListOf(0, 1, 0, 0), mutableStateListOf(0, 0, 2, 0)) }
    val solution = listOf(listOf(1, 2, 3, 4), listOf(3, 4, 1, 2), listOf(2, 1, 4, 3), listOf(4, 3, 2, 1))
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var timeLeft by remember { mutableIntStateOf(60) } ; var isGameOver by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    val isWin by remember { derivedStateOf { userBoard.indices.all { r -> userBoard[r].indices.all { c -> userBoard[r][c] == solution[r][c] } } } }

    LaunchedEffect(isWin) {
        if (isWin) {
            delay(1500)
            // Reset for infinite mode
            userBoard.forEachIndexed { r, row ->
                row.clear()
                row.addAll(fixedValues[r])
            }
            timeLeft = (timeLeft + 30).coerceAtMost(120)
        }
    }

    LaunchedEffect(isWin, isGameOver, isFinished) {
        if (!isWin && !isGameOver && !isFinished) {
            while (timeLeft > 0) { delay(1000L); timeLeft-- };
            if (timeLeft == 0) isGameOver = true
        } 
    }

    if (isGameOver || isFinished) {
        val finalScore = if (isWin) 100 + (timeLeft * 2) else 0;
        GameResult(vm, finalScore, if (isFinished) "Sudoku" else "Süre Bitti!") {
            if (finalScore > 0) vm.addCoins(30 + (timeLeft / 2)) else vm.addCoins(-20); onExit()
        }
    }
    else {
        val terminalColor = MaterialTheme.colorScheme.primary
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("SUDOKU (4x4)", fontWeight = FontWeight.Bold); Text("Zamana Karşı!", style = MaterialTheme.typography.labelSmall, color = terminalColor) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SÜRE: $timeLeft", color = if (timeLeft < 10) Color.Red else terminalColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(15.dp))
            Column(modifier = Modifier.background(Color.Black.copy(0.1f)).border(2.dp, terminalColor.copy(0.4f))) {
                repeat(4) { r -> Row { repeat(4) { c ->
                    val isInitial = fixedValues[r][c] != 0 ; val value = userBoard[r][c] ; val isWrong = value != 0 && value != solution[r][c] ; val isSelected = selectedCell == r to c
                    Box(modifier = Modifier.size(60.dp).border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) terminalColor else terminalColor.copy(0.2f)).background(when { isWrong -> Color.Red.copy(alpha = 0.3f); isSelected -> terminalColor.copy(0.2f); else -> Color.Transparent }).clickable(enabled = !isInitial) { selectedCell = r to c }, contentAlignment = Alignment.Center) { Text(text = if (value == 0) "" else value.toString(), fontWeight = if (isInitial) FontWeight.ExtraBold else FontWeight.Bold, color = when { isInitial -> terminalColor; isWrong -> Color.Red; else -> Color.White }, fontSize = 22.sp) }
                } } }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..4).forEach { num -> Button(onClick = { selectedCell?.let { (r, c) -> userBoard[r][c] = num } }, modifier = Modifier.size(50.dp), contentPadding = PaddingValues(0.dp), enabled = selectedCell != null) { Text(num.toString(), fontWeight = FontWeight.Bold) } } ; Button(onClick = { selectedCell?.let { (r, c) -> userBoard[r][c] = 0 } }, modifier = Modifier.size(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.7f)), contentPadding = PaddingValues(0.dp), enabled = selectedCell != null) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(24.dp), tint = Color.White) } }
        }
    }
}

// --- 7. WORD SCRAMBLE GAME (Sonsuz Mod) ---
@Composable
fun WordScrambleGame(vm: TaskViewModel, onExit: () -> Unit) {
    val wordList = listOf("ODAK", "ZİHİN", "DİKKAT", "BAŞARI", "HAFIZA", "MANTIK", "BİLGİ", "ZEKA", "DİSİPLİN", "GELİŞİM", "SABIR", "AZİM", "HEDEF", "PLAN", "ZAMAN", "VERİMLİ", "ANALİZ", "STRATEJİ", "ÇÖZÜM", "SİSTEM", "DÜZEN", "HUZUR", "MUTLULUK", "ÖZGÜVEN", "CESARET", "MERAK", "ÖĞRENME", "EĞİTİM", "KİTAP", "ANLAM", "KAVRAM", "TEKNOLOJİ", "GELECEK", "HAYAL", "GERÇEK", "BİLİM", "SANAT", "KÜLTÜR", "DOĞA", "EVREN", "YAŞAM", "İNSAN", "TOPLUM", "DOSTLUK", "SEVGİ", "SAYGI", "YARDIM", "BİRLİK", "GÜÇ", "KUVVET", "SAĞLIK", "ENERJİ", "NEFES", "GÜZEL", "HARİKA", "MÜKEMMEL", "ÖNEMLİ", "DEĞERLİ", "ÖZEL", "YARATICI", "ÜRETKEN", "AKTİF", "CANLI", "HIZLI", "SAKİN", "DİNGİN", "DERİN", "GENİŞ", "TÜRKİYE", "KÜRESEL", "VİZYON", "MİSYON", "DEĞİŞİM", "KONTROL", "DENGE", "ADALET", "ÖZGÜRLÜK", "BARIŞ", "UMUT", "IŞIK", "GÜNEŞ", "YILDIZ", "DENİZ", "ORMAN", "TOPRAK", "HAVA", "ATEŞ", "MİLLİ", "TARİH", "COĞRAFYA", "FELSEFE", "EDEBİYAT", "MÜZİK", "SİNEMA", "TİYATRO", "RESİM", "HEYKEL", "MİMARİ")

    var currentWord by remember { mutableStateOf(wordList.random()) }
    val scrambledLetters = remember { mutableStateListOf<Char>() }
    val userLetters = remember { mutableStateListOf<Char>() }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var isGameOver by remember { mutableStateOf(false) }
    var totalWordsSolved by remember { mutableIntStateOf(0) }
    var isQuitPressed by remember { mutableStateOf(false) }

    val terminalColor = MaterialTheme.colorScheme.primary

    // Başlangıç Kurulumu
    LaunchedEffect(currentWord) {
        scrambledLetters.clear()
        scrambledLetters.addAll(currentWord.toList().shuffled())
        userLetters.clear()
    }

    // Zamanlayıcı
    LaunchedEffect(isGameOver, isQuitPressed) {
        if (!isGameOver && !isQuitPressed) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            isGameOver = true
        }
    }

    // Kazanma Kontrolü (Sonsuz geçiş)
    LaunchedEffect(userLetters.size) {
        if (userLetters.joinToString("") == currentWord) {
            score += 50
            totalWordsSolved++
            timeLeft = (timeLeft + 15).coerceAtMost(60) // Bilince 15 sn ekle
            currentWord = wordList.random()
        }
    }

    if (isGameOver || isQuitPressed) {
        GameResult(vm, score, if (isQuitPressed) "Kelime Avcısı (Bitirildi)" else "Süre Bitti!") {
            vm.addCoins(score / 5)
            onExit()
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SKOR: $score", fontWeight = FontWeight.Bold, color = terminalColor)
                    Text("BİLİNEN: $totalWordsSolved", fontSize = 10.sp, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏳ $timeLeft", color = if (timeLeft < 7) Color.Red else terminalColor, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(16.dp))
                    IconButton(
                        onClick = { isQuitPressed = true },
                        modifier = Modifier.size(32.dp).background(Color.Red.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Logout, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text("Harfleri doğru sıraya diz!", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(40.dp))

            // Kullanıcının oluşturduğu kelime alanı
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentWord.forEachIndexed { i, _ ->
                    val char = userLetters.getOrNull(i)
                    // HATA KONTROLÜ: Girilen harf asıl kelimenin o sıradaki harfiyle eşleşmiyor mu?
                    val isWrong = char != null && char != currentWord[i]

                    Box(
                        modifier = Modifier
                            .size(if (currentWord.length > 7) 38.dp else 45.dp)
                            .padding(2.dp)
                            .border(
                                width = if (isWrong) 2.dp else 1.dp,
                                color = if (isWrong) Color.Red else terminalColor.copy(0.5f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(
                                when {
                                    isWrong -> Color.Red.copy(alpha = 0.2f)
                                    char != null -> terminalColor.copy(0.1f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { if (char != null) {
                                scrambledLetters.add(char)
                                userLetters.removeAt(i)
                            }},
                        contentAlignment = Alignment.Center
                    ) {
                        if (char != null) Text(
                            char.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (currentWord.length > 7) 16.sp else 20.sp,
                            color = if (isWrong) Color.Red else Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // Karışık harfler alanı
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                scrambledLetters.forEachIndexed { i, char ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(terminalColor)
                            .clickable {
                                userLetters.add(char)
                                scrambledLetters.removeAt(i)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char.toString(), color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text("Çıkmak için sağ üstteki butona dokun", fontSize = 10.sp, color = Color.Gray)
            Spacer(Modifier.height(10.dp))
        }
    }
}

// --- 8. MATH BALL GAME ---
@Composable
fun MathBallGame(vm: TaskViewModel, onExit: () -> Unit) {
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var currentQuestion by remember { mutableStateOf(generateMathQuestion(level)) }
    var isFinished by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(20) }
    val terminalColor = MaterialTheme.colorScheme.primary

    // Zamanlayıcı Efekti
    LaunchedEffect(currentQuestion, isFinished) {
        if (!isFinished) {
            timeLeft = (20 - (level / 2)).coerceAtLeast(5) // Seviye arttıkça süre azalır
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            // Süre biterse yeni soruya geç ama ceza puanı ver
            score = (score - 10).coerceAtLeast(0)
            currentQuestion = generateMathQuestion(level)
        }
    }
    
    if (isFinished) { GameResult(vm, score, "Matematik Fırtınası (LVL $level)") { vm.addCoins(score / 5); onExit() } }
    else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                Column {
                    Text("Skor: $score | Seviye: $level", fontWeight = FontWeight.Bold, color = terminalColor)
                    Text("Süre: $timeLeft", color = if (timeLeft < 5) Color.Red else terminalColor, fontWeight = FontWeight.ExtraBold)
                }
                TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(30.dp)); Text("HESAPLA VE DOĞRU TOPA DOKUN!", style = MaterialTheme.typography.labelSmall); Spacer(Modifier.height(10.dp)); Text(text = "${currentQuestion.firstNum} ${currentQuestion.op} ${currentQuestion.secondNum} = ?", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = terminalColor)
            Spacer(Modifier.weight(1f))
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.width(220.dp).height(240.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), userScrollEnabled = false) { 
                items(currentQuestion.options) { option -> 
                    val ballColor = remember { listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan).random() }
                    Box(modifier = Modifier.aspectRatio(1f).clip(CircleShape).background(ballColor.copy(alpha = 0.8f)).border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape).clickable { 
                        if (option == currentQuestion.correctAnswer) {
                            score += 10
                            if (score > 0 && score % 50 == 0) level++
                        } else {
                            score = (score - 5).coerceAtLeast(0)
                        }
                        currentQuestion = generateMathQuestion(level) 
                    }, contentAlignment = Alignment.Center) { 
                        Text(text = option.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) 
                    } 
                } 
            }
        }
    }
}
data class MathQuestion(val firstNum: Int, val secondNum: Int, val op: String, val correctAnswer: Int, val options: List<Int>)
fun generateMathQuestion(level: Int): MathQuestion {
    val ops = if (level < 3) listOf("+", "-") else listOf("+", "-", "*")
    val op = ops.random()
    val maxNum = 10 + (level * 5)
    var n1 = Random.nextInt(1, maxNum)
    var n2 = Random.nextInt(1, maxNum)
    if (op == "*") {
        n1 = Random.nextInt(1, 5 + level)
        n2 = Random.nextInt(1, 5 + level)
    }
    val correct = when(op) { "+" -> n1 + n2; "-" -> n1 - n2; "*" -> n1 * n2; else -> n1 + n2 }
    val options = mutableListOf(correct)
    while (options.size < 4) {
        val fake = correct + Random.nextInt(-15, 16)
        if (!options.contains(fake)) options.add(fake)
    }
    return MathQuestion(n1, n2, op, correct, options.shuffled())
}

// --- 9. SPOT DIFFERENCE GAME ---
@Composable
fun SpotDifferenceGame(vm: TaskViewModel, onExit: () -> Unit) {
    val sets = listOf(Pair("0", "O"), Pair("M", "N"), Pair("E", "F"), Pair("P", "R"), Pair("8", "B"), Pair("K", "X"), Pair("I", "L"), Pair("5", "S"))
    var level by remember { mutableIntStateOf(1) } ; var score by remember { mutableIntStateOf(0) } ; var currentSet by remember { mutableStateOf(sets.random()) }
    val gridSize = when { level <= 5 -> 6 ; level <= 10 -> 8 ; else -> 10 }
    var differentIndex by remember { mutableIntStateOf(Random.nextInt(gridSize * gridSize)) }
    var timeLeft by remember { mutableIntStateOf(30) } ; var isGameOver by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(currentSet, isGameOver, isFinished) {
        if (!isGameOver && !isFinished) {
            timeLeft = (30 - (level / 2)).coerceAtLeast(5)
            while (timeLeft > 0) { delay(1000L); timeLeft-- };
            if (timeLeft == 0) isGameOver = true
        }
    }

    if (isGameOver || isFinished) {
        GameResult(vm, score, if (isFinished) "Farkı Bul (LVL $level)" else "Süre Bitti!") { vm.addCoins(score / 4); onExit() }
    }
    else {
        val terminalColor = MaterialTheme.colorScheme.primary
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SEVİYE: $level", fontWeight = FontWeight.Bold)
                    Text("Puan: $score", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SÜRE: $timeLeft", color = if (timeLeft < 7) Color.Red else terminalColor, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
            }
            Text("Farklı olan sembolü bul!", style = MaterialTheme.typography.labelSmall, color = terminalColor)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(gridSize), modifier = Modifier.weight(1f).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), userScrollEnabled = false) {
                items(gridSize * gridSize) { index ->
                    val isDifferent = index == differentIndex
                    Box(modifier = Modifier.aspectRatio(1f).background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).border(1.dp, Color.White.copy(alpha = 0.05f)).clickable(enabled = !isGameOver) {
                        if (isDifferent) {
                            score += 20
                            level++
                            timeLeft = (timeLeft + 5).coerceAtMost(30)
                            currentSet = sets.random()
                            differentIndex = Random.nextInt(gridSize * gridSize)
                        } else {
                            score = (score - 10).coerceAtLeast(0)
                        }
                    }, contentAlignment = Alignment.Center) {
                        Text(text = if (isDifferent) currentSet.second else currentSet.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if(gridSize > 8) 12.sp else 18.sp)
                    }
                }
            }
        }
    }
}

// --- 10. MEMORY MATRIX GAME ---
@Composable
fun MemoryMatrixGame(vm: TaskViewModel, onExit: () -> Unit) {
    var level by remember { mutableIntStateOf(1) } ; var score by remember { mutableIntStateOf(0) } ; var gameState by remember { mutableStateOf("PREVIEW") }
    var isFinished by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(10) }
    val gridSize = when { level <= 2 -> 3; level <= 4 -> 4; else -> 5 } ; val activeTileCount = level + 2
    val activeTiles = remember(level, gameState) { if (gameState == "SHOWING" || gameState == "PREVIEW") (0 until (gridSize * gridSize)).toList().shuffled().take(activeTileCount) else emptyList() }
    val currentTargetTiles = remember { mutableStateListOf<Int>() } ; val userSelectedTiles = remember { mutableStateListOf<Int>() }

    LaunchedEffect(gameState, isFinished) {
        if (gameState == "SHOWING") {
            currentTargetTiles.clear(); currentTargetTiles.addAll(activeTiles); userSelectedTiles.clear(); delay(2000); gameState = "PLAYING"
        }
        if (gameState == "PLAYING" && !isFinished) {
            timeLeft = (10 - (level / 3)).coerceAtLeast(3)
            while (timeLeft > 0 && gameState == "PLAYING") {
                delay(1000L)
                timeLeft--
            }
            if (timeLeft == 0 && gameState == "PLAYING") gameState = "GAME_OVER"
        }
    }

    if (isFinished || gameState == "GAME_OVER") { GameResult(vm, score, "Hafıza Matrisi (LVL $level)") { vm.addCoins(score / 5); onExit() } }
    else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SEVİYE: $level", fontWeight = FontWeight.Bold)
                    if (gameState == "PLAYING") Text("SÜRE: $timeLeft", color = if (timeLeft < 3) Color.Red else MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HintButton(vm) { gameState = "SHOWING" }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
            }
            Text(text = when(gameState) { "PREVIEW" -> "HAZIR MISIN?"; "SHOWING" -> "YEŞİL KARELERİ HATIRLA!"; "PLAYING" -> "KARELERİ SEÇ!"; else -> "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))
            if (gameState == "PREVIEW") { Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Button(onClick = { gameState = "SHOWING" }) { Text("BAŞLAT") } } }
            else { LazyVerticalGrid(columns = GridCells.Fixed(gridSize), modifier = Modifier.size(260.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), userScrollEnabled = false) { items(gridSize * gridSize) { index -> val isTarget = currentTargetTiles.contains(index); val isSelected = userSelectedTiles.contains(index); Box(modifier = Modifier.aspectRatio(1f).background(when { gameState == "SHOWING" && isTarget -> Color.Green; gameState == "PLAYING" && isSelected -> if (currentTargetTiles.contains(index)) Color.Green else Color.Red; else -> Color.DarkGray.copy(alpha = 0.3f) }, RoundedCornerShape(4.dp)).clickable(enabled = gameState == "PLAYING" && !isSelected) { userSelectedTiles.add(index); if (!currentTargetTiles.contains(index)) gameState = "GAME_OVER" else if (userSelectedTiles.size == currentTargetTiles.size) { score += level * 20; level++; gameState = "PREVIEW" } }) } } }
        }
    }
}

// --- 11. FIND BALL GAME ---
@Composable
fun FindBallGame(vm: TaskViewModel, onExit: () -> Unit) {
    var gameState by remember { mutableStateOf("PREVIEW") } ; var ballCupIndex by remember { mutableIntStateOf(Random.nextInt(3)) } ; var selectedCupIndex by remember { mutableStateOf<Int?>(null) } ; var score by remember { mutableIntStateOf(0) } ; val cupPositions = remember { mutableStateListOf(0f, 1f, 2f) }
    var isFinished by remember { mutableStateOf(false) }
    LaunchedEffect(gameState) { if (gameState == "SHUFFLING") { delay(1000); repeat(10) { val idx1 = Random.nextInt(3); var idx2 = Random.nextInt(3); while (idx1 == idx2) idx2 = Random.nextInt(3); val temp = cupPositions[idx1]; cupPositions[idx1] = cupPositions[idx2]; cupPositions[idx2] = temp; delay(450) }; delay(500); gameState = "GUESSING" } }
    val terminalColor = MaterialTheme.colorScheme.primary

    if (isFinished) { GameResult(vm, score, "Bardak Bulmacası") { vm.addCoins(score / 5); onExit() } }
    else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
            }
            Text(text = when(gameState) { "PREVIEW" -> "TOPU UNUTMA!"; "SHUFFLING" -> "DİKKATLE TAKİP ET..."; "GUESSING" -> "TOP HANGİ BARDAKTA?"; else -> "SONUÇ" }, fontWeight = FontWeight.Bold, color = terminalColor); Spacer(Modifier.height(50.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                val ballPos = cupPositions[ballCupIndex] ; val animatedBallX by animateFloatAsState(targetValue = ballPos, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing), label = "ball_move")
                Box(modifier = Modifier.offset(x = (animatedBallX - 1f).dp * 100, y = 40.dp).size(30.dp).background(Color.Red, CircleShape).border(2.dp, Color.White, CircleShape).graphicsLayer { alpha = if (gameState == "PREVIEW" || gameState == "RESULT") 1f else 0f })
                repeat(3) { id -> val currentPos = cupPositions[id] ; val animatedX by animateFloatAsState(targetValue = currentPos, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing), label = "cup_move"); val isLifted = gameState == "PREVIEW" || (gameState == "RESULT" && id == ballCupIndex); val animatedY by animateFloatAsState(targetValue = if (isLifted) -70f else 0f, animationSpec = tween(durationMillis = 400), label = "cup_lift"); Box(modifier = Modifier.offset(x = (animatedX - 1f).dp * 100, y = animatedY.dp).size(75.dp, 95.dp).clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(Color(0xFF424242)).border(2.dp, terminalColor.copy(0.6f), RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).clickable(enabled = gameState == "GUESSING") { selectedCupIndex = id; if (id == ballCupIndex) score += 100; gameState = "RESULT" }, contentAlignment = Alignment.BottomCenter) { Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.Black.copy(0.3f))) } }
            }
            Spacer(Modifier.height(40.dp))
            if (gameState == "PREVIEW") { Button(onClick = { gameState = "SHUFFLING" }, colors = ButtonDefaults.buttonColors(containerColor = terminalColor)) { Text("KARIŞTIR", color = Color.Black, fontWeight = FontWeight.Bold) } }
            else if (gameState == "RESULT") { val isCorrect = selectedCupIndex == ballCupIndex; Text(text = if (isCorrect) "HARİKA! DOĞRU BARDAK! 🏆" else "ÜZGÜNÜM, YANLIŞ TERCİH! ❌", color = if (isCorrect) Color.Green else Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Spacer(Modifier.height(20.dp)); Button(onClick = { gameState = "PREVIEW"; ballCupIndex = Random.nextInt(3); selectedCupIndex = null }) { Text("DEVAM ET") } }
        }
    }
}

// --- 12. MAZE GAME ---
@Composable
fun MazeGame(vm: TaskViewModel, onExit: () -> Unit) {
    var difficulty by remember { mutableStateOf<String?>(null) }
    val terminalColor = MaterialTheme.colorScheme.primary

    if (difficulty == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LABİRENT ZORLUĞU SEÇ", fontWeight = FontWeight.Bold, color = terminalColor, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            listOf("KOLAY" to "EASY", "ORTA" to "MEDIUM", "ZOR" to "HARD").forEach { (label, diff) ->
                Button(onClick = { difficulty = diff }, modifier = Modifier.fillMaxWidth(0.7f).height(48.dp), shape = RoundedCornerShape(8.dp)) { Text(label) }
                Spacer(Modifier.height(12.dp))
            }
            TextButton(onClick = onExit) { Text("İPTAL", color = Color.Gray) }
        }
    } else {
        val size = when(difficulty) { "EASY" -> 10 ; "MEDIUM" -> 12 ; else -> 14 }
        var playerX by remember { mutableIntStateOf(0) }
        var playerY by remember { mutableIntStateOf(0) }
        var score by remember { mutableIntStateOf(0) }
        var isFinished by remember { mutableStateOf(false) }
        var mazeIndex by remember { mutableIntStateOf(0) }
        var timeLeft by remember { mutableIntStateOf(when(difficulty) { "EASY" -> 60 ; "MEDIUM" -> 45 ; else -> 30 }) }
        var isGameOver by remember { mutableStateOf(false) }

        val mazes = remember(difficulty) {
            when(difficulty) {
                "MEDIUM" -> listOf(
                    listOf(
                        listOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
                        listOf(1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1),
                        listOf(1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1),
                        listOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1),
                        listOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
                        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)
                    ),
                    listOf(
                        listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                        listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
                        listOf(1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1),
                        listOf(1, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1),
                        listOf(1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1),
                        listOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1),
                        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1),
                        listOf(1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1),
                        listOf(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0)
                    )
                )
                "HARD" -> listOf(
                    listOf(
                        listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                        listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1),
                        listOf(1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
                        listOf(1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1),
                        listOf(1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1),
                        listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1),
                        listOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    )
                )
                else -> listOf(
                    listOf(
                        listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                        listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
                        listOf(1, 1, 1, 0, 1, 0, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0),
                        listOf(1, 0, 1, 1, 1, 1, 1, 0, 1, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0),
                        listOf(1, 0, 1, 0, 1, 1, 1, 1, 1, 0),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    ),
                    listOf(
                        listOf(0, 0, 0, 1, 1, 1, 1, 1, 1, 1),
                        listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1),
                        listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 0, 1, 1, 1, 1, 1, 1, 1, 1),
                        listOf(1, 0, 0, 0, 0, 0, 1, 1, 1, 1),
                        listOf(1, 1, 1, 1, 1, 0, 1, 1, 1, 1),
                        listOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 1),
                        listOf(1, 0, 0, 0, 1, 1, 1, 1, 0, 1),
                        listOf(1, 0, 1, 1, 1, 1, 1, 1, 0, 0)
                    )
                )
            }
        }

        val currentMaze = mazes[mazeIndex % mazes.size]
        val isWin = playerX == size - 1 && playerY == size - 1

        LaunchedEffect(isWin) {
            if (isWin) {
                delay(500L)
                score += 50
                playerX = 0
                playerY = 0
                mazeIndex++
                timeLeft = when(difficulty) { "EASY" -> 60 ; "MEDIUM" -> 45 ; else -> 30 }
            }
        }

        LaunchedEffect(difficulty, isFinished, isGameOver) {
            if (!isFinished && !isGameOver) {
                while (timeLeft > 0) {
                    delay(1000L)
                    timeLeft--
                }
                if (timeLeft == 0) isGameOver = true
            }
        }

        if (isFinished || isGameOver) {
            GameResult(vm, score, if (isGameOver) "Süre Bitti!" else "Labirent Serüveni") { vm.addCoins(score / 5); onExit() }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Puan: $score", fontWeight = FontWeight.Bold, color = terminalColor)
                        Text("Süre: $timeLeft", color = if (timeLeft < 10) Color.Red else terminalColor, fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = { isFinished = true }) { Text("BİTİR", color = Color.Red, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(280.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).border(1.dp, terminalColor.copy(0.3f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                    val cellSize = 272.dp / size
                    Column {
                        for (y in 0 until size) {
                            Row {
                                for (x in 0 until size) {
                                    val isWall = currentMaze[y][x] == 1 ; val isPlayer = x == playerX && y == playerY ; val isGoal = x == size - 1 && y == size - 1
                                    Box(modifier = Modifier.size(cellSize).padding(1.dp).background(if (isWall) terminalColor.copy(0.8f) else Color.Transparent, RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                                        if (isPlayer) Text("🐭", fontSize = (cellSize.value * 0.6).sp)
                                        if (isGoal) Text("🧀", fontSize = (cellSize.value * 0.6).sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { if (playerY > 0 && currentMaze[playerY - 1][playerX] == 0) playerY-- }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = terminalColor, modifier = Modifier.size(36.dp)) }
                    Row {
                        IconButton(onClick = { if (playerX > 0 && currentMaze[playerY][playerX - 1] == 0) playerX-- }) { Icon(Icons.Default.KeyboardArrowLeft, null, tint = terminalColor, modifier = Modifier.size(36.dp)) }
                        Spacer(Modifier.width(30.dp))
                        IconButton(onClick = { if (playerX < size - 1 && currentMaze[playerY][playerX + 1] == 0) playerX++ }) { Icon(Icons.Default.KeyboardArrowRight, null, tint = terminalColor, modifier = Modifier.size(36.dp)) }
                    }
                    IconButton(onClick = { if (playerY < size - 1 && currentMaze[playerY + 1][playerX] == 0) playerY++ }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = terminalColor, modifier = Modifier.size(36.dp)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// --- 13. IQ TEST GAME ---
@Composable
fun IQTestGame(vm: TaskViewModel, onExit: () -> Unit) {
    val questions = remember { listOf(IQQuestion("Sayı Dizisi: 2, 4, 8, 16, ?\n\nSoru işareti yerine ne gelmelidir?", listOf("24", "32", "30", "20"), 1), IQQuestion("Mantık: Hangi sayı diğerlerinden farklıdır?", listOf("13", "17", "21", "19"), 2), IQQuestion("Hesaplama: 2 elma (5 TL) + 1 armut (10 TL) toplam kaç TL?", listOf("15", "25", "20", "30"), 2), IQQuestion("Analoji: Kitap : Sayfa :: Araba : ?", listOf("Tekerlek", "Hız", "Yol", "Sürücü"), 0), IQQuestion("Harf Dizisi: A, C, E, G, ?", listOf("H", "I", "J", "K"), 1)).shuffled() }
    var curIdx by remember { mutableIntStateOf(0) } ; var score by remember { mutableIntStateOf(0) } ; var selectedOpt by remember { mutableStateOf<Int?>(null) } ; var isFinished by remember { mutableStateOf(false) }
    if (isFinished) { GameResult(vm, score, "IQ Testi") { vm.addCoins(score / 5); onExit() } }
    else {
        val q = questions[curIdx] ; val terminalColor = MaterialTheme.colorScheme.primary
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Soru ${curIdx + 1}/${questions.size}", fontWeight = FontWeight.Bold, color = terminalColor); HintButton(vm) { selectedOpt = q.correctIdx; if(!isFinished) { score += 25; android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ if(curIdx < questions.size - 1) { curIdx++; selectedOpt = null } else isFinished = true }, 1000) } } }
            Spacer(Modifier.height(20.dp)); Text(text = q.text, textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Medium, minLines = 3); Spacer(Modifier.height(30.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { q.options.forEachIndexed { i, opt -> val isCorrect = i == q.correctIdx ; val color = when { selectedOpt == null -> MaterialTheme.colorScheme.surfaceVariant; i == selectedOpt && isCorrect -> Color.Green.copy(0.3f); i == selectedOpt && !isCorrect -> Color.Red.copy(0.3f); i == q.correctIdx -> Color.Green.copy(0.3f); else -> MaterialTheme.colorScheme.surfaceVariant }; Card(modifier = Modifier.fillMaxWidth().clickable(enabled = selectedOpt == null) { selectedOpt = i; if (isCorrect) score += 25; android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ if (curIdx < questions.size - 1) { curIdx++; selectedOpt = null } else isFinished = true }, 1500) }, colors = CardDefaults.cardColors(containerColor = color), border = if (selectedOpt != null && isCorrect) BorderStroke(2.dp, Color.Green) else null) { Text(text = opt, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) } } }
        }
    }
}
data class IQQuestion(val text: String, val options: List<String>, val correctIdx: Int)

// --- 14. PATTERN REPEAT GAME ---
@Composable
fun PatternRepeatGame(vm: TaskViewModel, onExit: () -> Unit) {
    val pattern = remember { mutableStateListOf<Int>() } ; val userPattern = remember { mutableStateListOf<Int>() } ; var gameState by remember { mutableStateOf("START") } ; var activeTile by remember { mutableIntStateOf(-1) } ; var score by remember { mutableIntStateOf(0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Text("SKOR: $score", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(24.dp))
        if (gameState == "START") { Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Button(onClick = { pattern.clear(); pattern.add(Random.nextInt(4)); gameState = "SHOWING" }) { Text("OYUNU BAŞLAT") } } }
        else if (gameState == "GAME_OVER") { GameResult(vm, score, "Desen Tekrarı") { vm.addCoins(score / 3); onExit() } }
        else { LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.size(240.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = false) { items(4) { index -> val color = when(index) { 0 -> Color.Red; 1 -> Color.Blue; 2 -> Color.Green; 3 -> Color.Yellow; else -> Color.Gray }; Box(modifier = Modifier.aspectRatio(1f).background(if (activeTile == index) color else color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).border(2.dp, if (activeTile == index) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).clickable(enabled = gameState == "PLAYING") { userPattern.add(index); if (userPattern.last() != pattern[userPattern.size - 1]) gameState = "GAME_OVER" else if (userPattern.size == pattern.size) { score += 10; pattern.add(Random.nextInt(4)); gameState = "SHOWING" } }) } } }
    }
}

// --- 15. RING AND NAIL GAME ---
@Composable
fun RingNailGame(vm: TaskViewModel, onExit: () -> Unit) {
    var nailY by remember { mutableFloatStateOf(0f) } ; var ringX by remember { mutableFloatStateOf(0f) } ; var ringDir by remember { mutableIntStateOf(1) } ; var isFalling by remember { mutableStateOf(false) } ; var attempts by remember { mutableIntStateOf(5) } ; var score by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempts, isFalling) { 
        if (attempts > 0 && !isFalling) { 
            while (!isFalling) { 
                delay(16)
                // Hız artışını dengeledik (0.002f -> 0.0005f) ve maksimum hız sınırı koyduk (0.1f)
                val currentSpeed = (0.015f + (score * 0.0005f)).coerceAtMost(0.1f)
                ringX += currentSpeed * ringDir
                if (ringX > 0.8f || ringX < -0.8f) ringDir *= -1 
            } 
        } 
    }
    LaunchedEffect(isFalling) { if (isFalling) { while (nailY < 0.85f) { delay(10); nailY += 0.05f }; if (kotlin.math.abs(0f - ringX) < 0.15f) score += 40 ; delay(1000); isFalling = false; nailY = 0f; attempts-- } }
    if (attempts == 0 && !isFalling) { GameResult(vm, score, "Halka ve Çivi") { vm.addCoins(score / 10); onExit() } }
    else {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Kalan Deneme: $attempts", fontWeight = FontWeight.Bold); Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f), RoundedCornerShape(12.dp)).clickable(enabled = !isFalling) { isFalling = true }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (nailY * 300).dp).size(6.dp, 30.dp).background(if (isFalling) Color.White else MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.align(Alignment.BottomCenter).offset(x = (ringX * 150).dp, y = (-20).dp).size(55.dp, 12.dp).border(3.dp, Color(0xFFFFD600), RoundedCornerShape(6.dp)))
            }
        }
    }
}

// --- 16. CUBE TOWER GAME ---
@Composable
fun CubeTowerGame(vm: TaskViewModel, onExit: () -> Unit) {
    var cubeX by remember { mutableFloatStateOf(0f) } ; var dir by remember { mutableIntStateOf(1) } ; val towerCubes = remember { mutableStateListOf<Float>() } ; var isGameOver by remember { mutableStateOf(false) } ; var score by remember { mutableIntStateOf(0) }
    LaunchedEffect(towerCubes.size, isGameOver) { if (!isGameOver) { while (true) { delay(16); cubeX += (0.02f + (towerCubes.size * 0.002f)) * dir; if (cubeX > 0.8f || cubeX < -0.8f) dir *= -1 } } }
    if (isGameOver) { GameResult(vm, score, "Küp Kulesi") { vm.addCoins(score / 5); onExit() } }
    else {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Kat Sayısı: ${towerCubes.size}", fontWeight = FontWeight.Bold); Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp).background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(12.dp)).clickable { val lastX = if (towerCubes.isEmpty()) 0f else towerCubes.last(); if (towerCubes.isEmpty() || kotlin.math.abs(cubeX - lastX) < 0.4f) { towerCubes.add(cubeX); score += 20; cubeX = if (Random.nextBoolean()) -0.8f else 0.8f } else isGameOver = true }, contentAlignment = Alignment.BottomCenter) {
                towerCubes.forEachIndexed { i, x -> Box(modifier = Modifier.align(Alignment.BottomCenter).offset(x = (x * 150).dp, y = (-i * 30).dp).size(60.dp, 30.dp).background(MaterialTheme.colorScheme.primary.copy(0.8f - (i * 0.05f).coerceAtMost(0.5f))).border(1.dp, Color.White.copy(0.5f))) }
                Box(modifier = Modifier.align(Alignment.BottomCenter).offset(x = (cubeX * 150).dp, y = (-(towerCubes.size * 30)).dp).size(60.dp, 30.dp).background(MaterialTheme.colorScheme.primary).border(2.dp, Color.White))
            }
        }
    }
}

// --- 17. SHADOW MATCH GAME ---
@Composable
fun ShadowMatchGame(vm: TaskViewModel, onExit: () -> Unit) {
    val items = listOf(Icons.Default.Adb, Icons.Default.AirplanemodeActive, Icons.Default.Anchor, Icons.Default.Brush, Icons.Default.Camera, Icons.Default.DirectionsCar, Icons.Default.Extension, Icons.Default.Favorite, Icons.Default.Home, Icons.Default.Key, Icons.Default.Lightbulb, Icons.Default.MusicNote, Icons.Default.RocketLaunch, Icons.Default.Star, Icons.Default.Timer, Icons.Default.WbSunny)
    var currentItem by remember { mutableStateOf(items.random()) } ; var level by remember { mutableIntStateOf(1) } ; var score by remember { mutableIntStateOf(0) } ; var selectedOpt by remember { mutableStateOf<ImageVector?>(null) }
    val options = remember(currentItem) { val list = mutableListOf(currentItem); while (list.size < 4) { val fake = items.random(); if (!list.contains(fake)) list.add(fake) }; list.shuffled() }
    if (level > 5) { GameResult(vm, score, "Görsel Puzzle") { vm.addCoins(score / 5); onExit() } }
    else {
        val terminalColor = MaterialTheme.colorScheme.primary
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("SEVİYE: $level/5", fontWeight = FontWeight.Bold); Text("NESNENİN GÖLGESİNİ BUL!", style = MaterialTheme.typography.labelSmall, color = terminalColor); Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.size(80.dp), colors = CardDefaults.cardColors(containerColor = terminalColor.copy(0.1f)), border = BorderStroke(1.dp, terminalColor.copy(0.3f))) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(currentItem, null, tint = terminalColor, modifier = Modifier.size(48.dp)) } }
            Spacer(Modifier.height(30.dp)); LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(320.dp), userScrollEnabled = false) { items(options) { opt -> val isCorrect = opt == currentItem ; val color = when { selectedOpt == null -> MaterialTheme.colorScheme.surfaceVariant; opt == selectedOpt && isCorrect -> Color.Green.copy(0.2f); opt == selectedOpt && !isCorrect -> Color.Red.copy(0.2f); opt == currentItem -> Color.Green.copy(0.2f); else -> MaterialTheme.colorScheme.surfaceVariant }; Card(modifier = Modifier.aspectRatio(1f).clickable(enabled = selectedOpt == null) { selectedOpt = opt; if (isCorrect) score += 20; android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ currentItem = items.random(); selectedOpt = null; level++ }, 1000) }, colors = CardDefaults.cardColors(containerColor = color)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(opt, null, tint = Color.Black, modifier = Modifier.size(48.dp)) } } } }
        }
    }
}

// --- 18. WHAT IS MISSING GAME ---
@Composable
fun WhatIsMissingGame(vm: TaskViewModel, onExit: () -> Unit) {
    val allIcons = listOf(Icons.Default.Favorite, Icons.Default.Star, Icons.Default.WbSunny, Icons.Default.Bolt, Icons.Default.Timer, Icons.Default.Settings, Icons.Default.Camera, Icons.Default.Anchor, Icons.Default.Brush, Icons.Default.Home, Icons.Default.Key, Icons.Default.RocketLaunch, Icons.Default.Extension, Icons.Default.DirectionsCar, Icons.Default.AirplanemodeActive)
    var level by remember { mutableIntStateOf(1) } ; var gameState by remember { mutableStateOf("PREVIEW") } ; var currentIcons by remember { mutableStateOf<List<ImageVector>>(emptyList()) } ; var missingIcon by remember { mutableStateOf<ImageVector?>(null) } ; var options by remember { mutableStateOf<List<ImageVector>>(emptyList()) } ; var score by remember { mutableIntStateOf(0) }
    LaunchedEffect(level, gameState) { if (gameState == "PREVIEW") { val selected = allIcons.shuffled().take((level + 3).coerceAtMost(8)); currentIcons = selected; delay(3000); missingIcon = selected.random(); currentIcons = selected.toMutableList().apply { remove(missingIcon) }; options = (allIcons.filter { !selected.contains(it) }.shuffled().take(3) + missingIcon!!).shuffled(); gameState = "MISSING" } }
    if (level > 5) { GameResult(vm, score, "Ne Eksik?") { vm.addCoins(score / 5); onExit() } }
    else {
        val terminalColor = MaterialTheme.colorScheme.primary
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("SEVİYE: $level/5", fontWeight = FontWeight.Bold); Text(text = if (gameState == "PREVIEW") "NESNELERİ AKLINDA TUT!" else "HANGİSİ KAYBOLDU?", style = MaterialTheme.typography.labelSmall, color = terminalColor); Spacer(Modifier.height(40.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false) { items(currentIcons) { icon -> Card(modifier = Modifier.aspectRatio(1f), colors = CardDefaults.cardColors(containerColor = terminalColor.copy(0.1f)), border = BorderStroke(1.dp, terminalColor.copy(0.3f))) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(icon, null, tint = terminalColor, modifier = Modifier.size(32.dp)) } } } }
            Spacer(Modifier.height(40.dp)); if (gameState == "MISSING") { Text("SEÇENEKLER:", style = MaterialTheme.typography.titleSmall); Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { options.forEach { opt -> Card(modifier = Modifier.size(60.dp).clickable { if (opt == missingIcon) { score += 20; level++; gameState = "PREVIEW" } else level = 6 }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(opt, null, tint = Color.White, modifier = Modifier.size(32.dp)) } } } } }
        }
    }
}

// --- 19. NUMBER PUZZLE GAME ---
@Composable
fun NumberPuzzleGame(vm: TaskViewModel, onExit: () -> Unit) {
    val numbers = remember { (1..12).toList().shuffled() }
    var nextTarget by remember { mutableIntStateOf(1) }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var wrongClicks by remember { mutableIntStateOf(0) }
    val terminalColor = MaterialTheme.colorScheme.primary

    if (nextTarget > 12) {
        val totalTime = System.currentTimeMillis() - startTime
        val score = (50000 / (totalTime / 1000 + 1)).toInt().coerceIn(10, 100) - (wrongClicks * 5)
        GameResult(vm, score.coerceAtLeast(10), "Sayı Bulmacası") { vm.addCoins(score / 5); onExit() }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text("HEDEF: $nextTarget", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = terminalColor)
            Text("Sayıları sırayla (1 -> 12) bul!", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(numbers) { num ->
                    val isFound = num < nextTarget
                    Surface(onClick = { if (num == nextTarget) nextTarget++ else wrongClicks++ }, enabled = !isFound, shape = RoundedCornerShape(8.dp), color = if (isFound) Color.DarkGray.copy(alpha = 0.5f) else terminalColor.copy(alpha = 0.2f), border = BorderStroke(1.dp, if (isFound) Color.Transparent else terminalColor)) {
                        Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) { Text(text = if (isFound) "✓" else "$num", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isFound) Color.Gray else Color.White) }
                    }
                }
            }
        }
    }
}

// --- 20. SLIDING PUZZLE GAME ---
@Composable
fun SlidingPuzzleGame(vm: TaskViewModel, onExit: () -> Unit) {
    var difficulty by remember { mutableStateOf<String?>(null) }
    val terminalColor = MaterialTheme.colorScheme.primary

    if (difficulty == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("YAPBOZ ZORLUĞU SEÇ", fontWeight = FontWeight.Bold, color = terminalColor, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            listOf("KOLAY (3x3)" to "EASY", "ORTA (4x4)" to "MEDIUM", "ZOR (5x5)" to "HARD").forEach { (label, diff) ->
                Button(onClick = { difficulty = diff }, modifier = Modifier.fillMaxWidth(0.7f).height(48.dp), shape = RoundedCornerShape(8.dp)) { Text(label) }
                Spacer(Modifier.height(12.dp))
            }
            TextButton(onClick = onExit) { Text("İPTAL", color = Color.Gray) }
        }
    } else {
        val size = when(difficulty) { "EASY" -> 3 ; "MEDIUM" -> 4 ; else -> 5 }
        val tileCount = size * size
        val tiles = remember(difficulty) { mutableStateListOf<Int>().apply { addAll((1 until tileCount).toList() + 0); shuffle() } }
        var moves by remember { mutableIntStateOf(0) }
        val isSolved = tiles.toList() == (1 until tileCount).toList() + 0

        if (isSolved && moves > 0) {
            val baseScore = when(difficulty) { "HARD" -> 200 ; "MEDIUM" -> 150 ; else -> 100 }
            val score = (baseScore - moves).coerceAtLeast(20)
            GameResult(vm, score, "Görsel Yapboz") { vm.addCoins(score / 4); onExit() }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Text("Hamle: $moves", fontWeight = FontWeight.Bold, color = terminalColor)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(size), modifier = Modifier.size(280.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(tiles.size) { index ->
                        val value = tiles[index]
                        if (value != 0) {
                            Surface(onClick = {
                                val emptyIdx = tiles.indexOf(0)
                                val isNeighbor = index == emptyIdx - 1 && emptyIdx % size != 0 || index == emptyIdx + 1 && index % size != 0 || index == emptyIdx - size || index == emptyIdx + size
                                if (isNeighbor) { tiles[emptyIdx] = value; tiles[index] = 0; moves++ }
                            }, shape = RoundedCornerShape(4.dp), color = terminalColor.copy(alpha = 0.2f), border = BorderStroke(1.dp, terminalColor)) { Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) { Text("$value", fontWeight = FontWeight.Bold, fontSize = if(size > 4) 14.sp else 18.sp, color = Color.White) } }
                        } else { Box(modifier = Modifier.aspectRatio(1f).background(Color.Black.copy(alpha = 0.1f))) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = { difficulty = null }) { Text("ZORLUĞU DEĞİŞTİR", color = terminalColor.copy(alpha = 0.7f), fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun GameResult(vm: TaskViewModel, score: Int, type: String, onOk: () -> Unit) {
    // Ses Efekti: Başarı durumunda tik sesi
    LaunchedEffect(Unit) {
        if (score > 0) {
            vm.playTickSound()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Konfeti Efekti: Sadece başarılı skorlarda (Herhangi bir kazançta)
        if (score > 0) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    KonfettiView(context).apply {
                        start(
                            Party(
                                speed = 0f,
                                maxSpeed = 30f,
                                damping = 0.9f,
                                spread = 360,
                                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def).map { it.toLong().toInt() },
                                position = Position.Relative(0.5, 0.3),
                                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                            )
                        )
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (score > 0) { Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD600), modifier = Modifier.size(80.dp).graphicsLayer { translationY = -20f }); Text("TEBRİKLER!", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary) }
            else { Icon(Icons.Default.TimerOff, null, tint = Color.Gray, modifier = Modifier.size(64.dp)); Text("BAŞARISIZ", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.Gray) }
            Spacer(Modifier.height(16.dp)); Text("$type testini tamamladın.", textAlign = TextAlign.Center)
            val feedback = when { score >= 80 -> "Çok başarılısın! 🏆"; score >= 40 -> "Daha iyi olabilirsin. ✨"; else -> "Maalesef, yetersizsin. ✍️" }
            Text(text = feedback, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (score >= 40) MaterialTheme.colorScheme.primary else Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(8.dp)); Text("Skorun: $score", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            if (score > 0) Text("Kazancın: +${score / 5} 🪙", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp)); Button(onClick = { if (score > 0) vm.incrementWinCount(); onOk() }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)) { Text("ÖDÜLÜ AL VE ÇIK", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ReviewDialog(onDismiss: () -> Unit, onRate: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD600), modifier = Modifier.size(48.dp)) }, title = { Text("FocusPath'i Beğeniyor musun?", textAlign = TextAlign.Center) }, text = { Text("Gelişimine katkı sağlamak bizi mutlu ediyor! Bizi Google Play'de değerlendirerek destek olmak ister misin?", textAlign = TextAlign.Center) }, confirmButton = { Button(onClick = onRate, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Bizi Değerlendir", color = Color.Black, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Şimdi Değil", color = Color.Gray) } })
}
