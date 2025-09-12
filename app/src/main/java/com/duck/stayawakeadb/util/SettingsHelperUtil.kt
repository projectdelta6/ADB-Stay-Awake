package com.duck.stayawakeadb.util

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
import com.duck.stayawakeadb.R
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
            return Settings.Secure
                .getString(
                    applicationContext.contentResolver,
                    "enabled_notification_listeners"
                )?.contains(applicationContext.packageName!!) == true
        }

    val writeSecureSettingsPermissionGranted: Boolean
        get() {
            return try {
                // Try to read a secure setting that requires WRITE_SECURE_SETTINGS
                Settings.Global.getInt(
                    applicationContext.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    -1
                )
                true
            } catch (e: SecurityException) {
                false
            }
        }

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
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            action.invoke(editor)
            editor.apply()
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
            "setStayAwake called with turnOn=$turnOn, developerOptionsEnabled=$developerOptionsEnabled, usbDebuggingEnabled=$usbDebuggingEnabled, wirelessDebuggingEnabled=$wirelessDebuggingEnabled"
        )
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
            } else if (!isOff) {
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
        } finally {
            /*if (changed) {
                toast(turnOn, name)
            }*/
            Log.d("SettingsHelperUtil", "setInt returning changed=$changed")
            return changed
        }
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

    /*
    val stayAwakeString: String
        get() {
            when (stayAwakeValue) {
                OFF -> {
                    //Stay Awake is off.
                    return "Stay Awake is off."
                }
                AC -> {
                    //Stay Awake is on for AC only.
                    return "Stay Awake is on for AC only."
                }
                USB -> {
                    //Stay Awake is on for USB only.
                    return "Stay Awake is on for USB only."
                }
                WIRELESS -> {
                    //Stay Awake is on for Wireless charging only.
                    return "Stay Awake is on for Wireless charging only."
                }
                ACandUSB -> {
                    //Stay Awake is on for AC and USB only.
                    return "Stay Awake is on for AC and USB only."
                }
                ACandWIRELESS -> {
                    //Stay Awake is on for AC and Wireless charging only.
                    return "Stay Awake is on for AC and Wireless charging only."
                }
                USBandWIRELESS -> {
                    //Stay Awake is on for USB and Wireless charging only.
                    return "Stay Awake is on for USB and Wireless charging only."
                }
                ACandUSBandWIRELESS -> {
                    //Stay Awake is on for AC, USB and Wireless charging.
                    return "Stay Awake is on for AC, USB and Wireless charging."
                }
            }
            return "none"
        }
    */

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
        const val STTINGS: String = "android.settings."
        const val STTINGS_NOTIFICATION_LISTENER: String = "${STTINGS}ACTION_NOTIFICATION_LISTENER_SETTINGS"
        const val STTINGS_DEVELOPER: String = "${STTINGS}ACTION_APPLICATION_DEVELOPMENT_SETTINGS"
        const val STTINGS_WIRELESS_DEBUG: String =
            "${STTINGS}ACTION_APPLICATION_DEVELOPMENT_SETTINGS" // Wireless debugging is in developer settings

        const val NOTIFICATION_KEY: String = "USE.NOTIFICATION"
        const val AUTO_TOGGLE_KEY: String = "AUTO_TOGGLE_STAY_AWAKE"

        var ADBConnectionState: Boolean = false
    }
}
