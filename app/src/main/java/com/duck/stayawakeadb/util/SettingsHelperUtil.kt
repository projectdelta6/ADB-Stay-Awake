package com.duck.stayawakeadb.util

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.duck.stayawakeadb.R
import com.duck.stayawakeadb.service.ADBNotificationListener
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Created by Bradley Duck on 2019/02/18.
 */
class SettingsHelperUtil(private val applicationContext: Context) {

    private val sharedPreferences: SharedPreferences
        get() {
            return applicationContext.getSharedPreferences(applicationContext.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)
        }

    val notificationPermissionGranted: Boolean
        get() {
            val listener = ComponentName(applicationContext, ADBNotificationListener::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                return applicationContext.getSystemService(NotificationManager::class.java)
                    .isNotificationListenerAccessGranted(listener)
            }
            // Match the exact component, a package-name substring also matches e.g. "<pkg>.debug"
            return Settings.Secure
                .getString(
                    applicationContext.contentResolver,
                    "enabled_notification_listeners"
                )
                ?.split(':')
                ?.any { ComponentName.unflattenFromString(it) == listener } == true
        }

    /**
     * WRITE_SECURE_SETTINGS can only be granted over ADB (`pm grant`), but once granted it shows up
     * as a normal permission grant. Reading Settings.Global never needs it, so a read is no test.
     */
    val writeSecureSettingsPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_SECURE_SETTINGS,
        ) == PackageManager.PERMISSION_GRANTED

    /** The ADB command that grants [writeSecureSettingsPermissionGranted] to this build. */
    val grantWriteSecureSettingsCommand: String
        get() = "adb shell pm grant ${applicationContext.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    /**
     * Android 17+ reports [Settings.Global.DEVELOPMENT_SETTINGS_ENABLED], [Settings.Global.ADB_ENABLED]
     * and `adb_wifi_enabled` as 0 to third-party apps, even with WRITE_SECURE_SETTINGS granted.
     * When true, [developerOptionsEnabled], [usbDebuggingEnabled] and [wirelessDebuggingEnabled]
     * can't be trusted, and ADB state comes from the system's ADB notifications instead
     * ([usbAdbConnected] / [wirelessAdbConnected]).
     */
    val debugStateHidden: Boolean
        get() = Build.VERSION.SDK_INT >= ANDROID_17

    val developerOptionsEnabled: Boolean
        get() {
            return Settings.Global.getInt(
                applicationContext.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
                , 0
            ) == 1
        }

    val usbDebuggingEnabled: Boolean
        get() {
            return Settings.Global.getInt(
                applicationContext.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        }

    val wirelessDebuggingEnabled: Boolean
        get() {
            return try {
                Settings.Global.getInt(
                    applicationContext.contentResolver,
                    "adb_wifi_enabled",
                    0
                ) == 1
            } catch (e: Exception) {
                // Wireless debugging might not be available on all devices/versions
                false
            }
        }

    val stayAwakeValue: Int
        get() {
            return Settings.Global.getInt(
                applicationContext.contentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                0
            )
        }

    val stayAwakeEnabled: Boolean
        get() {
            return stayAwakeValue != OFF
        }

    var showNotification: Boolean
        get() {
            sharedPrefLock.withLock {
                return sharedPreferences.getBoolean(NOTIFICATION_KEY, false)
            }
        }
        set(value) {
            editSharedPref {
                it.putBoolean(NOTIFICATION_KEY, value)
            }
            NotificationUtil.updateStayAwakeNotification(applicationContext)
        }

    var autoToggleStayAwake: Boolean
        get() {
            sharedPrefLock.withLock {
                return sharedPreferences.getBoolean(
                    AUTO_TOGGLE_KEY,
                    true
                ) // Default to true for existing users
            }
        }
        set(value) {
            editSharedPref {
                it.putBoolean(AUTO_TOGGLE_KEY, value)
            }
        }

    private fun editSharedPref(action:(editor: SharedPreferences.Editor) -> Unit) {
        sharedPrefLock.withLock {
            sharedPreferences.edit {
                action.invoke(this)
            }
        }
    }

    fun setUSBDebugging(turnOn: Boolean): Boolean {
        if (developerOptionsEnabled) {
            return setInt(turnOn, Settings.Global.ADB_ENABLED, 1, 0)
        }
        return false
    }

    fun setWirelessDebugging(turnOn: Boolean): Boolean {
        return try {
            if (developerOptionsEnabled) {
                setInt(turnOn, "adb_wifi_enabled", 1, 0)
            } else {
                false
            }
        } catch (e: Exception) {
            // Wireless debugging might not be available on all devices/versions
            false
        }
    }

    fun setStayAwake(turnOn: Boolean): Boolean {
        Log.d(
            "SettingsHelperUtil",
            "setStayAwake called with turnOn=$turnOn, debugStateHidden=$debugStateHidden, developerOptionsEnabled=$developerOptionsEnabled, usbDebuggingEnabled=$usbDebuggingEnabled, wirelessDebuggingEnabled=$wirelessDebuggingEnabled, usbAdbConnected=$usbAdbConnected, wirelessAdbConnected=$wirelessAdbConnected"
        )
        if (debugStateHidden) {
            // Debug settings read as 0 here, so pick the value from the ADB connection(s) we've seen
            val onValue = when {
                usbAdbConnected && wirelessAdbConnected -> ACandUSBandWIRELESS
                wirelessAdbConnected -> ACandWIRELESS
                else -> ACandUSB // USB connection, or a manual toggle with no connection seen
            }
            Log.d("SettingsHelperUtil", "Debug state hidden, setting stay awake to onValue=$onValue")
            return setInt(turnOn, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, onValue, OFF)
        }
        if (developerOptionsEnabled && (!turnOn || usbDebuggingEnabled || wirelessDebuggingEnabled)) {
            // Determine the appropriate stay awake value based on enabled debugging types
            val onValue = when {
                usbDebuggingEnabled && wirelessDebuggingEnabled -> ACandUSBandWIRELESS
                usbDebuggingEnabled -> ACandUSB
                wirelessDebuggingEnabled -> ACandWIRELESS
                else -> ACandUSB // fallback
            }
            Log.d("SettingsHelperUtil", "Setting stay awake to onValue=$onValue")
            return setInt(turnOn, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, onValue, OFF)
        }
        Log.d("SettingsHelperUtil", "setStayAwake returning false - conditions not met")
        return false
    }

    private fun setInt(turnOn: Boolean, name: String, onValue: Int, offValue: Int): Boolean {
        val isOff = Settings.Global.getInt(
            applicationContext.contentResolver,
            name,
            offValue
        ) == offValue

        Log.d(
            "SettingsHelperUtil",
            "setInt: turnOn=$turnOn, name=$name, onValue=$onValue, offValue=$offValue, isOff=$isOff"
        )
        var changed: Boolean = false
        try {
            if (turnOn && isOff) {
                Log.d("SettingsHelperUtil", "Setting $name to $onValue")
                Settings.Global.putInt(
                    applicationContext.contentResolver,
                    name,
                    onValue
                )
                changed = true
            } else if (!turnOn && !isOff) {
                Log.d("SettingsHelperUtil", "Setting $name to $offValue")
                Settings.Global.putInt(
                    applicationContext.contentResolver,
                    name,
                    offValue
                )
                changed = true
            } else {
                Log.d("SettingsHelperUtil", "No change needed for $name")
            }
        } catch (e: SecurityException) {
            Log.e("SettingsHelperUtil", "SecurityException setting $name: ", e)
            //todo: needs permission command
        }
        Log.d("SettingsHelperUtil", "setInt returning changed=$changed")
        return changed
    }

    private fun toast(turnOn: Boolean, name: String) {
        val text: String = if (turnOn) {
            "on"
        } else {
            "off"
        }
        Toast.makeText(
            applicationContext,
            "turned $name $text.",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        val sharedPrefLock: ReentrantLock = ReentrantLock(true)
        const val OFF: Int = 0
        const val AC: Int = BatteryManager.BATTERY_PLUGGED_AC
        const val USB: Int = BatteryManager.BATTERY_PLUGGED_USB
        const val WIRELESS: Int = BatteryManager.BATTERY_PLUGGED_WIRELESS
        const val ACandUSB: Int = BatteryManager.BATTERY_PLUGGED_AC +
                BatteryManager.BATTERY_PLUGGED_USB
        const val ACandWIRELESS: Int = BatteryManager.BATTERY_PLUGGED_AC +
                BatteryManager.BATTERY_PLUGGED_WIRELESS
        const val USBandWIRELESS: Int = BatteryManager.BATTERY_PLUGGED_USB +
                BatteryManager.BATTERY_PLUGGED_WIRELESS
        const val ACandUSBandWIRELESS: Int = BatteryManager.BATTERY_PLUGGED_AC +
                BatteryManager.BATTERY_PLUGGED_USB +
                BatteryManager.BATTERY_PLUGGED_WIRELESS

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
        const val SETTINGS_NOTIFICATION_LISTENER: String =
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

        const val NOTIFICATION_KEY: String = "USE.NOTIFICATION"
        const val AUTO_TOGGLE_KEY: String = "AUTO_TOGGLE_STAY_AWAKE"

        var ADBConnectionState: Boolean = false

        /** Set from the system "USB debugging connected" notification. */
        @Volatile
        var usbAdbConnected: Boolean = false

        /** Set from the system "Wireless debugging connected" notification. */
        @Volatile
        var wirelessAdbConnected: Boolean = false

        /** API level of Android 17, where debug-state reads started being hidden from apps. */
        private const val ANDROID_17: Int = 37
    }
}
