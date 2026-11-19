import {
  S3Client,
  PutObjectCommand,
  HeadObjectCommand,
  GetObjectCommand,
  DeleteObjectCommand,
  ListObjectsV2Command,
  DeleteObjectsCommand
} from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

const MAX_SINGLE_PUT = 5 * 1024 * 1024 * 1024;
const ALLOWED_EXPIRY_DAYS = [1, 7, 30];
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Access-Control-Max-Age": "86400"
};

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json; charset=utf-8" }
  });
}

function htmlResponse(html, status = 200) {
  return new Response(html, {
    status,
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff"
    }
  });
}

function getS3(env) {
  const required = ["R2_ACCOUNT_ID", "R2_ACCESS_KEY_ID", "R2_SECRET_ACCESS_KEY", "R2_BUCKET"];
  for (const key of required) {
    if (!env[key]) throw new Error("Thiếu cấu hình máy chủ: " + key);
  }
  return new S3Client({
    region: "auto",
    endpoint: "https://" + env.R2_ACCOUNT_ID + ".r2.cloudflarestorage.com",
    forcePathStyle: true,
    credentials: {
      accessKeyId: env.R2_ACCESS_KEY_ID,
      secretAccessKey: env.R2_SECRET_ACCESS_KEY
    }
  });
}

