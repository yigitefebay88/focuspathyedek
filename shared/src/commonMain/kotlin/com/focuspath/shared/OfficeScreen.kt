package com.focuspath.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.shared.model.WorkerAction
import com.focuspath.shared.model.WorkerInfo

@Composable
fun SharedOfficeScreen(
    workers: List<WorkerInfo>,
    officeLevel: Int = 1,
    isFocusActive: Boolean = false
) {
    var scale by remember { mutableStateOf(0.85f) }
    val ambientAlpha by animateFloatAsState(
        targetValue = if (isFocusActive) 0.5f else 1f,
        animationSpec = tween(1500)
    )

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Office Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("VIRTUAL OFFICE", style = MaterialTheme.typography.titleLarge, color = TerminalGreen)
                Text("Level $officeLevel • Operations Center", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.dp, TerminalGreen.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // 3D Perspective Scene
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .graphicsLayer {
                        rotationX = 55f
                        scaleX = scale
                        scaleY = scale
                        alpha = ambientAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ground
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color(0xFF222222), Color(0xFF050505))))
                        .border(1.dp, Color.White.copy(alpha = 0.1f))
                )

                // Render Workers and Desks
                workers.forEach { worker ->
                    WorkerDesk(worker)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Status Bar
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceColor
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(if(isFocusActive) TerminalGreen else Color.Gray, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    if(isFocusActive) "Deep Focus Session Active" else "Office on Standby",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if(isFocusActive) TerminalGreen else Color.White
                )
            }
        }
    }
}

@Composable
fun WorkerDesk(worker: WorkerInfo) {
    val deskOffset = IntOffset(worker.x.toInt(), worker.y.toInt())
    
    Box(
        modifier = Modifier
            .offset { deskOffset }
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Simple Desk Representation
        Box(
            modifier = Modifier
                .size(60.dp, 30.dp)
                .background(Color(0xFF3E2723), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(4.dp))
        )
        
        // Worker
        Box(
            modifier = Modifier
                .offset(y = (-15).dp)
                .size(32.dp)
                .graphicsLayer { rotationX = -55f } // Counter-rotate to face camera
                .background(if(worker.isMe) TerminalGreen else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (worker.photoUrl == null) {
                Text(worker.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        
        // Monitor Glow
        if (worker.isFocusing) {
            Box(
                modifier = Modifier
                    .offset(y = (-25).dp)
                    .size(24.dp, 16.dp)
                    .background(TerminalGreen.copy(0.3f), RoundedCornerShape(2.dp))
            )
        }
    }
}
