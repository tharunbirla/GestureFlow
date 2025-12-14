package com.tharunbirla.gestureflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewCarousel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestModeScreen(
    onFinish: () -> Unit,
    sharedPrefs: android.content.SharedPreferences
) {
    val context = LocalContext.current
    var detectedGesture by remember { mutableStateOf<String?>(null) }
    var sensitivity by remember { mutableStateOf(sharedPrefs.getFloat("sensitivity", 0.5f)) }

    // Receiver to listen for gestures from Service
    DisposableEffect(context) {
        // Enable test mode in prefs when screen is active
        sharedPrefs.edit().putBoolean("test_mode_active", true).apply()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.tharunbirla.gestureflow.GESTURE_DETECTED") {
                    val gestureName = intent.getStringExtra("gesture")
                    detectedGesture = gestureName
                }
            }
        }
        val filter = IntentFilter("com.tharunbirla.gestureflow.GESTURE_DETECTED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            // Disable test mode when screen is left
            sharedPrefs.edit().putBoolean("test_mode_active", false).apply()
            context.unregisterReceiver(receiver)
        }
    }

    // Auto-clear detected gesture after a short delay
    LaunchedEffect(detectedGesture) {
        if (detectedGesture != null) {
            delay(1500)
            detectedGesture = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Playground") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Try your gestures here!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Gestures won't trigger actual actions in this mode.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Sensitivity Slider
            SensitivityControl(sensitivity) {
                sensitivity = it
                sharedPrefs.edit().putFloat("sensitivity", it).apply()
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Gesture Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GestureIndicator("Back", Icons.Default.ArrowBack, detectedGesture == "BACK")
                GestureIndicator("Multitasking", Icons.Default.ViewCarousel, detectedGesture == "MULTITASKING")
                GestureIndicator("Notif.", Icons.Default.Notifications, detectedGesture == "NOTIFICATION_PULL_DOWN")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Finish Testing", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SensitivityControl(sensitivity: Float, onSensitivityChange: (Float) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sensitivity Adjustment",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stable", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Slider(
                    value = sensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Text("Sensitive", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                "%.1f".format(sensitivity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun GestureIndicator(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean) {
    val scale by animateFloatAsState(if (isActive) 1.2f else 1.0f, label = "scale")
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .scale(scale)
                .clip(RoundedCornerShape(20.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
