package com.duck.stayawakeadb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    /*
     * The WRITE_SECURE_SETTINGS is a System permission that is not granted to any non-System app. so to get around this
     * after installing the app, you have to connect the device to an ADB console and run this command to grant the
     * permission: 'adb shell pm grant com.duck.stayawakeadb android.permission.WRITE_SECURE_SETTINGS'
     */

    /*
     * ToDo: check if WRITE_SECURE_SETTINGS permission is granted and, if not, prompt to run command.
     */

    private lateinit var helperUtil: HelperUtil
    private var receiverCache: BroadcastReceiver? = null

    private val receiver: BroadcastReceiver
        get() {
            if (receiverCache == null) {
                receiverCache = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        setUpDevOpt()
                    }
                }
            }
            return receiverCache!!
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helperUtil = HelperUtil(applicationContext)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (checkAndAskNotificationPermission()) {
            registerReceiver()
            tv_version.text = fromHtml(getString(R.string.app_version, BuildConfig.VERSION_NAME))
            setUpDevOpt()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver()
    }

    private fun checkAndAskNotificationPermission(): Boolean {
        if (!helperUtil.notificationPermissionGranted) {
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setMessage(getString(R.string.request_notification_permission, getString(R.string.app_name)))
                .setPositiveButton("go to settings") { dialog, which ->
                    startActivity(Intent(HelperUtil.STTINGS_NOTIFICATION_LISTENER))
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.setTitle(R.string.request_notification_permission_title)
            alertDialog.show()
            return false
        }
        return true
    }

    private fun setUpDevOpt() {
        devopt_switch.setOnClickListener(null)
        devopt_switch.isChecked = helperUtil.developerOptionsEnabled
        devopt_group.visibility = View.VISIBLE
        if (helperUtil.developerOptionsEnabled) {
            tv_devopt_des.text = getString(R.string.dev_settings_on)
        } else {
            tv_devopt_des.text = fromHtml(getString(R.string.dev_settings_off))
        }
        setUpUSBDebug()
    }

    private fun setUpUSBDebug() {
        if (helperUtil.developerOptionsEnabled) {
            usbdebug_switch.isChecked = helperUtil.usbDebuggingEnabled
            usbdebug_group.visibility = View.VISIBLE
            usbdebug_switch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!helperUtil.setUSBDebugging(isChecked)) {
                    usbdebug_switch.isChecked = helperUtil.usbDebuggingEnabled
                }
                if (helperUtil.usbDebuggingEnabled) {
                    setUpStayAwake()
                } else {
                    stayawake_group.visibility = View.GONE
                }
            }
        } else {
            usbdebug_group.visibility = View.GONE
            usbdebug_switch.setOnCheckedChangeListener(null)
        }
        setUpStayAwake()
    }

    private fun setUpStayAwake() {
        if (helperUtil.usbDebuggingEnabled) {
            stayawake_switch.isChecked = helperUtil.stayAwakeEnabled
            stayawake_group.visibility = View.VISIBLE
            stayawake_switch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!helperUtil.setStayAwake(isChecked)) {
                    stayawake_switch.isChecked = helperUtil.stayAwakeEnabled
                }
            }

        } else {
            stayawake_group.visibility = View.GONE
            stayawake_switch.setOnCheckedChangeListener(null)
        }
    }

    private fun registerReceiver() {
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(receiver, ADBNotificationListener.intentFilter)
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
    }

    private fun fromHtml(string: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(string)
        }
    }
}
