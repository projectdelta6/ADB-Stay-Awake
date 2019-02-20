package com.duck.stayawakeadb

import android.content.Context
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast

/**
 * Created by Bradley Duck on 2019/02/18.
 */
class HelperUtil(private val applicationContext: Context?) {

    val notificationPermissionGranted: Boolean
        get() {
            return Settings.Secure
                .getString(
                    applicationContext?.contentResolver,
                    "enabled_notification_listeners"
                )
                .contains(applicationContext?.packageName!!)
        }

    val developerOptionsEnabled: Boolean
        get() {
            return Settings.Global.getInt(
                applicationContext?.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
                , 0
            ) == 1
        }

    val USBDebuggingEnabled: Boolean
        get() {
            return Settings.Global.getInt(
                applicationContext?.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        }

    val StayAwakeValue: Int
        get() {
            return Settings.Global.getInt(
                applicationContext?.contentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                0
            )
        }

    val StayAwakeEnabled: Boolean
        get() {
            return StayAwakeValue != OFF
        }

    fun setUSBDebugging(turnOn: Boolean) {
        if (developerOptionsEnabled) {
            var text: String
            if (turnOn) {
                text = "on"
                Settings.Global.putInt(
                    applicationContext?.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    1
                )
            } else {
                text = "off"
                Settings.Global.putInt(
                    applicationContext?.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    0
                )
            }
            Toast.makeText(
                applicationContext,
                "Turned USB debugging $text.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun setStayAwake(turnOn: Boolean) {
        if (developerOptionsEnabled && USBDebuggingEnabled) {
            var text: String
            if (turnOn) {
                text = "on"
                Settings.Global.putInt(
                    applicationContext?.contentResolver,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    ACandUSBandWIRELESS
                )
            } else {
                text = "off"
                Settings.Global.putInt(
                    applicationContext?.contentResolver,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    OFF
                )
            }
            Toast.makeText(
                applicationContext,
                "Turned StayAwake $text.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val stayAwakeString: String
        get() {
            when (StayAwakeValue) {
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

    companion object {
        const val OFF: Int = 0
        const val AC: Int = BatteryManager.BATTERY_PLUGGED_AC
        const val USB: Int = BatteryManager.BATTERY_PLUGGED_USB
        const val WIRELESS: Int = BatteryManager.BATTERY_PLUGGED_WIRELESS
        const val ACandUSB: Int = BatteryManager.BATTERY_PLUGGED_AC + BatteryManager.BATTERY_PLUGGED_USB
        const val ACandWIRELESS: Int = BatteryManager.BATTERY_PLUGGED_AC + BatteryManager.BATTERY_PLUGGED_WIRELESS
        const val USBandWIRELESS: Int = BatteryManager.BATTERY_PLUGGED_USB + BatteryManager.BATTERY_PLUGGED_WIRELESS
        const val ACandUSBandWIRELESS: Int =
            BatteryManager.BATTERY_PLUGGED_AC + BatteryManager.BATTERY_PLUGGED_USB + BatteryManager.BATTERY_PLUGGED_WIRELESS
    }
}