function cleanFilename(value) {
  const cleaned = String(value || "tep-tin")
    .replace(/[\\/\r\n\0]/g, "_")
    .replace(/[<>:"|?*]/g, "_")
    .trim()
    .slice(0, 180);
  return cleaned || "tep-tin";
}

function decodeFilename(value) {
  try {
    return decodeURIComponent(value || "tep-tin");
  } catch {
    return value || "tep-tin";
  }
}

function parseId(id) {
  const match = /^([0-9]{10})_([a-f0-9]{32})$/.exec(id || "");
  if (!match) return null;
  return {
    expiresAt: Number(match[1]) * 1000,
    key: "objects/" + id
  };
}

function formatSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return "Không rõ";
  if (bytes < 1024) return bytes + " B";
  const units = ["KB", "MB", "GB", "TB"];
  let value = bytes;
  let index = -1;
  do {
    value /= 1024;
    index++;
  } while (value >= 1024 && index < units.length - 1);
  return value.toFixed(2) + " " + units[index];
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

async function headSharedObject(env, id) {
  const parsed = parseId(id);
  if (!parsed) return { missing: true };
  if (Date.now() >= parsed.expiresAt) {
    const client = getS3(env);
    await client.send(new DeleteObjectCommand({ Bucket: env.R2_BUCKET, Key: parsed.key }));
    return { expired: true };
  }

  const client = getS3(env);
  try {
    const object = await client.send(new HeadObjectCommand({
      Bucket: env.R2_BUCKET,
      Key: parsed.key
    }));
    const name = decodeFilename(object.Metadata?.filename);
    return {
      id,
      key: parsed.key,
      name,
      size: Number(object.ContentLength || 0),
      contentType: object.ContentType || "application/octet-stream",
      expiresAt: new Date(parsed.expiresAt).toISOString()
    };
  } catch (error) {
    if (error?.name === "NotFound" || error?.name === "NoSuchKey" || error?.$metadata?.httpStatusCode === 404) {
      return { missing: true };
    }
    throw error;
  }
}

async function createUpload(request, env) {
  const body = await request.json().catch(() => null);
  if (!body || typeof body !== "object") {
    return jsonResponse({ error: "Dữ liệu không hợp lệ." }, 400);
  }

  const filename = cleanFilename(body.filename);
  const contentType = String(body.contentType || "application/octet-stream").slice(0, 160);
  const size = Number(body.size);
  const expiryDays = ALLOWED_EXPIRY_DAYS.includes(Number(body.expiresInDays))
    ? Number(body.expiresInDays)
    : 7;

  if (!Number.isFinite(size) || size < 0 || size > MAX_SINGLE_PUT) {
    return jsonResponse({ error: "Tệp phải có dung lượng từ 0 đến 5 GiB." }, 400);
  }

  const expiresAt = Date.now() + expiryDays * 24 * 60 * 60 * 1000;
  const id = Math.floor(expiresAt / 1000) + "_" + crypto.randomUUID().replace(/-/g, "");
  const key = "objects/" + id;
  const client = getS3(env);

  const uploadUrl = await getSignedUrl(
    client,
    new PutObjectCommand({
      Bucket: env.R2_BUCKET,
      Key: key,
      ContentType: contentType,
      Metadata: { filename: encodeURIComponent(filename) }
    }),
    { expiresIn: 900 }
  );

  const shareUrl = new URL("/s/" + id, request.url).toString();
  return jsonResponse({
    id,
    uploadUrl,
    uploadHeaders: {
      "Content-Type": contentType,
      "x-amz-meta-filename": encodeURIComponent(filename)
    },
    shareUrl,
    expiresAt: new Date(expiresAt).toISOString(),
    uploadUrlExpiresInSeconds: 900
  });
}

async function getShareInfo(env, id, request) {
  const object = await headSharedObject(env, id);
  if (object.expired) return jsonResponse({ error: "Liên kết đã hết hạn." }, 410);
  if (object.missing) return jsonResponse({ error: "Không tìm thấy tệp." }, 404);

  return jsonResponse({
    id: object.id,
    filename: object.name,
    size: object.size,
    sizeLabel: formatSize(object.size),
    contentType: object.contentType,
    expiresAt: object.expiresAt,
    shareUrl: new URL("/s/" + id, request.url).toString(),
    downloadUrl: new URL("/api/download/" + id, request.url).toString()
  });
}

async function downloadRedirect(env, id) {
  const object = await headSharedObject(env, id);
  if (object.expired) return new Response("Liên kết đã hết hạn.", { status: 410 });
  if (object.missing) return new Response("Không tìm thấy tệp.", { status: 404 });

  const client = getS3(env);
  const encodedName = encodeURIComponent(object.name);
  const disposition = "attachment; filename*=UTF-8''" + encodedName;
  const downloadUrl = await getSignedUrl(
    client,
    new GetObjectCommand({
      Bucket: env.R2_BUCKET,
      Key: object.key,
      ResponseContentDisposition: disposition,
      ResponseContentType: object.contentType
    }),
    { expiresIn: 600 }
  );
  return Response.redirect(downloadUrl, 302);
}

async function renderSharePage(env, id, request) {
  const object = await headSharedObject(env, id);
  if (object.expired) {
    return htmlResponse(pageShell(
      "Liên kết đã hết hạn",
      "<p>Liên kết chia sẻ này đã hết hạn. Hãy nhờ người gửi tạo liên kết mới.</p>"
    ), 410);
  }
  if (object.missing) {
    return htmlResponse(pageShell(
      "Không tìm thấy tệp",
      "<p>Tệp không tồn tại hoặc đã bị xóa.</p>"
    ), 404);
  }

  const safeName = escapeHtml(object.name);
  const safeSize = escapeHtml(formatSize(object.size));
  const expiry = new Date(object.expiresAt).toLocaleString("vi-VN");
  const body =
    '<div class="file"><div class="symbol">↗</div><h1>' + safeName + '</h1>' +
    '<p>' + safeSize + ' · Tệp gốc, không nén</p></div>' +
    '<a class="button" href="/api/download/' + encodeURIComponent(id) + '">Tải tệp gốc</a>' +
    '<p class="hint">Liên kết hết hạn: ' + escapeHtml(expiry) + '</p>' +
    '<p class="hint">Mở trang này trên iPhone, Android hoặc máy tính. Không cần cài ứng dụng.</p>';
  return htmlResponse(pageShell("NQV Share · Tải tệp", body));
}

function pageShell(title, body) {
  return '<!doctype html><html lang="vi"><head><meta charset="utf-8">' +
    '<meta name="viewport" content="width=device-width, initial-scale=1">' +
    '<meta name="theme-color" content="#172b68"><title>' + escapeHtml(title) + '</title>' +
    '<style>' +
    '*{box-sizing:border-box}body{margin:0;min-height:100vh;background:#f3f5fa;color:#1d2945;font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;display:grid;place-items:center;padding:22px}' +
    '.card{width:min(100%,520px);background:#fff;border:1px solid #e4e8f1;border-radius:24px;padding:28px;box-shadow:0 18px 55px #172b6814;text-align:center}' +
    '.brand{font-size:13px;letter-spacing:.16em;font-weight:800;color:#3157d5}.file{padding:28px 0 18px}.symbol{margin:auto;width:64px;height:64px;border-radius:20px;background:#edf1ff;color:#3157d5;display:grid;place-items:center;font-size:34px}.file h1{font-size:21px;overflow-wrap:anywhere;margin:18px 0 8px}.file p,.hint{font-size:13px;color:#72809a}.button{display:block;background:#3157d5;color:#fff;text-decoration:none;font-weight:700;padding:15px;border-radius:14px;margin:12px 0}.hint{line-height:1.6}.copyright{border-top:1px solid #edf0f5;margin-top:24px;padding-top:16px;font-size:12px;color:#8791a5}' +
    '</style></head><body><main class="card"><div class="brand">NQV SHARE</div>' +
    body +
    '<div class="copyright">Bản quyền Nông Quang Vinh</div></main></body></html>';
}

async function cleanupExpired(env) {
  const client = getS3(env);
  let continuationToken;
  do {
    const page = await client.send(new ListObjectsV2Command({
      Bucket: env.R2_BUCKET,
      Prefix: "objects/",
      MaxKeys: 1000,
      ContinuationToken: continuationToken
    }));
    const expired = (page.Contents || []).filter((item) => {
      const id = String(item.Key || "").slice("objects/".length);
      const parsed = parseId(id);
      return parsed && Date.now() >= parsed.expiresAt;
    }).map((item) => ({ Key: item.Key }));

    if (expired.length) {
      await client.send(new DeleteObjectsCommand({
        Bucket: env.R2_BUCKET,
        Delete: { Objects: expired, Quiet: true }
      }));
    }
    continuationToken = page.IsTruncated ? page.NextContinuationToken : undefined;
  } while (continuationToken);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    try {
      if (url.pathname === "/health" && request.method === "GET") {
        return jsonResponse({ ok: true, service: "NQV Share API", version: "0.2.0" });
      }
      if (url.pathname === "/api/create-upload" && request.method === "POST") {
        return await createUpload(request, env);
      }
      const shareMatch = /^\/api\/share\/([^/]+)$/.exec(url.pathname);
      if (shareMatch && request.method === "GET") {
        return await getShareInfo(env, shareMatch[1], request);
      }
      const downloadMatch = /^\/api\/download\/([^/]+)$/.exec(url.pathname);
      if (downloadMatch && request.method === "GET") {
        return await downloadRedirect(env, downloadMatch[1]);
      }
      const pageMatch = /^\/s\/([^/]+)$/.exec(url.pathname);
      if (pageMatch && request.method === "GET") {
        return await renderSharePage(env, pageMatch[1], request);
      }
      return jsonResponse({ error: "Không tìm thấy đường dẫn." }, 404);
    } catch (error) {
      return jsonResponse({
        error: "Máy chủ gặp lỗi khi xử lý yêu cầu.",
        detail: String(error?.message || error).slice(0, 300)
      }, 500);
    }
  },

  async scheduled(controller, env, ctx) {
    ctx.waitUntil(cleanupExpired(env));
  }
};
