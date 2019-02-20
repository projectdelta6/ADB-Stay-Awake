# ADB-Stay-Awake
This is a simple app that has one purpose: Automatically enable/dissable the "Stay Awake" Developer Options setting as you connect/disconnect to ADB.

## Important

The android.permission.WRITE_SECURE_SETTINGS is a System permission that is not granted to any non-System app.
So to get around this, after installing the app, you have to connect the device to an ADB console and run this command to grant the permission:
```
adb shell pm grant com.duck.stayawakeadb android.permission.WRITE_SECURE_SETTINGS
```

## ToDo:
* check if WRITE_SECURE_SETTINGS permission is granted and, if not, prompt to run command.
* make it look sexy.
