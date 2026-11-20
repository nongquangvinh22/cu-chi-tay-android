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
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
    private lateinit var tvSpeed: TextView
    private lateinit var btnCam: Button
    private lateinit var btnToggle: Button

    private val prefs by lazy { getSharedPreferences(Prefs.NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        migrateLegacyGesturePrefs()

        tvCam = findViewById(R.id.tvCam)
        tvA11y = findViewById(R.id.tvA11y)
        tvSens = findViewById(R.id.tvSens)
        tvSpeed = findViewById(R.id.tvSpeed)
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

        bindSwitch(R.id.swSwipeUp, Prefs.SWIPE_UP, true)
        bindSwitch(R.id.swSwipeDown, Prefs.SWIPE_DOWN, true)
        bindSwitch(R.id.swSwipeLeft, Prefs.SWIPE_LEFT, false)
        bindSwitch(R.id.swSwipeRight, Prefs.SWIPE_RIGHT, false)
        bindSwitch(R.id.swGrab, Prefs.GRAB, true)
        bindSwitch(R.id.swInvert, Prefs.INVERT, false)

        val sensitivitySeek = findViewById<SeekBar>(R.id.seekSens)
        sensitivitySeek.progress = prefs.getInt(Prefs.SENS, 50).coerceIn(0, 100)
        updateSensLabel(sensitivitySeek.progress)
        sensitivitySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt(Prefs.SENS, progress).apply()
                updateSensLabel(progress)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        val speedSeek = findViewById<SeekBar>(R.id.seekSpeed)
        speedSeek.progress = prefs.getInt(
            Prefs.SCROLL_SPEED,
            Prefs.DEFAULT_SCROLL_SPEED
        ).coerceIn(0, 100)
        updateSpeedLabel(speedSeek.progress)
        speedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.edit().putInt(Prefs.SCROLL_SPEED, progress).apply()
                updateSpeedLabel(progress)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        btnToggle.setOnClickListener {
            animateButtonFeedback()
            toggle()
        }
    }

    /**
     * Chuyển các thiết lập gộp của v1.1 sang các công tắc riêng.
     * Nếu đã có thiết lập mới thì không ghi đè lựa chọn của người dùng.
     */
    private fun migrateLegacyGesturePrefs() {
        val edit = prefs.edit()
        if (!prefs.contains(Prefs.SWIPE_UP)) {
            edit.putBoolean(Prefs.SWIPE_UP, prefs.getBoolean(Prefs.SWIPE, true))
        }
        if (!prefs.contains(Prefs.SWIPE_DOWN)) {
            edit.putBoolean(Prefs.SWIPE_DOWN, prefs.getBoolean(Prefs.SWIPE, true))
        }
        if (!prefs.contains(Prefs.SWIPE_LEFT)) {
            edit.putBoolean(Prefs.SWIPE_LEFT, prefs.getBoolean(Prefs.HORIZONTAL, false))
        }
        if (!prefs.contains(Prefs.SWIPE_RIGHT)) {
            edit.putBoolean(Prefs.SWIPE_RIGHT, prefs.getBoolean(Prefs.HORIZONTAL, false))
        }
        edit.apply()
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
        val gestureSwitch = findViewById<Switch>(id)
        val row = gestureSwitch.parent as? View
        gestureSwitch.isChecked = prefs.getBoolean(key, default)
        row?.isActivated = gestureSwitch.isChecked

        gestureSwitch.setOnCheckedChangeListener { button, checked ->
            prefs.edit().putBoolean(key, checked).apply()
            row?.isActivated = checked

            // Thumb vẫn trượt theo animation gốc của Android; lớp nảy nhẹ làm phản hồi rõ hơn.
            button.animate().cancel()
            button.scaleX = 1f
            button.scaleY = 1f
            button.animate()
                .scaleX(1.075f)
                .scaleY(1.075f)
                .setDuration(115)
                .setInterpolator(OvershootInterpolator(1.45f))
                .withEndAction {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(165)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                .start()

            row?.animate()?.cancel()
            row?.animate()
                ?.translationY(if (checked) -dp(1.5f) else 0f)
                ?.setDuration(170)
                ?.setInterpolator(DecelerateInterpolator())
                ?.start()
        }
    }

    private fun animateButtonFeedback() {
        btnToggle.animate().cancel()
        btnToggle.scaleX = 1f
        btnToggle.scaleY = 1f
        btnToggle.animate()
            .scaleX(0.985f)
            .scaleY(0.985f)
            .setDuration(75)
            .withEndAction {
                btnToggle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(175)
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()
            }
            .start()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun updateSensLabel(progress: Int) {
        tvSens.text = "Độ nhạy nhận diện: $progress%  ·  Sang phải để nhận diện nhạy hơn"
    }

    private fun updateSpeedLabel(progress: Int) {
        val description = when {
            progress >= 80 -> "rất nhanh"
            progress >= 60 -> "nhanh"
            progress >= 35 -> "vừa phải"
            else -> "chậm, mượt"
        }
        tvSpeed.text = "Tốc độ cuộn: $progress%  ·  $description"
    }

    private fun hasCamera(): Boolean =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(permissions.toTypedArray(), 100)
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
                    "Hãy bật \\"Cử Chỉ Tay\\" trong cài đặt Trợ năng trước",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return
            }
            ContextCompat.startForegroundService(this, Intent(this, GestureService::class.java))
            Toast.makeText(
                this,
                "Đã bật. Hãy chuyển sang ứng dụng khác và thử cử chỉ trước camera.",
                Toast.LENGTH_LONG
            ).show()
        }
        btnToggle.postDelayed({ refresh() }, 500)
    }

    private fun refresh() {
        val cameraOk = hasCamera()
        tvCam.text = if (cameraOk) "✓  Quyền camera: đã cấp" else "○  Quyền camera: chưa cấp"
        tvCam.setTextColor(getColor(if (cameraOk) R.color.status_good else R.color.status_warning))
        btnCam.visibility = if (cameraOk) View.GONE else View.VISIBLE

        val accessibilityOk = GestureAccessibilityService.instance != null
        tvA11y.text = if (accessibilityOk) {
            "✓  Trợ năng: đã bật"
        } else {
            "○  Trợ năng: chưa bật · Mở cài đặt và bật dịch vụ Cử Chỉ Tay"
        }
        tvA11y.setTextColor(
            getColor(if (accessibilityOk) R.color.status_good else R.color.status_warning)
        )

        val isRunning = GestureService.running
        btnToggle.text = if (isRunning) {
            "ĐANG HOẠT ĐỘNG  ·  CHẠM ĐỂ TẮT"
        } else {
            "BẬT ĐIỀU KHIỂN BẰNG CỬ CHỈ"
        }
        btnToggle.setBackgroundResource(
            if (isRunning) R.drawable.bg_button_stop else R.drawable.bg_button_primary
        )
        btnToggle.backgroundTintList = null
    }
}
