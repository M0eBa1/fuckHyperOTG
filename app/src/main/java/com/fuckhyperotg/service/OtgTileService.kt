package com.fuckhyperotg.service

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.fuckhyperotg.utils.ShellUtils

class OtgTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val isCurrentlyActive = tile.state == Tile.STATE_ACTIVE

        val newState = if (isCurrentlyActive) 0 else 1
        try {
            Settings.Global.putInt(contentResolver, "otg_switch", newState)
            Settings.System.putInt(contentResolver, "otg_switch", newState)
        } catch (_: SecurityException) {
            // If cannot write settings directly without permission, try root
            if (newState == 1) {
                ShellUtils.forceEnableOtgSysfs()
            } else {
                ShellUtils.executeRootCommand("echo 0 > /sys/class/power_supply/usb/otg_switch 2>/dev/null; settings put global otg_switch 0")
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isOtgOn = try {
            Settings.Global.getInt(contentResolver, "otg_switch", 0) == 1
        } catch (_: Throwable) {
            false
        }

        tile.state = if (isOtgOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
