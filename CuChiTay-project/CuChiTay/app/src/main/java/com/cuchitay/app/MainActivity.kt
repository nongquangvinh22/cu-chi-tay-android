package com.cuchitay.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var tvCam: TextView
    private lateinit var tvA11y: TextView
    private lateinit var tvSens: TextView
    private lateinit var btnCam: Button
    private lateinit var btnToggle: Button

    private val prefs by lazy { getSharedPreferences(Prefs.NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCam = findViewById(R.id.tvCam)
        tvA11y = findViewById(R.id.tvA11y)
        tvSens = findViewById(R.id.tvSens)
        btnCam = findViewById(R.id.btnCam)
        btnToggle = findViewById(R.id.btnToggle)

        btnCam.setOnClickListener { requestNeededPermissions() }

        findViewById<Button>(R.id.btnA11y).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnAppInfo).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        }

        bindSwitch(R.id.swSwipe, Prefs.SWIPE, true)
        bindSwitch(R.id.swGrab, Prefs.GRAB, true)
        bindSwitch(R.id.swHorizontal, Prefs.HORIZONTAL, false)
        bindSwitch(R.id.swInvert, Prefs.INVERT, false)

        val seek = findViewById<SeekBar>(R.id.seekSens)
        seek.progress = prefs.getInt(Prefs.SENS, 50)
        updateSensLabel(seek.progress)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt(Prefs.SENS, progress).apply()
                updateSensLabel(progress)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        btnToggle.setOnClickListener { toggle() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }

    private fun bindSwitch(id: Int, key: String, default: Boolean) {
        val sw = findViewById<Switch>(id)
        sw.isChecked = prefs.getBoolean(key, default)
        sw.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(key, checked).apply()
        }
    }

    private fun updateSensLabel(progress: Int) {
        tvSens.text = "Độ nhạy khi hất tay: $progress%  (kéo sang phải = nhạy hơn)"
    }

    private fun hasCamera(): Boolean =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestNeededPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 100)
    }

    private fun toggle() {
        if (GestureService.running) {
            stopService(Intent(this, GestureService::class.java))
        } else {
            if (!hasCamera()) {
                requestNeededPermissions()
                return
            }
            if (GestureAccessibilityService.instance == null) {
                Toast.makeText(
                    this,
                    "Hãy bật \"Cử Chỉ Tay\" trong cài đặt Trợ năng trước",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return
            }
            ContextCompat.startForegroundService(this, Intent(this, GestureService::class.java))
            Toast.makeText(
                this,
                "Đã bật. Hãy chuyển sang ứng dụng khác và thử hất tay trước camera.",
                Toast.LENGTH_LONG
            ).show()
        }
        btnToggle.postDelayed({ refresh() }, 500)
    }

    private fun refresh() {
        val camOk = hasCamera()
        tvCam.text = if (camOk) "✅ Quyền camera: đã cấp" else "❌ Quyền camera: chưa cấp"
        btnCam.visibility = if (camOk) View.GONE else View.VISIBLE

        val a11yOk = GestureAccessibilityService.instance != null
        tvA11y.text = if (a11yOk) {
            "✅ Trợ năng: đã bật"
        } else {
            "❌ Trợ năng: chưa bật (vào Cài đặt Trợ năng, chọn Cử Chỉ Tay rồi bật)"
        }

        btnToggle.text = if (GestureService.running) {
            "TẮT điều khiển bằng cử chỉ"
        } else {
            "BẬT điều khiển bằng cử chỉ"
        }
    }
}
