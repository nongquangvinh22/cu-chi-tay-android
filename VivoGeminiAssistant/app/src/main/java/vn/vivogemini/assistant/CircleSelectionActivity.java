package vn.vivogemini.assistant;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Lets the user draw a circle/freehand outline and share only that screen region. */
public final class CircleSelectionActivity extends Activity {
    private CircleSelectionView selectionView;
    private Bitmap screenBitmap;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11, 16, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 16, 32));

        String path = getIntent().getStringExtra(CircleCaptureService.EXTRA_IMAGE_PATH);
        if (path == null || path.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ảnh màn hình.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        screenBitmap = BitmapFactory.decodeFile(path);
        if (screenBitmap == null) {
            Toast.makeText(this, "Không đọc được ảnh màn hình.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 16, 32));
        root.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView title = new TextView(this);
        title.setText("Khoanh tròn đối tượng cần tìm");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView instruction = new TextView(this);
        instruction.setText("Dùng ngón tay vẽ một vòng quanh nội dung. Chỉ vùng bao quanh nét khoanh sẽ được cắt để chia sẻ.");
        instruction.setTextColor(0xFFB6C3DD);
        instruction.setTextSize(12);
        LinearLayout.LayoutParams instructionParams = new LinearLayout.LayoutParams(-1, -2);
        instructionParams.topMargin = dp(4);
        instructionParams.bottomMargin = dp(8);
        root.addView(instruction, instructionParams);

        selectionView = new CircleSelectionView();
        root.addView(selectionView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        Button clear = new Button(this);
        clear.setText("Xóa nét khoanh");
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(50), 1));
        clear.setOnClickListener(v -> selectionView.clearSelection());

        Button search = new Button(this);
        search.setText("Tìm vùng đã khoanh");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        searchParams.leftMargin = dp(8);
        actions.addView(search, searchParams);
        search.setOnClickListener(v -> shareSelectedRegion());

        TextView footer = new TextView(this);
        footer.setText("Bản quyền của Nông Quang Vinh");
        footer.setTextColor(0xFF8393B3);
        footer.setTextSize(10);
        footer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(4);
        root.addView(footer, footerParams);

        setContentView(root);
    }

    private void shareSelectedRegion() {
        RectF bounds = selectionView.getSelectionBoundsOnBitmap();
        if (bounds == null) {
            Toast.makeText(this, "Hãy vẽ một vòng quanh đối tượng trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        int left = Math.max(0, (int) Math.floor(bounds.left));
        int top = Math.max(0, (int) Math.floor(bounds.top));
        int right = Math.min(screenBitmap.getWidth(), (int) Math.ceil(bounds.right));
        int bottom = Math.min(screenBitmap.getHeight(), (int) Math.ceil(bounds.bottom));
        if (right - left < 35 || bottom - top < 35) {
            Toast.makeText(this, "Vùng chọn quá nhỏ. Hãy khoanh rộng hơn.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap crop = Bitmap.createBitmap(screenBitmap, left, top, right - left, bottom - top);
        File directory = new File(getCacheDir(), "circle-search");
        if (!directory.exists() && !directory.mkdirs()) {
            crop.recycle();
            Toast.makeText(this, "Không tạo được tệp ảnh tạm.", Toast.LENGTH_LONG).show();
            return;
        }

        File output = new File(directory, "selection.jpg");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!crop.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw new IllegalStateException("Không lưu được vùng đã chọn.");
            }
            stream.flush();
        } catch (Exception ex) {
            crop.recycle();
            Toast.makeText(this, "Lỗi lưu vùng chọn: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        crop.recycle();

        try {
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", output);
            android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
            send.setType("image/jpeg");
            send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            send.setClipData(android.content.ClipData.newUri(getContentResolver(), "Vùng đã khoanh", uri));
            send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(send, "Tìm bằng Google Lens hoặc ứng dụng khác"));
        } catch (Exception ex) {
            Toast.makeText(this, "Không mở được chia sẻ ảnh: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (screenBitmap != null && !screenBitmap.isRecycled()) {
            screenBitmap.recycle();
            screenBitmap = null;
        }
        super.onDestroy();
    }

    private final class CircleSelectionView extends View {
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final List<PointF> points = new ArrayList<>();
        private final RectF imageRect = new RectF();
        private float scale = 1f;
        private float offsetX;
        private float offsetY;
        private boolean drawing;

        CircleSelectionView() {
            super(CircleSelectionActivity.this);
            setBackgroundColor(Color.BLACK);
            pathPaint.setColor(0xFF66B5FF);
            pathPaint.setStyle(Paint.Style.STROKE);
            pathPaint.setStrokeWidth(dp(4));
            pathPaint.setStrokeCap(Paint.Cap.ROUND);
            pathPaint.setStrokeJoin(Paint.Join.ROUND);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            fitImage(w, h);
        }

        private void fitImage(int w, int h) {
            if (screenBitmap == null || w <= 0 || h <= 0) return;
            scale = Math.min(w / (float) screenBitmap.getWidth(),
                    h / (float) screenBitmap.getHeight());
            float shownW = screenBitmap.getWidth() * scale;
            float shownH = screenBitmap.getHeight() * scale;
            offsetX = (w - shownW) / 2f;
            offsetY = (h - shownH) / 2f;
            imageRect.set(offsetX, offsetY, offsetX + shownW, offsetY + shownH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (screenBitmap == null) return;
            if (imageRect.isEmpty()) fitImage(getWidth(), getHeight());
            canvas.drawBitmap(screenBitmap, null, imageRect, imagePaint);
            if (!path.isEmpty()) canvas.drawPath(path, pathPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!imageRect.contains(x, y)) return false;
                    clearSelection();
                    drawing = true;
                    points.add(new PointF(x, y));
                    path.moveTo(x, y);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!drawing) return false;
                    PointF last = points.get(points.size() - 1);
                    if (Math.hypot(x - last.x, y - last.y) >= dp(2)) {
                        points.add(new PointF(x, y));
                        path.lineTo(x, y);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!drawing) return false;
                    drawing = false;
                    if (points.size() >= 3) path.close();
                    invalidate();
                    performClick();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        void clearSelection() {
            drawing = false;
            points.clear();
            path.reset();
            invalidate();
        }

        RectF getSelectionBoundsOnBitmap() {
            if (points.size() < 8 || scale <= 0f) return null;
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (PointF point : points) {
                float bx = (point.x - offsetX) / scale;
                float by = (point.y - offsetY) / scale;
                minX = Math.min(minX, bx);
                minY = Math.min(minY, by);
                maxX = Math.max(maxX, bx);
                maxY = Math.max(maxY, by);
            }

            float padX = Math.max(8f, (maxX - minX) * 0.06f);
            float padY = Math.max(8f, (maxY - minY) * 0.06f);
            RectF result = new RectF(
                    Math.max(0f, minX - padX),
                    Math.max(0f, minY - padY),
                    Math.min(screenBitmap.getWidth(), maxX + padX),
                    Math.min(screenBitmap.getHeight(), maxY + padY));
            return result;
        }
    }
}
