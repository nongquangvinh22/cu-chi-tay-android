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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

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

private data class ShareLink(
    val fileName: String,
    val fileSize: Long,
    val url: String
)

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
    var selectedFiles by remember { mutableStateOf(emptyList<LocalFile>()) }
    var shareLinks by remember { mutableStateOf(emptyList<ShareLink>()) }
    var isUploading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentFileName by remember { mutableStateOf("") }
    var status by remember {
        mutableStateOf("Chọn tệp để tạo liên kết chia sẻ.")
    }
    var qrLink by remember { mutableStateOf<ShareLink?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedFiles = uris.mapNotNull { readLocalFile(context, it) }
        status = if (selectedFiles.isEmpty()) {
            "Chưa chọn tệp nào."
        } else {
            "Đã chọn " + selectedFiles.size + " tệp."
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF3157D5),
            onPrimary = Color.White,
            secondary = Color(0xFF5C6BC0),
            background = Color(0xFFF4F6FB),
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4F6FB)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF172B68))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "NQV SHARE",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gửi tệp gốc bằng liên kết và mã QR",
                            color = Color(0xFFDCE5FF),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Android  →  iPhone  →  Windows",
                            color = Color(0xFFB8C9FF),
                            fontSize = 12.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = "1. Kết nối máy chủ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Địa chỉ máy chủ Cloudflare Worker") },
                            placeholder = { Text("https://nqv-share-api.ten-ban.workers.dev") }
                        )
                        Button(
                            onClick = {
                                val cleaned = serverUrl.trim().trimEnd('/')
                                serverUrl = cleaned
                                preferences.edit().putString("server_url", cleaned).apply()
                                status = if (cleaned.startsWith("https://")) {
                                    "Đã lưu địa chỉ máy chủ."
                                } else {
                                    "Hãy nhập địa chỉ HTTPS của Worker."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lưu địa chỉ máy chủ")
                        }
                        Text(
                            text = "Bạn cần triển khai máy chủ theo hướng dẫn trong repository trước khi tải tệp lên.",
                            color = Color(0xFF68728A),
                            fontSize = 12.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "2. Chọn ảnh, video hoặc tài liệu",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading
                        ) {
                            Text("Chọn tệp từ điện thoại")
                        }
                        if (selectedFiles.isEmpty()) {
                            Text(
                                text = "Bạn có thể chọn nhiều tệp trong một lần.",
                                color = Color(0xFF68728A),
                                fontSize = 13.sp
                            )
                        } else {
                            selectedFiles.forEach { file ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = file.name,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = formatBytes(file.size),
                                        color = Color(0xFF68728A),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            TextButton(
                                onClick = { selectedFiles = emptyList() },
                                enabled = !isUploading
                            ) {
                                Text("Bỏ danh sách đã chọn")
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "3. Tải lên và tạo liên kết",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = {
                                val base = serverUrl.trim().trimEnd('/')
                                if (!base.startsWith("https://")) {
                                    status = "Hãy lưu địa chỉ HTTPS của máy chủ trước."
                                } else if (selectedFiles.isEmpty()) {
                                    status = "Bạn chưa chọn tệp."
                                } else {
                                    scope.launch {
                                        isUploading = true
                                        shareLinks = emptyList()
                                        try {
                                            selectedFiles.forEach { file ->
                                                currentFileName = file.name
                                                progress = 0f
                                                val created = uploadFile(
                                                    context = context,
                                                    file = file,
                                                    baseUrl = base,
                                                    onProgress = { value -> progress = value / 100f }
                                                )
                                                shareLinks = shareLinks + created
                                            }
                                            status = "Hoàn tất! Mỗi tệp đã có một liên kết riêng."
                                        } catch (error: Exception) {
                                            status = "Tải lên chưa hoàn tất: " +
                                                (error.message ?: "Lỗi không xác định")
                                        } finally {
                                            isUploading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading && selectedFiles.isNotEmpty()
                        ) {
                            Text(if (isUploading) "Đang tải lên..." else "Tải lên và tạo link")
                        }
                        if (isUploading) {
                            Text(
                                text = currentFileName,
                                color = Color(0xFF68728A),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.0f%%", progress * 100f),
                                color = Color(0xFF68728A),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = status,
                            color = Color(0xFF33415C),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Không nén, không đổi độ phân giải và không chuyển mã tệp.",
                            color = Color(0xFF24734A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (shareLinks.isNotEmpty()) {
                    Text(
                        text = "Liên kết đã tạo",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    shareLinks.forEach { link ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = link.fileName,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatBytes(link.fileSize),
                                    color = Color(0xFF68728A),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = link.url,
                                    color = Color(0xFF3157D5),
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    TextButton(onClick = {
                                        val clipboard = context.getSystemService(
                                            Context.CLIPBOARD_SERVICE
                                        ) as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("NQV Share", link.url)
                                        )
                                        Toast.makeText(context, "Đã sao chép liên kết", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Sao chép")
                                    }
                                    TextButton(onClick = { qrLink = link }) {
                                        Text("Mã QR")
                                    }
                                    TextButton(onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, link.url)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, "Chia sẻ liên kết")
                                        )
                                    }) {
                                        Text("Chia sẻ")
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFDCE1EC))
                Text(
                    text = "Liên kết mặc định hết hạn sau 7 ngày. Tệp được lưu trên Cloudflare R2.",
                    color = Color(0xFF68728A),
                    fontSize = 11.sp
                )
                Text(
                    text = "© Bản quyền của Nông Quang Vinh",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF68728A),
                    fontSize = 12.sp
                )
            }
        }
    }

    qrLink?.let { item ->
        Dialog(onDismissRequest = { qrLink = null }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Quét mã để tải tệp", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val qrBitmap = remember(item.url) { makeQrBitmap(item.url) }
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Mã QR NQV Share",
                        modifier = Modifier.size(240.dp)
                    )
                    Text(
                        text = item.fileName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                    Text(
                        text = item.url,
                        color = Color(0xFF3157D5),
                        fontSize = 11.sp
                    )
                    Button(onClick = { qrLink = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Đóng")
                    }
                }
            }
        }
    }
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

private suspend fun uploadFile(
    context: Context,
    file: LocalFile,
    baseUrl: String,
    onProgress: (Int) -> Unit
): ShareLink = withContext(Dispatchers.IO) {
    if (file.size <= 0L) {
        throw IllegalStateException("Không đọc được dung lượng tệp: " + file.name)
    }
    if (file.size > 5L * 1024L * 1024L * 1024L) {
        throw IllegalStateException("Tệp vượt quá giới hạn 5 GiB của một lần tải lên.")
    }

    val createConnection = (URL(baseUrl + "/api/create-upload").openConnection() as HttpURLConnection)
    createConnection.requestMethod = "POST"
    createConnection.connectTimeout = 30000
    createConnection.readTimeout = 30000
    createConnection.doOutput = true
    createConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

    val payload = JSONObject()
        .put("filename", file.name)
        .put("contentType", file.mimeType)
        .put("size", file.size)
        .put("expiresInDays", 7)

    val createResponse: JSONObject
    try {
        createConnection.outputStream.use { output ->
            output.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
        }
        val code = createConnection.responseCode
        val responseText = if (code in 200..299) {
            createConnection.inputStream.bufferedReader().use { it.readText() }
        } else {
            createConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP " + code
        }
        if (code !in 200..299) throw IllegalStateException("Máy chủ: " + responseText)
        createResponse = JSONObject(responseText)
    } finally {
        createConnection.disconnect()
    }

    val uploadUrl = createResponse.getString("uploadUrl")
    val headers = createResponse.getJSONObject("uploadHeaders")
    val uploadConnection = (URL(uploadUrl).openConnection() as HttpURLConnection)
    uploadConnection.requestMethod = "PUT"
    uploadConnection.connectTimeout = 60000
    uploadConnection.readTimeout = 60000
    uploadConnection.doOutput = true
    uploadConnection.setFixedLengthStreamingMode(file.size)

    val headerIterator = headers.keys()
    while (headerIterator.hasNext()) {
        val key = headerIterator.next()
        uploadConnection.setRequestProperty(key, headers.getString(key))
    }

    try {
        val input = context.contentResolver.openInputStream(file.uri)
            ?: throw IllegalStateException("Không mở được tệp nguồn.")
        input.use { source ->
            uploadConnection.outputStream.use { destination ->
                val buffer = ByteArray(256 * 1024)
                var sent = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    destination.write(buffer, 0, count)
                    sent += count
                    val percent = ((sent * 100L) / file.size).toInt().coerceIn(0, 100)
                    withContext(Dispatchers.Main) { onProgress(percent) }
                }
                destination.flush()
            }
        }

        val uploadCode = uploadConnection.responseCode
        if (uploadCode !in 200..299) {
            val details = uploadConnection.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("Kho lưu trữ trả về HTTP " + uploadCode + ". " + (details ?: ""))
        }
        withContext(Dispatchers.Main) { onProgress(100) }
    } finally {
        uploadConnection.disconnect()
    }

    ShareLink(
        fileName = file.name,
        fileSize = file.size,
        url = createResponse.getString("shareUrl")
    )
}

private fun makeQrBitmap(content: String): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 600, 600)
    val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)
    for (x in 0 until 600) {
        for (y in 0 until 600) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0L) return "Không rõ dung lượng"
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
