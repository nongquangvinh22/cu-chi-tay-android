package com.cuchitay.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.net.URLEncoder

/**
 * Compact in-app browser with multiple tabs, history controls and downloads.
 * Web content stays in Android WebView; only HTTP and HTTPS navigation is allowed.
 */
class BrowserActivity : Activity() {

    companion object {
        const val EXTRA_URL = "com.cuchitay.app.BROWSER_URL"
        private const val HOME_URL = "https://www.google.com/?safe=active"
    }

    private data class BrowserTab(
        val webView: WebView,
        var title: String = "Tab mới",
        var url: String = HOME_URL
    )

    private val tabs = mutableListOf<BrowserTab>()
    private var activeIndex = -1

    private lateinit var address: EditText
    private lateinit var tabRow: LinearLayout
    private lateinit var webHost: FrameLayout
    private lateinit var browserProgress: ProgressBar
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 19, 39)
        window.navigationBarColor = Color.rgb(7, 19, 39)
        buildInterface()
        addTab(intent.getStringExtra(EXTRA_URL).orEmpty())
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 18, 36))
            setPadding(dp(8), dp(8), dp(8), dp(4))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        backButton = toolButton("‹") { currentWeb()?.let { if (it.canGoBack()) it.goBack() } }
        forwardButton = toolButton("›") { currentWeb()?.let { if (it.canGoForward()) it.goForward() } }
        val reloadButton = toolButton("↻") { currentWeb()?.reload() }

        address = EditText(this).apply {
            setSingleLine(true)
            textSize = 13f
            setTextColor(Color.rgb(242, 249, 255))
            setHintTextColor(Color.rgb(158, 183, 211))
            hint = "Tìm kiếm hoặc nhập địa chỉ…"
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedBackground(Color.rgb(29, 47, 76), Color.rgb(87, 132, 178), 16)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
            setOnEditorActionListener { _, actionId, event ->
                val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    loadAddress()
                    true
                } else false
            }
        }
        val goButton = toolButton("Mở") { loadAddress() }
        toolbar.addView(backButton)
        toolbar.addView(forwardButton)
        toolbar.addView(reloadButton)
        toolbar.addView(address)
        toolbar.addView(goButton)
        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
        ))

        browserProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(111, 225, 255))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }
        root.addView(browserProgress, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(3)
        ))

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(0, dp(45), 1f)
        }
        tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scroll.addView(tabRow)
        tabBar.addView(scroll)
        tabBar.addView(toolButton("+") { addTab("") })
        tabBar.addView(toolButton("×") { closeActiveTab() })
        root.addView(tabBar)

        webHost = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(250, 251, 253))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(webHost)

        val copyright = TextView(this).apply {
            text = "✦  © Nông Quang Vinh"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(162, 190, 220))
            setPadding(0, dp(8), 0, dp(5))
        }
        root.addView(copyright, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(25)
        ))

        setContentView(root)
    }

    private fun toolButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = if (label.length > 1) 12f else 20f
            isAllCaps = false
            setTextColor(Color.rgb(238, 248, 255))
            background = roundedBackground(Color.rgb(36, 57, 87), Color.rgb(90, 129, 165), 14)
            minWidth = dp(40)
            minimumWidth = dp(40)
            minHeight = dp(40)
            minimumHeight = dp(40)
            setPadding(dp(5), 0, dp(5), 0)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)
            ).apply { marginEnd = dp(3) }
        }
    }

    private fun roundedBackground(fill: Int, stroke: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun addTab(initial: String) {
        val view = createWebView()
        val tab = BrowserTab(view)
        tabs.add(tab)
        webHost.addView(view, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        activateTab(tabs.lastIndex)
        val destination = if (initial.isBlank()) HOME_URL else normalizeAddress(initial)
        view.loadUrl(destination)
        renderTabs()
    }

    private fun createWebView(): WebView {
        return WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = false
                loadsImagesAutomatically = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return true
                    val scheme = uri.scheme?.lowercase()
                    if (scheme == "https" || scheme == "http") return false
                    if (scheme == "mailto" || scheme == "tel") {
                        return try {
                            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                            true
                        } catch (_: Exception) {
                            true
                        }
                    }
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    val index = tabs.indexOfFirst { it.webView === view }
                    if (index >= 0) {
                        tabs[index].url = url.orEmpty()
                        if (index == activeIndex) {
                            address.setText(url.orEmpty())
                            browserProgress.visibility = View.VISIBLE
                            browserProgress.progress = 5
                        }
                        renderTabs()
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val index = tabs.indexOfFirst { it.webView === view }
                    if (index >= 0) {
                        tabs[index].url = url.orEmpty()
                        if (index == activeIndex) {
                            address.setText(url.orEmpty())
                            browserProgress.visibility = View.GONE
                            updateNavigationButtons()
                        }
                        renderTabs()
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    val index = tabs.indexOfFirst { it.webView === view }
                    if (index >= 0) {
                        tabs[index].title = title?.take(32)?.ifBlank { "Trang web" } ?: "Trang web"
                        renderTabs()
                    }
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (view === currentWeb()) {
                        browserProgress.progress = newProgress
                        browserProgress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                    }
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val uri = Uri.parse(url)
                    if (uri.scheme != "https" && uri.scheme != "http") {
                        showToast("Đường dẫn tải xuống không an toàn.")
                        return@setDownloadListener
                    }
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    val request = DownloadManager.Request(uri)
                        .setTitle(fileName)
                        .setDescription("Đang tải xuống từ trình duyệt NQV")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                        .setAllowedOverMetered(true)
                    if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
                    CookieManager.getInstance().getCookie(url)?.let {
                        request.addRequestHeader("Cookie", it)
                    }
                    getSystemService(DownloadManager::class.java).enqueue(request)
                    showToast("Đã bắt đầu tải: $fileName")
                } catch (_: Exception) {
                    showToast("Không thể tải tệp từ trang này.")
                }
            }
        }
    }

    private fun activateTab(index: Int) {
        if (index !in tabs.indices) return
        activeIndex = index
        tabs.forEachIndexed { i, tab ->
            tab.webView.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        val tab = tabs[index]
        address.setText(tab.webView.url ?: tab.url)
        updateNavigationButtons()
        renderTabs()
    }

    private fun renderTabs() {
        if (!::tabRow.isInitialized) return
        tabRow.removeAllViews()
        tabs.forEachIndexed { index, tab ->
            val chip = TextView(this).apply {
                text = (if (index == activeIndex) "● " else "") +
                    tab.title.take(17).ifBlank { "Trang mới" }
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (index == activeIndex) Color.WHITE else Color.rgb(183, 202, 224))
                setPadding(dp(13), 0, dp(13), 0)
                background = roundedBackground(
                    if (index == activeIndex) Color.rgb(47, 83, 121) else Color.rgb(20, 37, 62),
                    if (index == activeIndex) Color.rgb(107, 210, 242) else Color.rgb(65, 87, 115),
                    14
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)
                ).apply { marginEnd = dp(5) }
                setOnClickListener { activateTab(index) }
            }
            tabRow.addView(chip)
        }
    }

    private fun closeActiveTab() {
        if (activeIndex !in tabs.indices) return
        val removed = tabs.removeAt(activeIndex)
        webHost.removeView(removed.webView)
        removed.webView.stopLoading()
        removed.webView.destroy()
        if (tabs.isEmpty()) {
            addTab("")
        } else {
            activateTab((activeIndex - 1).coerceAtLeast(0).coerceAtMost(tabs.lastIndex))
        }
    }

    private fun loadAddress() {
        val tab = currentTab() ?: return
        val destination = normalizeAddress(address.text.toString())
        tab.webView.loadUrl(destination)
    }

    private fun normalizeAddress(input: String): String {
        val raw = input.trim()
        if (raw.isEmpty()) return HOME_URL
        val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(raw)
        val looksLikeDomain = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:(\\d+))?(/.*)?$").matches(raw)
        if (!hasScheme && (raw.any { it.isWhitespace() } || !looksLikeDomain)) {
            val query = URLEncoder.encode(raw, "UTF-8")
            return "https://www.google.com/search?q=$query&safe=active"
        }
        val candidate = if (hasScheme) raw else "https://$raw"
        val uri = runCatching { Uri.parse(candidate) }.getOrNull()
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            val query = URLEncoder.encode(raw, "UTF-8")
            return "https://www.google.com/search?q=$query&safe=active"
        }
        return uri.toString()
    }

    private fun updateNavigationButtons() {
        val web = currentWeb()
        backButton.isEnabled = web?.canGoBack() == true
        forwardButton.isEnabled = web?.canGoForward() == true
        backButton.alpha = if (backButton.isEnabled) 1f else .45f
        forwardButton.alpha = if (forwardButton.isEnabled) 1f else .45f
    }

    private fun currentTab(): BrowserTab? = tabs.getOrNull(activeIndex)
    private fun currentWeb(): WebView? = currentTab()?.webView
    private fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated by Android; retained for older devices")
    override fun onBackPressed() {
        val web = currentWeb()
        if (web?.canGoBack() == true) web.goBack()
        else if (tabs.size > 1) closeActiveTab()
        else finish()
    }

    override fun onDestroy() {
        tabs.forEach { tab ->
            runCatching {
                tab.webView.stopLoading()
                tab.webView.loadUrl("about:blank")
                tab.webView.destroy()
            }
        }
        tabs.clear()
        super.onDestroy()
    }
}
