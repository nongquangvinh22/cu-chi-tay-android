package vn.vivogemini.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Starts one user-approved screen-capture session.
 * This activity is translucent so the app underneath remains visible.
 */
public final class CircleCaptureActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 8801;
    private static final int REQUEST_SCREEN_CAPTURE = 8802;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
            return;
        }
        requestScreenCaptureConsent();
    }

    private void requestScreenCaptureConsent() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            Toast.makeText(this, "Thiết bị không hỗ trợ chụp màn hình theo phiên.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            startActivityForResult(
                    manager.createScreenCaptureIntent(),
                    REQUEST_SCREEN_CAPTURE);
        } catch (Exception ex) {
            Toast.makeText(this, "Không mở được hộp thoại chụp màn hình: " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SCREEN_CAPTURE) return;

        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Bạn đã hủy thao tác khoanh vùng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent service = new Intent(this, CircleCaptureService.class);
        service.putExtra(CircleCaptureService.EXTRA_RESULT_CODE, resultCode);
        service.putExtra(CircleCaptureService.EXTRA_RESULT_DATA, data);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
            Toast.makeText(this,
                    "Đang chụp một ảnh màn hình. Vui lòng chờ thông báo để khoanh vùng.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Không thể bắt đầu chụp màn hình: " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQUEST_NOTIFICATIONS) return;

        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            requestScreenCaptureConsent();
        } else {
            Toast.makeText(this,
                    "Cần cho phép thông báo để nhận ảnh và chọn vùng tìm kiếm.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
