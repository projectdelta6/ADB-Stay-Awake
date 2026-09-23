package com.duck.stayawakeadb.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.BuildConfig
import com.duck.stayawakeadb.R
import com.duck.stayawakeadb.constant.Constants.notificationData
import com.duck.stayawakeadb.service.ADBNotificationListener
import com.duck.stayawakeadb.ui.composables.SettingSection
import com.duck.stayawakeadb.ui.theme.ADBStayAwakeTheme
import com.duck.stayawakeadb.util.NotificationUtil
import com.duck.stayawakeadb.util.SettingsHelperUtil
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    /*
     * The WRITE_SECURE_SETTINGS is a System permission that is not granted to any non-System app. so to get around this
     * after installing the app, you have to connect the device to an ADB console and run this command to grant the
     * permission: 'adb shell pm grant com.duck.stayawakeadb android.permission.WRITE_SECURE_SETTINGS'
     */

    private lateinit var settingsHelperUtil: SettingsHelperUtil
    private var receiverCache: BroadcastReceiver? = null

    private val receiver: BroadcastReceiver
        get() {
            if (receiverCache == null) {
                receiverCache = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        // Broadcasts are now handled in the Compose MainScreen
                        Log.d("MainActivity", "Broadcast received in Activity: ${intent.action}")
                    }
                }
            }
            return receiverCache!!
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsHelperUtil = SettingsHelperUtil(applicationContext)
        //ensure the notification channel is created
        NotificationUtil.createNotificationChannel(
            this,
            notificationData
        )

        setContent {
            ADBStayAwakeTheme {
                MainScreen(settingsHelperUtil)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkAndAskShowNotificationPermission() && checkAndAskNotificationPermission()) {
            registerReceiver()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver()
    }

    private fun checkAndAskShowNotificationPermission(): Boolean {
        //Todo extract strings
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Notification permission")
                    .setMessage("In Order to show the sticky notification with controlls for turning Stay Awake setting on/off the app requires permission to show notifications.")
                    .setPositiveButton("ok") { dialog, _ ->
                        dialog.dismiss()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .show()
                false
            }
        } else {
            true
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted
            } else {
                // Permission denied
            }
        }

    private fun checkAndAskNotificationPermission(): Boolean {
        if (!settingsHelperUtil.notificationPermissionGranted) {
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setMessage(
                getString(
                    R.string.request_notification_permission, getString(
                        R.string.app_name
                    )
                )
            )
            dialogBuilder.setPositiveButton(R.string.go_to_settings) { _, _ ->
                startActivity(Intent(SettingsHelperUtil.SETTINGS_NOTIFICATION_LISTENER))
            }
            dialogBuilder.setNegativeButton(R.string.cancel) { _, _ ->
                // Do nothing
            }
            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.setTitle(R.string.request_notification_permission_title)
            alertDialog.show()
            return false
        }
        return true
    }

    private fun registerReceiver() {
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(receiver,
                ADBNotificationListener.intentFilter
            )
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
    }
}

