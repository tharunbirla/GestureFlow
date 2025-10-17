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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    var isServiceEnabled by remember { mutableStateOf(false) }
    val sharedPrefs = context.getSharedPreferences("gestureflow_prefs", Context.MODE_PRIVATE)
    var sensitivity by remember { mutableStateOf(sharedPrefs.getFloat("sensitivity", 0.5f)) }
    var showOnboarding by remember { mutableStateOf(!sharedPrefs.getBoolean("onboard_shown", false)) }
    var currentScreen by remember { mutableStateOf("home") }
    val coroutineScope = rememberCoroutineScope()

    // Save sensitivity
    fun saveSensitivity(value: Float) {
        sharedPrefs.edit().putFloat("sensitivity", value).apply()
    }

    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
    }

    // Onboarding
    if (showOnboarding) {
        OnboardingScreen(onOpenSettings = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }, onSkip = {
            sharedPrefs.edit().putBoolean("onboard_shown", true).apply()
            showOnboarding = false
        })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GestureFlow") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { currentScreen = "gestures" }) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage gestures")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Render gestures screen outside the vertically scrollable home content to avoid nested scrolling
        if (currentScreen == "gestures") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                GesturesManagementScreen(sharedPrefs) { currentScreen = "home" }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Service Status Card
                ServiceStatusCard(isServiceEnabled = isServiceEnabled) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sensitivity Slider Card
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
                        Text(
                            text = "Gesture Sensitivity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Low", style = MaterialTheme.typography.bodySmall)
                            Text(text = "High", style = MaterialTheme.typography.bodySmall)
                        }

                        androidx.compose.material3.Slider(
                            value = sensitivity,
                            onValueChange = {
                                sensitivity = it
                                saveSensitivity(it)
                            },
                            valueRange = 0.1f..1.0f,
                            steps = 8,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Text(
                            text = "Current: %.2f (Low → High; higher = more sensitive)".format(sensitivity),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About Card
                InfoCard(
                    title = "About GestureFlow",
                    description = "GestureFlow allows you to control your Android device with intuitive micro-motion gestures, without needing to touch the screen. It uses your device's sensors to detect flicks and movements, converting them into system actions like Back, Multitasking, and Notification Pull-down.",
                    icon = Icons.Default.Info
                )

                Spacer(modifier = Modifier.height(16.dp))

                // How to Use Card - expanded friendly instructions
                InfoCard(
                    title = "How to Use",
                    description = """
                    Quick steps to get started:

                    1) Enable the Accessibility Service: Tap the 'Enable Service' card above. In Accessibility settings, enable 'GestureFlow Service'.

                    2) Sensitivity: Use the slider to choose how easily gestures are detected. Move the slider right for MORE sensitivity (easier to trigger), left for LESS sensitivity (harder to trigger accidental gestures).

                    3) Gestures (what they do):
                       • Back — Hold your phone and make a short, sharp flick TOWARDS you along the Y-axis (like pulling the device toward your body).
                       • Multitasking — Make a quick horizontal flick to the RIGHT along the X-axis (like a fast swipe motion).
                       • Notification Pull-down — Move the phone quickly DOWN along the Z-axis (like dropping your hand straight down).

                    Tips:
                    - Practice each gesture once or twice while watching the logs (adb logcat) to tune sensitivity.
                    - You can manage which gestures are active from the Gestures screen (tap the settings icon in the top bar).

                    Gesture processing happens entirely on your device — no data is sent to any server.
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
}

@Composable
fun ServiceStatusCard(isServiceEnabled: Boolean, onButtonClick: () -> Unit) {
    val statusText = if (isServiceEnabled) "ENABLED" else "DISABLED"
    val statusColor = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val buttonText = if (isServiceEnabled) "Disable Service" else "Enable Service"
    val statusIcon = if (isServiceEnabled) Icons.Default.Accessibility else Icons.Default.Settings

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
                contentDescription = null,
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

@Composable
fun OnboardingScreen(onOpenSettings: () -> Unit, onSkip: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to GestureFlow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Use small, intentional micro-motions to control Back, Multitasking and Notifications. We'll ask you to enable the Accessibility service next.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onOpenSettings) {
                Text("Open Accessibility Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSkip) {
                Text("Skip and Continue")
            }
        }
    }
}

@Composable
fun GesturesManagementScreen(sharedPrefs: android.content.SharedPreferences, onBack: () -> Unit) {
    data class GestureItem(val id: String, val title: String, val prefKey: String)

    val gestures = listOf(
        GestureItem("back", "Back", "gesture_back_enabled"),
        GestureItem("multitasking", "Multitasking", "gesture_multitasking_enabled"),
        GestureItem("notifications", "Notifications Pull-down", "gesture_notifications_enabled")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Gestures",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) { Text("Done") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(gestures) { g ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.elevatedCardElevation()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(g.title, style = MaterialTheme.typography.titleMedium)
                            Text("Enable or disable this gesture", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val enabled = sharedPrefs.getBoolean(g.prefKey, true)
                        var checked by remember { mutableStateOf(enabled) }
                        Switch(checked = checked, onCheckedChange = {
                            checked = it
                            sharedPrefs.edit().putBoolean(g.prefKey, it).apply()
                        }, colors = SwitchDefaults.colors())
                    }
                }
            }
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