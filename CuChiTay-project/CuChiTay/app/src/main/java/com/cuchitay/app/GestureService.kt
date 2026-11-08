package com.cuchitay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Dịch vụ nền (foreground, loại camera): đọc camera trước -> MediaPipe nhận diện
 * bàn tay -> GestureEngine nhận ra cử chỉ -> nhờ GestureAccessibilityService thực hiện.
 * Ảnh camera chỉ xử lý tại chỗ trong bộ nhớ, không lưu, không gửi đi đâu.
 */
class GestureService : LifecycleService() {

    companion object {
        @Volatile
        var running = false

        const val ACTION_STOP = "com.cuchitay.app.STOP"
        private const val CHANNEL_ID = "gesture_channel"
        private const val NOTIF_ID = 1
        private const val TAG = "GestureService"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var engine: GestureEngine
    private lateinit var analysisExecutor: ExecutorService
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var handLandmarker: HandLandmarker? = null
    private var orientationListener: OrientationEventListener? = null

    private var started = false
    private var lastTimestamp = 0L
    @Volatile private var frameAspect = 0.75f

    override fun onCreate() {
        super.onCreate()
        running = true
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        analysisExecutor = Executors.newSingleThreadExecutor()
        engine = GestureEngine(
            onSwipe = { dir ->
                mainHandler.post { GestureAccessibilityService.instance?.swipe(dir) }
            },
            onGrab = {
                mainHandler.post { GestureAccessibilityService.instance?.screenshot() }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        )

        if (!started) {
            started = true
            startPipeline()
        }
        return START_NOT_STICKY
    }

    private fun startPipeline() {
        try {
            val base = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ -> handleResult(result) }
                .setErrorListener { e -> Log.e(TAG, "MediaPipe lỗi", e) }
                .build()
            handLandmarker = HandLandmarker.createFromOptions(this, options)
        } catch (e: Exception) {
            Log.e(TAG, "Không tạo được bộ nhận diện bàn tay", e)
            Toast.makeText(this, "Không tải được mô hình nhận diện bàn tay", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider

                val a = ImageAnalysis.Builder()
                    .setTargetResolution(Size(480, 640))
                    .setTargetRotation(Surface.ROTATION_0)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                a.setAnalyzer(analysisExecutor) { proxy -> analyze(proxy) }
                analysis = a

                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, a)

                // Theo dõi hướng cầm máy để ảnh luôn thẳng đứng
                orientationListener = object : OrientationEventListener(this) {
                    override fun onOrientationChanged(orientation: Int) {
                        if (orientation == ORIENTATION_UNKNOWN) return
                        analysis?.targetRotation = when (orientation) {
                            in 45 until 135 -> Surface.ROTATION_270
                            in 135 until 225 -> Surface.ROTATION_180
                            in 225 until 315 -> Surface.ROTATION_90
                            else -> Surface.ROTATION_0
                        }
                    }
                }.also { it.enable() }
            } catch (e: Exception) {
                Log.e(TAG, "Không mở được camera trước", e)
                Toast.makeText(this, "Không mở được camera trước", Toast.LENGTH_LONG).show()
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(proxy: ImageProxy) {
        try {
            val landmarker = handLandmarker ?: return
            val raw = proxy.toBitmap()
            val rotation = proxy.imageInfo.rotationDegrees
            val upright: Bitmap = if (rotation != 0) {
                val m = Matrix()
                m.postRotate(rotation.toFloat())
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
            } else {
                raw
            }
            frameAspect = upright.width.toFloat() / upright.height.toFloat()

            var ts = SystemClock.uptimeMillis()
            if (ts <= lastTimestamp) ts = lastTimestamp + 1
            lastTimestamp = ts

            landmarker.detectAsync(BitmapImageBuilder(upright).build(), ts)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xử lý khung hình", e)
        } finally {
            proxy.close()
        }
    }

    private fun handleResult(result: HandLandmarkerResult) {
        applyPrefs()
        val hands = result.landmarks()
        val now = result.timestampMs()
        if (hands.isEmpty()) {
            engine.onHand(null, now, frameAspect)
        } else {
            engine.onHand(hands[0], now, frameAspect)
        }
    }

    private fun applyPrefs() {
        engine.swipeEnabled = prefs.getBoolean(Prefs.SWIPE, true)
        engine.grabEnabled = prefs.getBoolean(Prefs.GRAB, true)
        engine.horizontalEnabled = prefs.getBoolean(Prefs.HORIZONTAL, false)
        engine.invertVertical = prefs.getBoolean(Prefs.INVERT, false)
        engine.sensitivity = 0.6f + prefs.getInt(Prefs.SENS, 50) / 100f * 0.9f
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Điều khiển bằng cử chỉ", NotificationManager.IMPORTANCE_LOW)
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, GestureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hand)
            .setContentTitle("Cử Chỉ Tay đang chạy")
            .setContentText("Camera trước đang theo dõi cử chỉ tay. Chạm để mở.")
            .setContentIntent(openIntent)
            .addAction(0, "Tắt", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        try {
            orientationListener?.disable()
            analysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
            handLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tắt dịch vụ", e)
        }
        super.onDestroy()
    }
}