@Composable
fun MainScreen(settingsHelperUtil: SettingsHelperUtil) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Android 17+ reports debug settings as off to apps; see SettingsHelperUtil.debugStateHidden
    val debugStateHidden = settingsHelperUtil.debugStateHidden
    var developerOptionsEnabled by remember { mutableStateOf(settingsHelperUtil.developerOptionsEnabled) }
    var usbDebuggingEnabled by remember { mutableStateOf(settingsHelperUtil.usbDebuggingEnabled) }
    var stayAwakeEnabled by remember { mutableStateOf(settingsHelperUtil.stayAwakeEnabled) }
    var wirelessDebuggingEnabled by remember { mutableStateOf(settingsHelperUtil.wirelessDebuggingEnabled) }
    var showNotification by remember { mutableStateOf(settingsHelperUtil.showNotification) }
    var autoToggleStayAwake by remember { mutableStateOf(settingsHelperUtil.autoToggleStayAwake) }
    var writeSecureSettingsGranted by remember { mutableStateOf(settingsHelperUtil.writeSecureSettingsPermissionGranted) }

    // Listen for broadcasts to update UI state
    DisposableEffect(Unit) {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("MainScreen", "Received broadcast: ${intent.action}")
                // Update all state variables to reflect current system state
                developerOptionsEnabled = settingsHelperUtil.developerOptionsEnabled
                usbDebuggingEnabled = settingsHelperUtil.usbDebuggingEnabled
                stayAwakeEnabled = settingsHelperUtil.stayAwakeEnabled
                wirelessDebuggingEnabled = settingsHelperUtil.wirelessDebuggingEnabled
                showNotification = settingsHelperUtil.showNotification
                autoToggleStayAwake = settingsHelperUtil.autoToggleStayAwake
                writeSecureSettingsGranted = settingsHelperUtil.writeSecureSettingsPermissionGranted
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(
            broadcastReceiver,
            ADBNotificationListener.intentFilter
        )

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
        }
    }

    @Composable
    fun getStayAwakeDescription(stayAwakeValue: Int): String {
        return when (stayAwakeValue) {
            0 -> stringResource(R.string.stay_awake_off)
            1 -> stringResource(R.string.stay_awake_ac_only)
            2 -> stringResource(R.string.stay_awake_usb_only)
            4 -> stringResource(R.string.stay_awake_wireless_only)
            3 -> stringResource(R.string.stay_awake_ac_usb) // AC + USB = 1 + 2 = 3
            5 -> stringResource(R.string.stay_awake_ac_wireless) // AC + Wireless = 1 + 4 = 5
            6 -> stringResource(R.string.stay_awake_usb_wireless) // USB + Wireless = 2 + 4 = 6
            7 -> stringResource(R.string.stay_awake_all) // AC + USB + Wireless = 1 + 2 + 4 = 7
            else -> stringResource(R.string.stay_awake_off)
        }
    }

    // Re-check WRITE_SECURE_SETTINGS on every resume. It's usually granted over ADB while the app
    // is open on screen, so also poll while it's missing and we're resumed.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            writeSecureSettingsGranted = settingsHelperUtil.writeSecureSettingsPermissionGranted
            while (!writeSecureSettingsGranted) {
                delay(PERMISSION_POLL_INTERVAL_MS)
                writeSecureSettingsGranted = settingsHelperUtil.writeSecureSettingsPermissionGranted
            }
            // Refresh what the permission gates, now that it's granted
            stayAwakeEnabled = settingsHelperUtil.stayAwakeEnabled
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Version info
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (!writeSecureSettingsGranted) {
                    PermissionRequiredCard(
                        modifier = Modifier.padding(bottom = 16.dp),
                        command = settingsHelperUtil.grantWriteSecureSettingsCommand,
                    )
                }

                // Developer Options Section
                SettingSection(
                    title = stringResource(R.string.developer_options),
                    description = AnnotatedString.fromHtml(
                        when {
                            debugStateHidden -> stringResource(R.string.dev_settings_hidden)
                            developerOptionsEnabled -> stringResource(R.string.dev_settings_on)
                            else -> stringResource(R.string.dev_settings_off)
                        },
                    ),
                    checked = developerOptionsEnabled,
                    onCheckedChange = null, // Read-only
                    modifier = Modifier.padding(bottom = 16.dp),
                    showStatus = !debugStateHidden,
                )

                // USB Debugging Section
                if (!debugStateHidden && developerOptionsEnabled) {
                    SettingSection(
                        title = stringResource(R.string.usb_debugging),
                        description = stringResource(R.string.usb_debugging_description),
                        checked = usbDebuggingEnabled,
                        onCheckedChange = { checked ->
                            if (settingsHelperUtil.setUSBDebugging(checked)) {
                                usbDebuggingEnabled = checked
                                if (checked) {
                                    stayAwakeEnabled = settingsHelperUtil.stayAwakeEnabled
                                }
                            } else {
                                usbDebuggingEnabled = settingsHelperUtil.usbDebuggingEnabled
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        enabled = writeSecureSettingsGranted,
                    )
                }

                // Wireless Debugging Section
                if (!debugStateHidden && developerOptionsEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.wireless_debugging),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.wireless_debugging_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = wirelessDebuggingEnabled,
                                    enabled = writeSecureSettingsGranted,
                                    onCheckedChange = { checked ->
                                        if (settingsHelperUtil.setWirelessDebugging(checked)) {
                                            wirelessDebuggingEnabled = checked
                                        } else {
                                            wirelessDebuggingEnabled =
                                                settingsHelperUtil.wirelessDebuggingEnabled
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Open Developer Options Button
                if (debugStateHidden || developerOptionsEnabled) {
                    val errorMessage = stringResource(R.string.unable_to_open_developer_options)
                    Button(
                        onClick = {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(stringResource(R.string.open_developer_options))
                    }
                }

                // Auto Toggle Section
                if (debugStateHidden || (developerOptionsEnabled && (usbDebuggingEnabled || wirelessDebuggingEnabled))) {
                    SettingSection(
                        title = stringResource(R.string.auto_toggle_stay_awake),
                        description = stringResource(R.string.auto_toggle_stay_awake_description),
                        checked = autoToggleStayAwake,
                        onCheckedChange = { checked ->
                            settingsHelperUtil.autoToggleStayAwake = checked
                            autoToggleStayAwake = checked
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        enabled = writeSecureSettingsGranted,
                    )
                }

                // Stay Awake Section
                if (debugStateHidden || (developerOptionsEnabled && usbDebuggingEnabled)) {
                    SettingSection(
                        title = stringResource(R.string.stay_awake),
                        description = getStayAwakeDescription(settingsHelperUtil.stayAwakeValue),
                        checked = stayAwakeEnabled,
                        onCheckedChange = { checked ->
                            if (settingsHelperUtil.setStayAwake(checked)) {
                                stayAwakeEnabled = checked
                                NotificationUtil.updateStayAwakeNotification(context)
                            } else {
                                stayAwakeEnabled = settingsHelperUtil.stayAwakeEnabled
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        enabled = writeSecureSettingsGranted,
                    )
                }

                // Notification Section
                if (debugStateHidden || (developerOptionsEnabled && usbDebuggingEnabled)) {
                    SettingSection(
                        title = stringResource(R.string.show_notification),
                        description = stringResource(R.string.toggle_if_the_notification_should_be_shown),
                        checked = showNotification,
                        onCheckedChange = { checked ->
                            settingsHelperUtil.showNotification = checked
                            showNotification = checked
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        enabled = writeSecureSettingsGranted,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    )
}


private const val PERMISSION_POLL_INTERVAL_MS = 1_000L

/**
 * Shown while WRITE_SECURE_SETTINGS is missing, with the ADB [command] that grants it.
 */
@Composable
private fun PermissionRequiredCard(
    modifier: Modifier = Modifier,
    command: String,
) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.command_copied)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.permission_required_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.permission_required_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            SelectionContainer {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText(command, command))
                    // Android 13+ shows its own clipboard confirmation
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text(stringResource(R.string.copy_command))
            }
        }
    }
}
