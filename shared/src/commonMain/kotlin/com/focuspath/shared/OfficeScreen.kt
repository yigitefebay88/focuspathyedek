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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuspath.shared.model.WorkerInfo

@Composable
fun SharedOfficeScreen(
    workers: List<WorkerInfo>,
    officeLevel: Int = 1,
    coins: Int = 120,
    isFocusActive: Boolean = false
) {
    var scale by remember { mutableStateOf(0.85f) }
    val upgradeCost = officeLevel * 500
    
    val ambientAlpha by animateFloatAsState(
        targetValue = if (isFocusActive) 0.5f else 1f,
        animationSpec = tween(1500)
    )

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("YİME CENTER", style = MaterialTheme.typography.titleLarge, color = TerminalGreen)
                Text("LVL $officeLevel • $coins 🪙", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF020202))
                .border(1.dp, TerminalGreen.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(350.dp).graphicsLayer { 
                rotationX = 55f
                scaleX = scale
                scaleY = scale
                alpha = ambientAlpha
            }) {
                Box(modifier = Modifier.align(Alignment.Center).size(300.dp).background(Brush.radialGradient(listOf(Color(0xFF222222), Color(0xFF050505)))))
                
                workers.forEach { worker ->
                    Box(modifier = Modifier.align(Alignment.Center).offset(x = worker.x.dp, y = worker.y.dp)) {
                        DeskSetup(worker = worker, onWorkerClick = {})
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "OFİS YÜKSELTMELERİ", style = MaterialTheme.typography.titleMedium, color = TerminalGreen)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$upgradeCost 🪙", fontWeight = FontWeight.Bold, color = AccentYellow)
                    Button(
                        onClick = { 
                         },
                        colors = ButtonDefaults.buttonColors(containerColor = if (coins >= upgradeCost) TerminalGreen else Color.Gray),
                        shape = RoundedCornerShape(8.dp)
                    ) { 
                        Text("YÜKSELT", color = Color.Black, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("AKTİF OTURUMA KATIL", color = Color.Black, fontWeight = FontWeight.Black)
        }
        
        Spacer(Modifier.height(16.dp))
    }
}
