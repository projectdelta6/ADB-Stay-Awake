package com.duck.stayawakeadb.activity

import android.Manifest
import android.content.BroadcastReceiver
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.BuildConfig
import com.duck.stayawakeadb.R
import com.duck.stayawakeadb.constant.Constants.notificationData
import com.duck.stayawakeadb.service.ADBNotificationListener
import com.duck.stayawakeadb.ui.composables.SettingSection
import com.duck.stayawakeadb.ui.theme.ADBStayAwakeTheme
import com.duck.stayawakeadb.util.NotificationUtil
import com.duck.stayawakeadb.util.SettingsHelperUtil


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

    // Permission check UI
    if (!writeSecureSettingsGranted) {
        PermissionRequiredDialog(
            onGrantPermission = {
                // Dialog is now handled internally by PermissionRequiredDialog
            }
        )
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

                // Developer Options Section
                SettingSection(
                    title = stringResource(R.string.developer_options),
                    description = if (developerOptionsEnabled)
                        stringResource(R.string.dev_settings_on)
                    else
                        stringResource(R.string.dev_settings_off),
                    checked = developerOptionsEnabled,
                    onCheckedChange = null, // Read-only
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // USB Debugging Section
                if (developerOptionsEnabled) {
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Wireless Debugging Section
                if (developerOptionsEnabled) {
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
                if (developerOptionsEnabled) {
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
                if (developerOptionsEnabled && (usbDebuggingEnabled || wirelessDebuggingEnabled)) {
                    SettingSection(
                        title = stringResource(R.string.auto_toggle_stay_awake),
                        description = stringResource(R.string.auto_toggle_stay_awake_description),
                        checked = autoToggleStayAwake,
                        onCheckedChange = { checked ->
                            settingsHelperUtil.autoToggleStayAwake = checked
                            autoToggleStayAwake = checked
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Stay Awake Section
                if (developerOptionsEnabled && usbDebuggingEnabled) {
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Notification Section
                if (developerOptionsEnabled && usbDebuggingEnabled) {
                    SettingSection(
                        title = stringResource(R.string.show_notification),
                        description = stringResource(R.string.toggle_if_the_notification_should_be_shown),
                        checked = showNotification,
                        onCheckedChange = { checked ->
                            settingsHelperUtil.showNotification = checked
                            showNotification = checked
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    )
}


@Composable
fun PermissionRequiredDialog(onGrantPermission: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                stringResource(R.string.permission_required_title),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                stringResource(R.string.permission_required_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onGrantPermission) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
