package vn.nongquangvinh.nqvshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

private val Navy = Color(0xFF172554)
private val Blue = Color(0xFF3157D5)
private val Purple = Color(0xFF7655D9)
private val Teal = Color(0xFF159C9A)
private val Page = Color(0xFFF4F6FC)
private val Muted = Color(0xFF69758E)
private const val MAX_FILE_SIZE = 5L * 1024L * 1024L * 1024L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NQVShareApp() }
    }
}

private data class LocalFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
)

private data class SharedFile(
    val fileName: String,
    val fileSize: Long,
    val url: String,
    val expiresInHours: Int
)

private enum class ShareMode(val title: String) {
    LINK("Liên kết"),
    QR("Mã QR"),
    OTHER_APP("Ứng dụng khác")
}

@Composable
private fun NQVShareApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember {
        context.getSharedPreferences("nqv_share_settings", Context.MODE_PRIVATE)
    }

    var serverUrl by remember {
        mutableStateOf(preferences.getString("server_url", "") ?: "")
    }
    var uploadKey by remember {
        mutableStateOf(preferences.getString("upload_key", "") ?: "")
    }
    var selectedFiles by remember { mutableStateOf(emptyList<LocalFile>()) }
    var sharedFiles by remember { mutableStateOf(emptyList<SharedFile>()) }
    var shareMode by remember { mutableStateOf(ShareMode.LINK) }
    var isUploading by remember { mutableStateOf(false) }
    var isCheckingServer by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentFileName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Nhập địa chỉ máy chủ và mã kết nối riêng để bắt đầu chia sẻ.") }
    var statusIsError by remember { mutableStateOf(false) }
    var qrItem by remember { mutableStateOf<SharedFile?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        selectedFiles = uris.mapNotNull { readLocalFile(context, it) }
        statusIsError = false
        status = if (selectedFiles.isEmpty()) {
            "Chưa chọn được tệp nào."
        } else {
            "Đã chọn " + selectedFiles.size + " tệp. Bạn có thể tải lên khi máy chủ đã kết nối."
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            secondary = Purple,
            tertiary = Teal,
            background = Page,
            surface = Color.White,
            onSurface = Navy
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFEAF0FF), Page, Color(0xFFF8F5FF))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HeaderCard()

                SectionCard(
                    title = "1. Kết nối máy chủ",
                    subtitle = "Dán địa chỉ HTTPS của máy chủ NQV Share và nhập mã kết nối riêng."
                ) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Địa chỉ máy chủ Cloudflare Worker") },
                        placeholder = { Text("https://ten-may-chu.ts.net") }
                    )
                    OutlinedTextField(
                        value = uploadKey,
                        onValueChange = { uploadKey = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Mã kết nối riêng của máy chủ") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val cleaned = normalizeServerUrl(serverUrl)
                                if (cleaned == null || uploadKey.isBlank()) {
                                    status = "Hãy nhập địa chỉ HTTPS hợp lệ và mã kết nối riêng."
                                    statusIsError = true
                                } else {
                                    serverUrl = cleaned
                                    preferences.edit()
                                        .putString("server_url", cleaned)
                                        .putString("upload_key", uploadKey)
                                        .apply()
                                    status = "Đã lưu địa chỉ và mã kết nối trên điện thoại."
                                    statusIsError = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            Text("Lưu địa chỉ")
                        }
                        OutlinedButton(
                            onClick = {
                                val cleaned = normalizeServerUrl(serverUrl)
                                if (cleaned == null || uploadKey.isBlank()) {
                                    status = "Hãy nhập URL HTTPS hợp lệ và mã kết nối riêng."
                                    statusIsError = true
                                } else {
                                    serverUrl = cleaned
                                    preferences.edit()
                                        .putString("server_url", cleaned)
                                        .putString("upload_key", uploadKey)
                                        .apply()
                                    scope.launch {
                                        isCheckingServer = true
                                        try {
                                            status = checkWorker(cleaned)
                                            statusIsError = false
                                        } catch (error: Exception) {
                                            status = "Không kết nối được Worker: " +
                                                (error.message ?: "Lỗi không xác định")
                                            statusIsError = true
                                        } finally {
                                            isCheckingServer = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isCheckingServer && !isUploading
                        ) {
                            Text(if (isCheckingServer) "Đang kiểm tra…" else "Kiểm tra")
                        }
                    }
                    Text(
                        text = "Mã kết nối được lưu riêng trong dữ liệu ứng dụng và chỉ dùng để cho phép tải lên.",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }

                SectionCard(
                    title = "2. Chọn tệp cần gửi",
                    subtitle = "Chọn ảnh, video hoặc tài liệu. Tệp gốc được giữ nguyên, không nén."
                ) {
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("＋  Chọn tệp từ điện thoại")
                    }

                    if (selectedFiles.isEmpty()) {
                        Text("Chưa có tệp nào được chọn.", color = Muted, fontSize = 13.sp)
                    } else {
                        selectedFiles.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F5FF), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📄", fontSize = 20.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(formatBytes(file.size), color = Muted, fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = {
                                        selectedFiles = selectedFiles.filterIndexed { i, _ -> i != index }
                                    },
                                    enabled = !isUploading
                                ) {
                                    Text("Xóa", color = Color(0xFFB54755))
                                }
                            }
                        }
                        TextButton(
                            onClick = { selectedFiles = emptyList() },
                            enabled = !isUploading
                        ) {
                            Text("Xóa toàn bộ danh sách")
                        }
                    }
                }

                SectionCard(
                    title = "3. Thời hạn liên kết",
                    subtitle = "Liên kết sẽ tự hết hạn và tệp được dọn khỏi kho lưu trữ."
                ) {
                    Text(
                        "Thời hạn được cố định ở 24 giờ kể từ khi tải lên thành công.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }

                SectionCard(
                    title = "4. Tải lên và tạo liên kết",
                    subtitle = "Mỗi tệp sẽ có một liên kết tải riêng và một mã QR tương ứng."
                ) {
                    Button(
                        onClick = {
                            val base = normalizeServerUrl(serverUrl)
                            when {
                                base == null -> {
                                    status = "Hãy nhập và lưu địa chỉ HTTPS của Worker trước."
                                    statusIsError = true
                                }
                                uploadKey.isBlank() -> {
                                    status = "Hãy nhập mã kết nối riêng của máy chủ trước."
                                    statusIsError = true
                                }
                                selectedFiles.isEmpty() -> {
                                    status = "Bạn chưa chọn tệp để tải lên."
                                    statusIsError = true
                                }
                                else -> {
                                    serverUrl = base
                                    preferences.edit()
                                        .putString("server_url", base)
                                        .putString("upload_key", uploadKey)
                                        .apply()
                                    scope.launch {
                                        isUploading = true
                                        sharedFiles = emptyList()
                                        val failures = mutableListOf<String>()
                                        var successes = 0
                                        try {
                                            selectedFiles.forEach { file ->
                                                currentFileName = file.name
                                                progress = 0f
                                                try {
                                                    val shared = uploadFile(
                                                        context = context,
                                                        file = file,
                                                        baseUrl = base,
                                                        uploadKey = uploadKey,
                                                        onProgress = { value -> progress = value / 100f }
                                                    )
                                                    sharedFiles = sharedFiles + shared
                                                    successes++
                                                } catch (error: Exception) {
                                                    failures.add(
                                                        file.name + ": " +
                                                            (error.message ?: "Lỗi không xác định")
                                                    )
                                                }
                                            }
                                            if (failures.isEmpty()) {
                                                status = "Hoàn tất! Đã tải lên " + successes +
                                                    " tệp và tạo liên kết thành công."
                                                statusIsError = false
                                            } else {
                                                status = "Đã tải lên thành công " + successes +
                                                    "/" + selectedFiles.size + " tệp. Lỗi: " +
                                                    failures.joinToString(" | ")
                                                statusIsError = successes == 0
                                            }
                                        } finally {
                                            isUploading = false
                                            currentFileName = ""
                                            progress = 0f
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading && selectedFiles.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Text(if (isUploading) "Đang tải lên…" else "TẢI LÊN VÀ TẠO LINK")
                    }

                    if (isUploading) {
                        Text(
                            text = currentFileName,
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Teal
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.0f%%", progress * 100f),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (statusIsError) Color(0xFFFFF0F0) else Color(0xFFEAF8F3),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = status,
                            color = if (statusIsError) Color(0xFF9C3038) else Color(0xFF23634B),
                            fontSize = 13.sp
                        )
                    }
                }

                if (sharedFiles.isNotEmpty()) {
                    SectionCard(
                        title = "5. Chọn cách chia sẻ",
                        subtitle = "Chọn phương thức bạn muốn dùng cho các liên kết vừa tạo."
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ShareMode.values().forEach { mode ->
                                FilterChip(
                                    selected = shareMode == mode,
                                    onClick = { shareMode = mode },
                                    label = { Text(mode.title, fontSize = 12.sp) }
                                )
                            }
                        }
                        sharedFiles.forEach { item ->
                            SharedFileCard(
                                item = item,
                                mode = shareMode,
                                onCopy = {
                                    copyLink(context, item.url)
                                },
                                onShowQr = { qrItem = item },
                                onShare = { shareLink(context, item.fileName, item.url) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "NQV SHARE  •  Chia sẻ tệp gốc đa nền tảng",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Bản quyền Nông Quang Vinh",
                        color = Navy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    qrItem?.let { item ->
        Dialog(onDismissRequest = { qrItem = null }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("MÃ QR CHIA SẺ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
                    val bitmap = remember(item.url) { makeQrBitmap(item.url) }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Mã QR NQV Share",
                        modifier = Modifier.size(240.dp)
                    )
                    Text(
                        item.fileName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Liên kết có hiệu lực " + item.expiresInHours + " giờ",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            copyLink(context, item.url)
                            qrItem = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sao chép liên kết")
                    }
                    TextButton(onClick = { qrItem = null }) {
                        Text("Đóng")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF172554), Color(0xFF3157D5), Color(0xFF7655D9))
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "BẢN QUYỀN NÔNG QUANG VINH",
                    color = Color(0xFFDCE5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                )
                Text(
                    "NQV SHARE",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Chia sẻ ảnh, video và tài liệu",
                    color = Color(0xFFE4E9FF),
                    fontSize = 15.sp
                )
                Text(
                    "ANDROID   ↔   IPHONE   ↔   WINDOWS",
                    color = Color(0xFFC8D4FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Navy)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = Muted)
            }
            content()
        }
    }
}

@Composable
private fun SharedFileCard(
    item: SharedFile,
    mode: ShareMode,
    onCopy: () -> Unit,
    onShowQr: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F7FF))
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                item.fileName,
                fontWeight = FontWeight.Bold,
                color = Navy,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatBytes(item.fileSize) + "  •  Hết hạn sau " + item.expiresInHours + " giờ",
                fontSize = 11.sp,
                color = Muted
            )
            when (mode) {
                ShareMode.LINK -> {
                    Text(
                        item.url,
                        color = Blue,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = onCopy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) {
                        Text("Sao chép liên kết")
                    }
                }
                ShareMode.QR -> {
                    Button(
                        onClick = onShowQr,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("Hiển thị mã QR")
                    }
                }
                ShareMode.OTHER_APP -> {
                    Button(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Text("Chia sẻ qua ứng dụng khác")
                    }
                }
            }
        }
    }
}

private fun normalizeServerUrl(input: String): String? {
    val value = input.trim().trimEnd('/')
    if (value.isBlank()) return null
    val uri = Uri.parse(value)
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return null
    if (uri.query != null || uri.fragment != null) return null
    return value
}

private fun readLocalFile(context: Context, uri: Uri): LocalFile? {
    var name = "tep-tin"
    var size = -1L
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return LocalFile(
        uri = uri,
        name = name,
        mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
        size = size
    )
}

private suspend fun checkWorker(baseUrl: String): String = withContext(Dispatchers.IO) {
    val connection = URL(baseUrl + "/health").openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            throw IllegalStateException("HTTP " + code + ": " + body.take(250))
        }
        val json = JSONObject(body)
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException("Worker đã phản hồi nhưng trạng thái không hợp lệ.")
        }
        "Kết nối thành công! NQV Share API đang hoạt động."
    } finally {
        connection.disconnect()
    }
}

