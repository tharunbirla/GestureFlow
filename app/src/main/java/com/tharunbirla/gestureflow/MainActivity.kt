package com.tharunbirla.gestureflow

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tharunbirla.gestureflow.ui.theme.GestureFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safety Reset: Ensure test mode is disabled on app launch.
        // This prevents the service from being stuck in "suppress action" mode if the app previously crashed.
        getSharedPreferences("gestureflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("test_mode_active", false)
            .apply()

        enableEdgeToEdge()
        setContent {
            GestureFlowTheme {
                MainAppLogic()
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

@Composable
fun MainAppLogic() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("gestureflow_prefs", Context.MODE_PRIVATE) }
    var isServiceEnabled by remember { mutableStateOf(false) }

    // Lifecycle observer to update service status on Resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showOnboarding by remember { mutableStateOf(!sharedPrefs.getBoolean("onboard_shown", false)) }
    var currentScreen by remember { mutableStateOf("home") } // home, gestures, test_mode

    if (showOnboarding) {
        OnboardingFlow(
            isServiceEnabled = isServiceEnabled,
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            onFinishOnboarding = {
                sharedPrefs.edit().putBoolean("onboard_shown", true).apply()
                showOnboarding = false
                // Optional: Go to test mode immediately after onboarding?
                // Let's stick to Home first as requested, or Test Mode if you prefer flow.
                // User asked: "onboard to take user to test mode"
                currentScreen = "test_mode"
            },
            onSkip = {
                sharedPrefs.edit().putBoolean("onboard_shown", true).apply()
                showOnboarding = false
            }
        )
    } else {
        when (currentScreen) {
            "test_mode" -> {
                TestModeScreen(
                    onFinish = { currentScreen = "home" },
                    sharedPrefs = sharedPrefs
                )
            }
            "gestures" -> {
                GesturesManagementScreen(sharedPrefs) { currentScreen = "home" }
            }
            else -> {
                MainScreenContent(
                    isServiceEnabled = isServiceEnabled,
                    sharedPrefs = sharedPrefs,
                    onNavigateToGestures = { currentScreen = "gestures" },
                    onNavigateToTestMode = { currentScreen = "test_mode" },
                    onToggleService = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    isServiceEnabled: Boolean,
    sharedPrefs: SharedPreferences,
    onNavigateToGestures: () -> Unit,
    onNavigateToTestMode: () -> Unit,
    onToggleService: () -> Unit
) {
    var sensitivity by remember { mutableStateOf(sharedPrefs.getFloat("sensitivity", 0.5f)) }

    fun saveSensitivity(value: Float) {
        sharedPrefs.edit().putFloat("sensitivity", value).apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GestureFlow", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onNavigateToGestures) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage gestures")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Service Status Card
            ServiceStatusCard(isServiceEnabled = isServiceEnabled, onButtonClick = onToggleService)

            Spacer(modifier = Modifier.height(24.dp))

            // Sensitivity Card
            SensitivityControl(sensitivity) {
                sensitivity = it
                saveSensitivity(it)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Playground Card
            Card(
                onClick = onNavigateToTestMode,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MotionPhotosOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Test Playground", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Practice gestures safely", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Guides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // How to Use
            InfoCard(
                title = "How to Use",
                description = "• Back: Sharp flick TOWARDS you.\n• Multitasking: Quick flick RIGHT.\n• Notifications: Quick drop DOWN.",
                icon = Icons.Default.Info
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ServiceStatusCard(isServiceEnabled: Boolean, onButtonClick: () -> Unit) {
    val containerColor = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (isServiceEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
    val statusText = if (isServiceEnabled) "Active" else "Inactive"
    val icon = if (isServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Info

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gesture Service is $statusText",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isServiceEnabled) "Ready to detect your moves." else "Enable in Accessibility settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isServiceEnabled) "Manage Service" else "Enable Service", color = contentColor)
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
fun OnboardingFlow(
    isServiceEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onFinishOnboarding: () -> Unit,
    onSkip: () -> Unit
) {
    // If service becomes enabled while on this screen, we can auto-advance or show a "Continue" button
    // Let's show a Success state if enabled.

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MotionPhotosOn,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Welcome to GestureFlow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Control your device with simple motion gestures. To start, we need Accessibility permissions.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            if (isServiceEnabled) {
                // Success State
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Service Enabled!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Let's Test Gestures ->")
                }
            } else {
                // Action State
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Enable Service")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onSkip) {
                    Text("Skip for now")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesManagementScreen(sharedPrefs: SharedPreferences, onBack: () -> Unit) {
    var vibrationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("vibration_enabled", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("General", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // Vibration Toggle
            ListItem(
                headlineContent = { Text("Vibration Feedback") },
                supportingContent = { Text("Vibrate when a gesture is detected") },
                leadingContent = { Icon(Icons.Default.Vibration, null) },
                trailingContent = {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            sharedPrefs.edit().putBoolean("vibration_enabled", it).apply()
                        }
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Gestures", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            val gestures = listOf(
                Triple("Back", "gesture_back_enabled", "Flick towards you"),
                Triple("Multitasking", "gesture_multitasking_enabled", "Flick right"),
                Triple("Notifications", "gesture_notifications_enabled", "Drop down")
            )

            gestures.forEach { (name, key, desc) ->
                var enabled by remember { mutableStateOf(sharedPrefs.getBoolean(key, true)) }
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(desc) },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                sharedPrefs.edit().putBoolean(key, it).apply()
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}
