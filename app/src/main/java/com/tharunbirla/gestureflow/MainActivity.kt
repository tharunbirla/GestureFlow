package com.tharunbirla.gestureflow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tharunbirla.gestureflow.ui.theme.GestureFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestureFlowTheme {
                MainScreenContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-render UI on resume to update service status
        setContent {
            GestureFlowTheme {
                MainScreenContent()
            }
        }
    }
}

/**
 * Checks if the GestureFlowAccessibilityService is enabled.
 */
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    // Construct the expected service ID string
    val accessibilityServiceId = context.packageName + "/" + GestureFlowAccessibilityService::class.java.name
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(accessibilityServiceId) == true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent() {
    val context = LocalContext.current
    // State to hold the current accessibility service status
    var isServiceEnabled by remember { mutableStateOf(false) }

    // LaunchedEffect to update the service status when the Composable is first displayed
    // or when relevant keys change (in this case, we'll re-check on resume)
    LaunchedEffect(key1 = Unit) { // Use Unit as key to run once or when the composable is initially composed
        isServiceEnabled = isAccessibilityServiceEnabled(context)
    }

    // For `onResume` to trigger UI update reliably, we need to ensure this state is observed.
    // The `onResume` in MainActivity already calls `setContent` again, which rebuilds the Composables.
    // This makes `isServiceEnabled` re-evaluated.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GestureFlow") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Make screen scrollable
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Align content to the top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Service Status Card
            ServiceStatusCard(isServiceEnabled = isServiceEnabled) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Card
            InfoCard(
                title = "About GestureFlow",
                description = "GestureFlow allows you to control your Android device with intuitive micro-motion gestures, without needing to touch the screen. It uses your device's sensors to detect flicks and movements, converting them into system actions like Back, Multitasking, and Notification Pull-down.",
                icon = Icons.Default.Info
            )

            Spacer(modifier = Modifier.height(16.dp))

            // How to Use Card
            InfoCard(
                title = "How to Use",
                description = """
                    1. Enable Service: Tap the 'Enable/Disable Service' button above and turn on 'GestureFlow Service' in Accessibility settings.
                    2. Back: Perform a quick flick of your device *towards you* along the Y-axis (like pulling a trigger).
                    3. Multitasking: Perform a quick flick of your device *to the right* along the X-axis (like swiping horizontally with your hand).
                    4. Notification Pull-down: Perform a quick flick of your device *downwards* along the Z-axis (like dropping your hand straight down).

                    (Note: Gesture sensitivity can be fine-tuned in future versions for a more personalized experience.)
                """.trimIndent(),
                icon = Icons.Default.MotionPhotosOn
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Disclaimer/Privacy Note (optional but good practice)
            Text(
                text = "GestureFlow requires Accessibility Service permission to function. Your sensor data is processed on-device and not collected or stored.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ServiceStatusCard(isServiceEnabled: Boolean, onButtonClick: () -> Unit) {
    val statusText = if (isServiceEnabled) "ENABLED" else "DISABLED"
    val statusColor = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val buttonText = if (isServiceEnabled) "Disable Service" else "Enable Service"
    val statusIcon = if (isServiceEnabled) Icons.Default.Accessibility else Icons.Default.Settings // Using Settings icon for disabled to imply "go to settings"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Service Status Icon",
                tint = statusColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "GestureFlow Service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineLarge,
                color = statusColor,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun InfoCard(title: String, description: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Content description for icon is often null if text provides context
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreenContent() {
    GestureFlowTheme {
        MainScreenContent()
    }
}