private suspend fun uploadFile(
    context: Context,
    file: LocalFile,
    baseUrl: String,
    uploadKey: String,
    onProgress: (Int) -> Unit
): SharedFile = withContext(Dispatchers.IO) {
    var cachedFile: File? = null
    try {
        if (file.size < 0L) {
            cachedFile = copyUnknownSizeToCache(context, file.uri)
        }
        val uploadSize = cachedFile?.length() ?: file.size
        if (uploadSize < 0L || uploadSize > MAX_FILE_SIZE) {
            throw IllegalStateException("Tệp vượt quá giới hạn 5 GiB hoặc không đọc được dung lượng.")
        }

        val createConnection = URL(baseUrl + "/api/create-upload")
            .openConnection() as HttpURLConnection
        createConnection.requestMethod = "POST"
        createConnection.connectTimeout = 30000
        createConnection.readTimeout = 30000
        createConnection.doOutput = true
        createConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        createConnection.setRequestProperty("X-NQV-Key", uploadKey)

        val payload = JSONObject()
            .put("filename", file.name)
            .put("contentType", file.mimeType)
            .put("size", uploadSize)

        val createResponse: JSONObject
        try {
            createConnection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                output.flush()
            }
            val code = createConnection.responseCode
            val stream = if (code in 200..299) createConnection.inputStream else createConnection.errorStream
            val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val message = runCatching { JSONObject(responseText).optString("detail").ifBlank { JSONObject(responseText).optString("error") } }
                    .getOrDefault("")
                throw IllegalStateException(
                    "Worker trả về HTTP " + code + ": " +
                        (message.ifBlank { responseText.take(250) })
                )
            }
            createResponse = JSONObject(responseText)
        } finally {
            createConnection.disconnect()
        }

        val uploadUrl = createResponse.getString("uploadUrl")
        val headers = createResponse.getJSONObject("uploadHeaders")
        val uploadConnection = URL(uploadUrl).openConnection() as HttpURLConnection
        uploadConnection.requestMethod = "PUT"
        uploadConnection.connectTimeout = 60000
        uploadConnection.readTimeout = 120000
        uploadConnection.doOutput = true
        uploadConnection.setFixedLengthStreamingMode(uploadSize)

        val headerIterator = headers.keys()
        while (headerIterator.hasNext()) {
            val key = headerIterator.next()
            uploadConnection.setRequestProperty(key, headers.getString(key))
        }

        try {
            val source = if (cachedFile != null) {
                FileInputStream(cachedFile)
            } else {
                context.contentResolver.openInputStream(file.uri)
                    ?: throw IllegalStateException("Không mở được tệp nguồn.")
            }
            source.use { input ->
                uploadConnection.outputStream.use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var sent = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        sent += count
                        val percent = if (uploadSize == 0L) 100
                            else ((sent * 100L) / uploadSize).toInt().coerceIn(0, 100)
                        withContext(Dispatchers.Main) { onProgress(percent) }
                    }
                    output.flush()
                    if (sent != uploadSize) {
                        throw IllegalStateException(
                            "Dung lượng tệp thay đổi trong khi tải lên (" +
                                sent + "/" + uploadSize + " byte)."
                        )
                    }
                }
            }

            val uploadCode = uploadConnection.responseCode
            if (uploadCode !in 200..299) {
                val details = uploadConnection.errorStream
                    ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
                throw IllegalStateException(
                    "Kho lưu trữ trả về HTTP " + uploadCode + ". " + details.take(250)
                )
            }
            withContext(Dispatchers.Main) { onProgress(100) }
        } finally {
            uploadConnection.disconnect()
        }

        SharedFile(
            fileName = file.name,
            fileSize = uploadSize,
            url = createResponse.getString("shareUrl"),
            expiresInHours = 24
        )
    } finally {
        cachedFile?.delete()
    }
}

