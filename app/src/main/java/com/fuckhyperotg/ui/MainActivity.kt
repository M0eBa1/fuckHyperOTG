package com.fuckhyperotg.ui

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fuckhyperotg.R
import com.fuckhyperotg.databinding.ActivityMainBinding
import com.fuckhyperotg.utils.PrefsManager
import com.fuckhyperotg.utils.ShellUtils
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStatusView()
        initSettingsToggles()
        initActionButtons()
    }

    override fun onResume() {
        super.onResume()
        initStatusView()
    }

    /**
     * Hooked by HookEntry when the Xposed module is active.
     * Returns false by default if not hooked.
     */
    fun isModuleActivated(): Boolean {
        return false
    }

    private fun initStatusView() {
        val activated = isModuleActivated()
        if (activated) {
            val green = ContextCompat.getColor(this, R.color.status_green)
            binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(green)
            binding.tvStatusTitle.setText(R.string.status_activated)
            binding.tvStatusTitle.setTextColor(green)
            binding.tvStatusDesc.setText(R.string.status_desc_activated)
        } else {
            val red = ContextCompat.getColor(this, R.color.status_red)
            binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(red)
            binding.tvStatusTitle.setText(R.string.status_not_activated)
            binding.tvStatusTitle.setTextColor(red)
            binding.tvStatusDesc.setText(R.string.status_desc_not_activated)
        }
    }

    private fun initSettingsToggles() {
        val prefs = PrefsManager.getAppPrefs(this)

        binding.switchPreventTimeout.isChecked = prefs.getBoolean(PrefsManager.KEY_PREVENT_TIMEOUT, true)
        binding.switchForceShow.isChecked = prefs.getBoolean(PrefsManager.KEY_FORCE_SHOW_SWITCH, true)
        binding.switchKeepPower.isChecked = prefs.getBoolean(PrefsManager.KEY_KEEP_HOST_POWER, true)

        binding.switchPreventTimeout.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PrefsManager.KEY_PREVENT_TIMEOUT, isChecked).apply()
        }

        binding.switchForceShow.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PrefsManager.KEY_FORCE_SHOW_SWITCH, isChecked).apply()
        }

        binding.switchKeepPower.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PrefsManager.KEY_KEEP_HOST_POWER, isChecked).apply()
        }
    }

    private fun initActionButtons() {
        binding.btnOpenSettings.setOnClickListener {
            openSystemOtgSettings()
        }

        binding.btnRootForceOtg.setOnClickListener {
            binding.btnRootForceOtg.isEnabled = false
            thread {
                val success = ShellUtils.forceEnableOtgSysfs()
                runOnUiThread {
                    binding.btnRootForceOtg.isEnabled = true
                    val msg = if (success) {
                        getString(R.string.toast_root_success)
                    } else {
                        getString(R.string.toast_root_failed)
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openSystemOtgSettings() {
        val candidates = listOf(
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.SubSettings")),
            Intent("android.settings.SETTINGS")
        )

        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}
