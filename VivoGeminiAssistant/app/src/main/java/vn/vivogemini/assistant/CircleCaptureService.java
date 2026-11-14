package vn.vivogemini.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Captures exactly one frame after explicit system consent, then stops.
 * It does not keep a screen-recording session running in the background.
 */
public final class CircleCaptureService extends Service {
    static final String EXTRA_RESULT_CODE = "circle_result_code";
    static final String EXTRA_RESULT_DATA = "circle_result_data";
    static final String EXTRA_IMAGE_PATH = "circle_image_path";

    private static final String CHANNEL_CAPTURE = "circle_capture";
    private static final String CHANNEL_RESULT = "circle_result";
    private static final int FOREGROUND_ID = 8803;
    private static final int RESULT_ID = 8804;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private boolean captureArmed;
    private boolean frameTaken;
    private boolean resourcesReleased;

    private final MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            handler.post(() -> {
                releaseProjectionResources(false);
                stopForeground(true);
                stopSelf();
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, RESULT_CANCELED);
        Intent resultData;
        if (Build.VERSION.SDK_INT >= 33) {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        } else {
            //noinspection deprecation
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        }

        if (resultCode != ActivityResult.RESULT_OK || resultData == null) {
            fail("Thiếu xác nhận chụp màn hình. Hãy thử lại.");
            return START_NOT_STICKY;
        }

        try {
            startCaptureForeground();
            beginOneFrameCapture(resultCode, resultData);
        } catch (SecurityException ex) {
            fail("Android đã từ chối phiên chụp màn hình: " + ex.getMessage());
        } catch (Exception ex) {
            fail("Không chụp được màn hình: " + ex.getMessage());
        }
        return START_NOT_STICKY;
    }

    private void startCaptureForeground() {
        Notification notification = new Notification.Builder(this, CHANNEL_CAPTURE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Vinh Circle")
                .setContentText("Đang chụp một ảnh theo yêu cầu của bạn")
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }
    }

    private void beginOneFrameCapture(int resultCode, Intent resultData) {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (manager == null) throw new IllegalStateException("Không tìm thấy MediaProjectionManager.");

        // The user-approved token is consumed for this one capture session only.
        projection = manager.getMediaProjection(resultCode, resultData);
        if (projection == null) throw new IllegalStateException("Không tạo được phiên chụp màn hình.");

        WindowManager windowManager = getSystemService(WindowManager.class);
        Display display = windowManager == null ? null : windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        if (display != null) {
            display.getRealMetrics(metrics);
        }
        if (metrics.widthPixels <= 0 || metrics.heightPixels <= 0) {
            metrics = getResources().getDisplayMetrics();
        }

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;
        if (width <= 0 || height <= 0 || density <= 0) {
            throw new IllegalStateException("Không đọc được kích thước màn hình.");
        }

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        projection.registerCallback(projectionCallback, handler);
        imageReader.setOnImageAvailableListener(this::onImageAvailable, handler);

        virtualDisplay = projection.createVirtualDisplay(
                "VinhCircle-OneShot",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler);

        if (virtualDisplay == null) {
            throw new IllegalStateException("Không tạo được màn hình ảo để chụp.");
        }

        // The translucent consent Activity has to disappear before taking the frame.
        handler.postDelayed(() -> captureArmed = true, 900);
        handler.postDelayed(() -> {
            if (!frameTaken) fail("Không nhận được khung hình màn hình. Hãy thử lại.");
        }, 6000);
    }

    private void onImageAvailable(ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null) return;
            if (!captureArmed || frameTaken) {
                image.close();
                return;
            }
            frameTaken = true;
            captureArmed = false;
            final Image selectedFrame = image;
            image = null;
            new Thread(() -> saveFrame(selectedFrame), "VinhCircle-Capture").start();
        } catch (Exception ex) {
            if (image != null) image.close();
            fail("Không đọc được ảnh màn hình: " + ex.getMessage());
        }
    }

    private void saveFrame(Image image) {
        File output = null;
        String error = null;
        Bitmap padded = null;
        Bitmap cropped = null;
        try {
            Image.Plane[] planes = image.getPlanes();
            if (planes == null || planes.length == 0) {
                throw new IllegalStateException("Ảnh màn hình không có dữ liệu.");
            }

            Image.Plane plane = planes[0];
            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int paddedWidth = rowStride / pixelStride;
            int width = image.getWidth();
            int height = image.getHeight();

            padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
            buffer.rewind();
            padded.copyPixelsFromBuffer(buffer);
            cropped = Bitmap.createBitmap(padded, 0, 0, width, height);

            File directory = new File(getCacheDir(), "circle-search");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("Không tạo được thư mục tạm.");
            }
            File[] oldFiles = directory.listFiles();
            if (oldFiles != null) {
                for (File old : oldFiles) {
                    if (old.isFile()) old.delete();
                }
            }

            output = new File(directory, "screen.jpg");
            try (FileOutputStream stream = new FileOutputStream(output)) {
                if (!cropped.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                    throw new IllegalStateException("Không ghi được ảnh chụp.");
                }
                stream.flush();
            }
        } catch (Exception ex) {
            error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        } finally {
            image.close();
            if (cropped != null && cropped != padded) cropped.recycle();
            if (padded != null) padded.recycle();
        }

        final File saved = output;
        final String failure = error;
        handler.post(() -> {
            if (failure != null || saved == null || !saved.exists()) {
                fail("Không lưu được ảnh màn hình: " + failure);
                return;
            }
            showSelectionNotification(saved);
            releaseProjectionResources(true);
            stopForeground(true);
            stopSelf();
        });
    }

    private void showSelectionNotification(File imageFile) {
        Intent open = new Intent(this, CircleSelectionActivity.class);
        open.putExtra(EXTRA_IMAGE_PATH, imageFile.getAbsolutePath());
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(
                this, 8805, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, CHANNEL_RESULT)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Đã chụp màn hình")
                .setContentText("Chạm vào đây để khoanh vùng cần tìm")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(RESULT_ID, notification);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel capture = new NotificationChannel(
                CHANNEL_CAPTURE, "Chụp màn hình một lần", NotificationManager.IMPORTANCE_LOW);
        capture.setDescription("Hiển thị khi bạn chủ động chụp một ảnh màn hình để khoanh vùng.");
        manager.createNotificationChannel(capture);

        NotificationChannel result = new NotificationChannel(
                CHANNEL_RESULT, "Kết quả khoanh vùng", NotificationManager.IMPORTANCE_HIGH);
        result.setDescription("Mở ảnh đã chụp để bạn tự khoanh và chia sẻ vùng tìm kiếm.");
        manager.createNotificationChannel(result);
    }

    private void fail(String message) {
        handler.post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            releaseProjectionResources(true);
            stopForeground(true);
            stopSelf();
        });
    }

    private void releaseProjectionResources(boolean stopProjection) {
        if (resourcesReleased) return;
        resourcesReleased = true;

        if (imageReader != null) {
            try {
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
            } catch (Exception ignored) {}
            imageReader = null;
        }

        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (Exception ignored) {}
            virtualDisplay = null;
        }

        if (projection != null) {
            try {
                projection.unregisterCallback(projectionCallback);
                if (stopProjection) projection.stop();
            } catch (Exception ignored) {}
            projection = null;
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseProjectionResources(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Avoids shadowing Activity.RESULT_OK in the Service class. */
    private static final class ActivityResult {
        static final int RESULT_OK = -1;
    }
}