private fun copyUnknownSizeToCache(context: Context, uri: Uri): File {
    val temp = File.createTempFile("nqvshare-", ".upload", context.cacheDir)
    var complete = false
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Không đọc được tệp đã chọn.")
        input.use { source ->
            FileOutputStream(temp).use { destination ->
                val buffer = ByteArray(256 * 1024)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_FILE_SIZE) {
                        throw IllegalStateException("Tệp vượt quá giới hạn 5 GiB.")
                    }
                    destination.write(buffer, 0, count)
                }
                destination.flush()
            }
        }
        complete = true
        return temp
    } finally {
        if (!complete) temp.delete()
    }
}

private fun copyLink(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("NQV Share", link))
    Toast.makeText(context, "Đã sao chép liên kết", Toast.LENGTH_SHORT).show()
}

private fun shareLink(context: Context, fileName: String, link: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TEXT, link)
    }
    context.startActivity(Intent.createChooser(intent, "Chọn ứng dụng để chia sẻ"))
}

private fun makeQrBitmap(content: String): Bitmap {
    val dimension = 640
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, dimension, dimension)
    val bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
    for (x in 0 until dimension) {
        for (y in 0 until dimension) {
            bitmap.setPixel(
                x,
                y,
                if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
        }
    }
    return bitmap
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0L) return "Đang xác định dung lượng"
    if (bytes == 0L) return "0 B"
    if (bytes < 1024L) return bytes.toString() + " B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    do {
        value /= 1024.0
        unit++
    } while (value >= 1024.0 && unit < units.lastIndex)
    return String.format(Locale.getDefault(), "%.2f %s", value, units[unit])
}
