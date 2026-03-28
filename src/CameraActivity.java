package com.blockblast.solver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.ExifInterface;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {

    private static final int PERM_CAMERA = 101;

    // ── State mesin ────────────────────────────────────────────────────────
    private enum State { PREVIEW, PICK_CORNER1, PICK_CORNER2, CONFIRM }
    private State state = State.PREVIEW;

    // ── Views ──────────────────────────────────────────────────────────────
    private TextureView  textureView;
    private OverlayView  overlayView;
    private ImageView    ivCaptured;      // tampilkan foto setelah capture
    private CropOverlay  cropOverlay;     // overlay tap 2 sudut
    private Button       btnScan, btnConfirm, btnRetry;
    private TextView     tvStatus;
    private GridView     previewGrid;
    private View         confirmPanel;

    // ── Camera2 ────────────────────────────────────────────────────────────
    private CameraDevice         cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader          imageReader;
    private HandlerThread        bgThread;
    private Handler              bgHandler;
    private Size                 previewSize;

    // ── Data ───────────────────────────────────────────────────────────────
    private Bitmap        capturedBitmap;
    private byte[]        lastJpegBytes;   // simpan untuk baca Exif
    private PointF        corner1, corner2;
    private boolean[][]   detectedGrid;

    // ══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView  = findViewById(R.id.textureView);
        overlayView  = findViewById(R.id.overlayView);
        ivCaptured   = findViewById(R.id.ivCaptured);
        cropOverlay  = findViewById(R.id.cropOverlay);
        btnScan      = findViewById(R.id.btnScan);
        btnConfirm   = findViewById(R.id.btnConfirm);
        btnRetry     = findViewById(R.id.btnRetry);
        tvStatus     = findViewById(R.id.tvStatus);
        previewGrid  = findViewById(R.id.previewGrid);
        confirmPanel = findViewById(R.id.confirmPanel);

        previewGrid.setInteractive(true);
        confirmPanel.setVisibility(View.GONE);
        ivCaptured.setVisibility(View.GONE);
        cropOverlay.setVisibility(View.GONE);

        // ── Tombol Scan: ambil foto ────────────────────────────────────────
        btnScan.setOnClickListener(v -> capturePhoto());

        // ── Tap di foto: pilih sudut ───────────────────────────────────────
        cropOverlay.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

            // Konversi koordinat view → koordinat bitmap
            float bx = event.getX() / ivCaptured.getWidth()  * capturedBitmap.getWidth();
            float by = event.getY() / ivCaptured.getHeight() * capturedBitmap.getHeight();

            if (state == State.PICK_CORNER1) {
                corner1 = new PointF(bx, by);
                cropOverlay.setCorners(toViewPoint(corner1), null,
                        ivCaptured.getWidth(), ivCaptured.getHeight(),
                        capturedBitmap.getWidth(), capturedBitmap.getHeight());
                state = State.PICK_CORNER2;
                tvStatus.setText("Sekarang tap sudut KANAN BAWAH grid");

            } else if (state == State.PICK_CORNER2) {
                corner2 = new PointF(bx, by);
                cropOverlay.setCorners(toViewPoint(corner1), toViewPoint(corner2),
                        ivCaptured.getWidth(), ivCaptured.getHeight(),
                        capturedBitmap.getWidth(), capturedBitmap.getHeight());
                processDetection();
            }
            return true;
        });

        // ── Konfirmasi hasil ───────────────────────────────────────────────
        btnConfirm.setOnClickListener(v -> {
            if (detectedGrid == null) return;
            Intent result = new Intent();
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++)
                    sb.append(detectedGrid[r][c] ? "1" : "0");
            result.putExtra("grid", sb.toString());
            setResult(RESULT_OK, result);
            finish();
        });

        // ── Ulangi: kembali ke preview kamera ─────────────────────────────
        btnRetry.setOnClickListener(v -> resetToPreview());

        textureView.setSurfaceTextureListener(surfaceTextureListener);
        checkCameraPermission();
    }

    // ── Proses deteksi setelah 2 sudut dipilih ─────────────────────────────
    private void processDetection() {
        state = State.CONFIRM;
        tvStatus.setText("Memproses…");
        cropOverlay.setVisibility(View.GONE);

        new Thread(() -> {
            int left   = (int) Math.min(corner1.x, corner2.x);
            int top    = (int) Math.min(corner1.y, corner2.y);
            int right  = (int) Math.max(corner1.x, corner2.x);
            int bottom = (int) Math.max(corner1.y, corner2.y);
            int w = right - left, h = bottom - top;
            if (w < 10 || h < 10) {
                runOnUiThread(() -> tvStatus.setText("Area terlalu kecil, coba lagi"));
                state = State.PICK_CORNER1;
                return;
            }

            Bitmap cropped = Bitmap.createBitmap(capturedBitmap, left, top, w, h);
            GridDetector.DetectionResult res = GridDetector.detect(cropped);
            cropped.recycle();

            runOnUiThread(() -> {
                if (res.success) {
                    detectedGrid = res.grid;
                    previewGrid.setGrid(detectedGrid);
                    confirmPanel.setVisibility(View.VISIBLE);
                    ivCaptured.setVisibility(View.GONE);
                    tvStatus.setText("Deteksi selesai! Koreksi jika perlu, lalu Konfirmasi");
                } else {
                    tvStatus.setText("Gagal: " + res.message + ". Tap lagi untuk pilih ulang.");
                    state = State.PICK_CORNER1;
                    cropOverlay.setVisibility(View.VISIBLE);
                    cropOverlay.reset();
                }
            });
        }).start();
    }

    // ── Reset ke mode preview kamera ──────────────────────────────────────
    private void resetToPreview() {
        state = State.PREVIEW;
        confirmPanel.setVisibility(View.GONE);
        ivCaptured.setVisibility(View.GONE);
        cropOverlay.setVisibility(View.GONE);
        overlayView.setVisibility(View.VISIBLE);
        btnScan.setVisibility(View.VISIBLE);
        tvStatus.setText("Arahkan kamera ke grid Block Blast, lalu tekan Scan");
        detectedGrid = null;
        corner1 = corner2 = null;
        if (capturedBitmap != null) { capturedBitmap.recycle(); capturedBitmap = null; }
        cropOverlay.reset();
    }

    // ── Konversi koordinat bitmap → koordinat view ivCaptured ─────────────
    private PointF toViewPoint(PointF bitmapPt) {
        float vx = bitmapPt.x / capturedBitmap.getWidth()  * ivCaptured.getWidth();
        float vy = bitmapPt.y / capturedBitmap.getHeight() * ivCaptured.getHeight();
        return new PointF(vx, vy);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Camera2
    // ══════════════════════════════════════════════════════════════════════

    @Override protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) openCamera();
    }

    @Override protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERM_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PERM_CAMERA && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (textureView.isAvailable()) openCamera();
        } else {
            Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_LONG).show();
        }
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(@NonNull SurfaceTexture st, int w, int h) { openCamera(); }
                @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture st, int w, int h) {}
                @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture st) { return true; }
                @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture st) {}
            };

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) return;
        CameraManager mgr = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String camId = null;
            for (String id : mgr.getCameraIdList()) {
                CameraCharacteristics ch = mgr.getCameraCharacteristics(id);
                Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { camId = id; break; }
            }
            if (camId == null) return;
            CameraCharacteristics chars = mgr.getCameraCharacteristics(camId);
            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return;
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class));
            Size captureSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG));
            imageReader = ImageReader.newInstance(captureSize.getWidth(), captureSize.getHeight(), ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(onImageAvailable, bgHandler);
            mgr.openCamera(camId, stateCallback, bgHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(@NonNull CameraDevice cam) { cameraDevice = cam; createPreviewSession(); }
        @Override public void onDisconnected(@NonNull CameraDevice cam) { cam.close(); cameraDevice = null; }
        @Override public void onError(@NonNull CameraDevice cam, int err) { cam.close(); cameraDevice = null; }
    };

    private void createPreviewSession() {
        try {
            SurfaceTexture st = textureView.getSurfaceTexture();
            if (st == null) return;
            st.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(st);
            CaptureRequest.Builder b = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            b.addTarget(surface);
            b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(@NonNull CameraCaptureSession s) {
                            captureSession = s;
                            try { s.setRepeatingRequest(b.build(), null, bgHandler);
                                runOnUiThread(() -> tvStatus.setText("Arahkan kamera ke grid Block Blast, lalu tekan Scan"));
                            } catch (CameraAccessException e) { e.printStackTrace(); }
                        }
                        @Override public void onConfigureFailed(@NonNull CameraCaptureSession s) {}
                    }, bgHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void capturePhoto() {
        if (cameraDevice == null || captureSession == null) return;
        try {
            CaptureRequest.Builder b = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            b.addTarget(imageReader.getSurface());
            b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            b.set(CaptureRequest.JPEG_ORIENTATION, getDeviceRotation());
            captureSession.capture(b.build(), null, bgHandler);
            runOnUiThread(() -> tvStatus.setText("Foto diambil…"));
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private final ImageReader.OnImageAvailableListener onImageAvailable = reader -> {
        Image image = reader.acquireLatestImage();
        if (image == null) return;
        ByteBuffer buf = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        image.close();
        lastJpegBytes = bytes;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        bmp = correctRotation(bmp, bytes);
        final Bitmap finalBmp = bmp;
        runOnUiThread(() -> {
            if (capturedBitmap != null) capturedBitmap.recycle();
            capturedBitmap = finalBmp;
            // Tampilkan foto, sembunyikan preview kamera
            ivCaptured.setImageBitmap(capturedBitmap);
            ivCaptured.setVisibility(View.VISIBLE);
            overlayView.setVisibility(View.GONE);
            btnScan.setVisibility(View.GONE);
            cropOverlay.setVisibility(View.VISIBLE);
            cropOverlay.reset();
            state = State.PICK_CORNER1;
            tvStatus.setText("Tap sudut KIRI ATAS grid di foto");
        });
    };

    // ── Helpers ───────────────────────────────────────────────────────────
    private Size chooseOptimalSize(Size[] sizes) {
        Size best = sizes[0]; long bestDiff = Long.MAX_VALUE;
        for (Size s : sizes) {
            long diff = Math.abs((long) s.getWidth() * s.getHeight() - 1920L * 1080);
            if (diff < bestDiff) { bestDiff = diff; best = s; }
        }
        return best;
    }

    private int getDeviceRotation() {
        int rot = getWindowManager().getDefaultDisplay().getRotation();
        switch (rot) {
            case Surface.ROTATION_90:  return 0;
            case Surface.ROTATION_180: return 270;
            case Surface.ROTATION_270: return 180;
            default: return 90;
        }
    }

    private Bitmap correctRotation(Bitmap bmp, byte[] jpegBytes) {
        int degrees = 0;
        try {
            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(jpegBytes));
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  degrees = 90;  break;
                case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (degrees == 0) return bmp;
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        bmp.recycle();
        return rotated;
    }

    private void closeCamera() {
        if (captureSession != null) { captureSession.close(); captureSession = null; }
        if (cameraDevice  != null) { cameraDevice.close();  cameraDevice  = null; }
        if (imageReader   != null) { imageReader.close();   imageReader   = null; }
    }

    private void startBackgroundThread() {
        bgThread = new HandlerThread("CameraBG"); bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (bgThread != null) { bgThread.quitSafely();
            try { bgThread.join(); } catch (InterruptedException ignored) {}
            bgThread = null; bgHandler = null; }
    }
}
