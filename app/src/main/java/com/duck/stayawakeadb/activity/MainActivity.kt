package com.duck.stayawakeadb.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.BuildConfig
import com.duck.stayawakeadb.R
import com.duck.stayawakeadb.constant.Constants.notificationData
import com.duck.stayawakeadb.databinding.ActivityMainBinding
import com.duck.stayawakeadb.service.ADBNotificationListener
import com.duck.stayawakeadb.util.NotificationUtil
import com.duck.stayawakeadb.util.SettingsHelperUtil


class MainActivity : AppCompatActivity() {

    /*
     * The WRITE_SECURE_SETTINGS is a System permission that is not granted to any non-System app. so to get around this
     * after installing the app, you have to connect the device to an ADB console and run this command to grant the
     * permission: 'adb shell pm grant com.duck.stayawakeadb android.permission.WRITE_SECURE_SETTINGS'
     */

    /*
     * ToDo: check if WRITE_SECURE_SETTINGS permission is granted and, if not, prompt to run command.
     */

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsHelperUtil: SettingsHelperUtil
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
        settingsHelperUtil = SettingsHelperUtil(applicationContext)
        //ensure the notification channel is created
        NotificationUtil.createNotificationChannel(
            this,
            notificationData
        )
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        if (checkAndAskNotificationPermission()) {
            registerReceiver()
            binding.tvVersion.text = fromHtml(
                getString(
                    R.string.app_version,
                    BuildConfig.VERSION_NAME
                )
            )
            setUpDevOpt()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver()
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
                .setPositiveButton(R.string.go_to_settings) { dialog, which ->
                    startActivity(Intent(SettingsHelperUtil.STTINGS_NOTIFICATION_LISTENER))
                }
                .setNegativeButton(R.string.cancel) { dialog, which ->
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
        binding.devoptSwitch.setOnClickListener(null)
        binding.devoptSwitch.isChecked = settingsHelperUtil.developerOptionsEnabled
        binding.devoptGroup.visibility = View.VISIBLE
        if (settingsHelperUtil.developerOptionsEnabled) {
            binding.tvDevoptDes.text = getString(R.string.dev_settings_on)
        } else {
            binding.tvDevoptDes.text = fromHtml(getString(R.string.dev_settings_off))
        }
        setUpUSBDebug()
    }

    private fun setUpUSBDebug() {
        if (settingsHelperUtil.developerOptionsEnabled) {
            binding.usbdebugSwitch.setOnCheckedChangeListener(null)// clear listener
            binding.usbdebugSwitch.isChecked =
                settingsHelperUtil.usbDebuggingEnabled// set checked state
            binding.usbdebugGroup.visibility = View.VISIBLE
            binding.usbdebugSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!settingsHelperUtil.setUSBDebugging(isChecked)) {
                    binding.usbdebugSwitch.isChecked = settingsHelperUtil.usbDebuggingEnabled
                }
                if (settingsHelperUtil.usbDebuggingEnabled) {
                    setUpStayAwake()
                    setUpNotificationSetting()
                } else {
                    binding.stayawakeGroup.visibility = View.GONE
                    binding.notificationGroup.visibility = View.GONE
                }
            }// set listener
        } else {
            binding.usbdebugGroup.visibility = View.GONE
            binding.usbdebugSwitch.setOnCheckedChangeListener(null)
        }
        setUpStayAwake()
        setUpNotificationSetting()
    }

    private fun setUpStayAwake() {
        if (settingsHelperUtil.usbDebuggingEnabled) {
            binding.stayawakeSwitch.setOnCheckedChangeListener(null)//clear listener
            binding.stayawakeSwitch.isChecked =
                settingsHelperUtil.stayAwakeEnabled//set checked state
            binding.stayawakeGroup.visibility = View.VISIBLE
            binding.stayawakeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!settingsHelperUtil.setStayAwake(isChecked)) {
                    binding.stayawakeSwitch.isChecked = settingsHelperUtil.stayAwakeEnabled
                }
                NotificationUtil.updateStayAwakeNotification(this)
            }// set listener

        } else {
            binding.stayawakeGroup.visibility = View.GONE
            binding.stayawakeSwitch.setOnCheckedChangeListener(null)
        }
    }

    private fun setUpNotificationSetting() {
        binding.notificationSwitch.setOnCheckedChangeListener(null)
        binding.notificationSwitch.isChecked = settingsHelperUtil.showNotification
        binding.notificationGroup.visibility = View.VISIBLE
        binding.notificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            settingsHelperUtil.showNotification = isChecked
        }
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

    private fun fromHtml(string: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(string)
        }
    }
}
