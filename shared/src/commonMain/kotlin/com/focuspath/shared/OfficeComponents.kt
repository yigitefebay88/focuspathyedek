package com.focuspath.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.shared.model.WorkerAction
import com.focuspath.shared.model.WorkerInfo

@Composable
fun WorkerModel(
    worker: WorkerInfo,
    fixedRotationX: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    val typingOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(150, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )

    val headBob by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse)
    )

    val isTyping = worker.currentAction == WorkerAction.TYPING
    val isMouse = worker.currentAction == WorkerAction.MOUSE
    val isThinking = worker.currentAction == WorkerAction.THINKING
    val isAsking = worker.currentAction == WorkerAction.ASKING
    val isSitting = worker.isFocusing && worker.deskId.isNotEmpty() && worker.currentAction != WorkerAction.WALKING

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.offset(y = (-10).dp),
            border = if (worker.isFocusing) BorderStroke(1.dp, TerminalGreen) else null
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) {
                if (worker.isFocusing) {
                    Box(modifier = Modifier.size(4.dp).background(TerminalGreen, CircleShape))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = worker.name.uppercase(),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Box(modifier = Modifier
            .offset(y = if (isSitting) 18.dp else 0.dp)
            .size(44.dp, 54.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .graphicsLayer {
                rotationX = -fixedRotationX + (if (isSitting) 5f else 0f)
                if (worker.currentAction == WorkerAction.WALKING) {
                    rotationY = if (worker.isFacingRight) -50f else 50f
                    rotationX += 10f
                }
                cameraDistance = 12f
                transformOrigin = TransformOrigin(0.5f, 1f)
            }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width ; val h = size.height
                val clothesColor = when(worker.name.length % 3) {
                    0 -> Color(0xFF1B5E20); 1 -> Color(0xFF0D47A1); else -> Color(0xFF37474F)
                }

                drawRoundRect(color = clothesColor, topLeft = Offset(w * 0.22f, h * 0.28f), size = Size(w * 0.56f, h * 0.38f), cornerRadius = CornerRadius(8f, 8f))

                val legHeight = if (isSitting) h * 0.34f else h * 0.42f
                drawRoundRect(color = Color(0xFF212121), topLeft = Offset(w * 0.18f, h * 0.58f), size = Size(w * 0.28f, legHeight), cornerRadius = CornerRadius(6f, 6f))
                drawRoundRect(color = Color(0xFF212121), topLeft = Offset(w * 0.54f, h * 0.58f), size = Size(w * 0.28f, legHeight), cornerRadius = CornerRadius(6f, 6f))

                val leftArmOffset = if(isTyping) typingOffset else 0f
                val rightArmOffset = if(isTyping) -typingOffset else if(isMouse) typingOffset else 0f
                drawRoundRect(color = clothesColor, topLeft = Offset(w * 0.08f, h * 0.32f + leftArmOffset), size = Size(w * 0.18f, h * 0.22f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = clothesColor, topLeft = Offset(w * 0.74f, h * 0.32f + rightArmOffset), size = Size(w * 0.18f, h * 0.22f), cornerRadius = CornerRadius(4f, 4f))

                val headY = h * 0.18f + (if(isThinking || isAsking) headBob else 0f)
                val hairColor = if (worker.name.length % 2 == 0) Color(0xFF3E2723) else Color(0xFF212121)
                drawCircle(color = hairColor, radius = w * 0.21f, center = Offset(w * 0.50f, headY))
                
                val statusColor = when(worker.currentAction) {
                    WorkerAction.WORKING, WorkerAction.TYPING, WorkerAction.MOUSE -> TerminalGreen
                    WorkerAction.COFFEE, WorkerAction.RESTING -> AccentYellow
                    else -> Color.Gray
                }
                drawCircle(color = statusColor, radius = 4f, center = Offset(w * 0.85f, headY - h * 0.1f))
            }
        }
    }
}

@Composable
fun DeskSetup(
    worker: WorkerInfo,
    isPremium: Boolean = false,
    onWorkerClick: () -> Unit
) {
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 45.dp)) {
            Box(modifier = Modifier.size(36.dp, 26.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)))
        }
        
        Box(modifier = Modifier.width(if (isPremium) 76.dp else 70.dp).height(34.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(if (isPremium) Color.Black else Color(0xFF3E2723), RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp)))
        }
        
        Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-30).dp).size(44.dp, 32.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212), RoundedCornerShape(4.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))) {
                if(worker.isFocusing) {
                    Box(modifier = Modifier.fillMaxSize().background(TerminalGreen.copy(alpha = 0.2f)))
                }
            }
        }
        
        Box(modifier = Modifier.align(Alignment.Center)) {
            WorkerModel(worker = worker, fixedRotationX = 55f, onClick = onWorkerClick)
        }
    }
}
