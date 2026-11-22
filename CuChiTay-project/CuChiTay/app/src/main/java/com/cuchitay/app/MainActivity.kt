package com.cuchitay.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

/** Native Android host for the Liquid Glass UI, hand preview and mini-games. */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private val prefs by lazy { getSharedPreferences(Prefs.NAME, MODE_PRIVATE) }
    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private var webCameraRequest: PermissionRequest? = null
    private var pendingWebResources = emptyArray<String>()
    private var pendingSystemStart = false
    private var pendingAccessibilityReturn = false
    private var notificationAsked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(6, 19, 39)
        window.navigationBarColor = Color.rgb(6, 19, 39)

        web = WebView(this).apply {
            setBackgroundColor(Color.rgb(6, 19, 39))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            addJavascriptInterface(NativeBridge(), "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return request?.url?.host != ASSET_HOST
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    sendStatus()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    val resources = request.resources.filter {
                        it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    }.toTypedArray()
                    if (request.origin.host != ASSET_HOST || resources.isEmpty()) {
                        request.deny()
                        return
                    }
                    runOnUiThread {
                        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(resources)
                        } else {
                            webCameraRequest = request
                            pendingWebResources = resources
                            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_WEB_CAMERA)
                        }
                    }
                }
            }
        }
        setContentView(web)
        web.loadUrl("https://$ASSET_HOST/assets/www/index.html")
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.onResume()
        if (pendingAccessibilityReturn) {
            pendingAccessibilityReturn = false
            if (GestureAccessibilityService.instance != null) continueSystemStart()
            else {
                pendingSystemStart = false
                toast("Bạn chưa bật dịch vụ Cử Chỉ Tay trong phần Trợ năng.")
            }
        }
        sendStatus()
    }

    override fun onPause() {
        if (::web.isInitialized) web.onPause()
        super.onPause()
    }

    @Deprecated("Deprecated by Android; retained for older devices")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        val granted = results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED
        when (code) {
            REQ_WEB_CAMERA -> {
                webCameraRequest?.let { if (granted) it.grant(pendingWebResources) else it.deny() }
                webCameraRequest = null
                pendingWebResources = emptyArray()
            }
            REQ_SYSTEM_CAMERA -> {
                if (granted && pendingSystemStart) continueSystemStart()
                else if (!granted) {
                    pendingSystemStart = false
                    toast("Cần quyền camera để bật điều khiển cử chỉ.")
                }
            }
            REQ_NOTIFICATIONS -> {
                notificationAsked = true
                if (pendingSystemStart) continueSystemStart()
            }
        }
        sendStatus()
    }

    private fun continueSystemStart() {
        if (!pendingSystemStart) return
        if (GestureAccessibilityService.instance == null) {
            pendingAccessibilityReturn = true
            toast("Hãy bật Cử Chỉ Tay trong danh sách dịch vụ Trợ năng, rồi quay lại ứng dụng.")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_SYSTEM_CAMERA)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !notificationAsked &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationAsked = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
            return
        }
        pendingSystemStart = false
        ContextCompat.startForegroundService(this, Intent(this, GestureService::class.java))
        toast("Đã bật điều khiển toàn hệ thống. Bạn có thể chuyển sang ứng dụng khác.")
        sendStatus()
    }

    private fun startSystemControlNative() {
        if (GestureService.running) return
        pendingSystemStart = true
        continueSystemStart()
    }

    private fun stopSystemControlNative() {
        pendingSystemStart = false
        stopService(Intent(this, GestureService::class.java))
        toast("Đã tắt điều khiển toàn hệ thống.")
        sendStatus()
    }

    private fun sendStatus() {
        if (!::web.isInitialized) return
        val running = GestureService.running
        web.post { web.evaluateJavascript("window.CuChiTayNativeStatus&&window.CuChiTayNativeStatus($running);", null) }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private inner class NativeBridge {
        @JavascriptInterface fun getConfig(): String {
            val j = JSONObject()
            j.put("up", prefs.getBoolean(Prefs.SWIPE_UP, true))
            j.put("down", prefs.getBoolean(Prefs.SWIPE_DOWN, true))
            j.put("left", prefs.getBoolean(Prefs.SWIPE_LEFT, false))
            j.put("right", prefs.getBoolean(Prefs.SWIPE_RIGHT, false))
            j.put("clench", prefs.getBoolean(Prefs.GRAB, true))
            j.put("reverse", prefs.getBoolean(Prefs.INVERT, false))
            j.put("sensitivity", prefs.getInt(Prefs.SENS, 58))
            j.put("scrollSpeed", prefs.getInt(Prefs.SCROLL_SPEED, 74))
            return j.toString()
        }

        @JavascriptInterface fun setGestureEnabled(key: String, value: Boolean) {
            val pref = when (key) {
                "up" -> Prefs.SWIPE_UP
                "down" -> Prefs.SWIPE_DOWN
                "left" -> Prefs.SWIPE_LEFT
                "right" -> Prefs.SWIPE_RIGHT
                "clench" -> Prefs.GRAB
                "reverse" -> Prefs.INVERT
                else -> return
            }
            prefs.edit().putBoolean(pref, value).apply()
        }
        @JavascriptInterface fun setSensitivity(value: Int) = prefs.edit().putInt(Prefs.SENS, value.coerceIn(0, 100)).apply()
        @JavascriptInterface fun setScrollSpeed(value: Int) = prefs.edit().putInt(Prefs.SCROLL_SPEED, value.coerceIn(0, 100)).apply()
        @JavascriptInterface fun isSystemControlRunning(): Boolean = GestureService.running
        @JavascriptInterface fun startSystemControl() { runOnUiThread { startSystemControlNative() } }
        @JavascriptInterface fun stopSystemControl() { runOnUiThread { stopSystemControlNative() } }
        @JavascriptInterface fun openAccessibilitySettings() { runOnUiThread { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        @JavascriptInterface fun openAppSettings() {
            runOnUiThread { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        }
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }

    companion object {
        private const val ASSET_HOST = "appassets.androidplatform.net"
        private const val REQ_WEB_CAMERA = 601
        private const val REQ_SYSTEM_CAMERA = 602
        private const val REQ_NOTIFICATIONS = 603
    }
}
