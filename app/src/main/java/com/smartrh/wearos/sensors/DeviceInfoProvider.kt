package com.smartrh.wearos.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class DeviceInfoProvider(private val context: Context) {
    fun getBatteryPercent(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        if (level < 0 || scale <= 0) return 0

        return ((level / scale.toFloat()) * 100).toInt()
    }
}
