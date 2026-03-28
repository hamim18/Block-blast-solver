package com.blockblast.solver;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity — hanya launcher kecil yang:
 * 1. Minta izin SYSTEM_ALERT_WINDOW (overlay)
 * 2. Start FloatingWindowService
 * 3. Langsung minimize dirinya sendiri (biar overlay kelihatan)
 *
 * Semua logika solver sekarang ada di FloatingWindowService.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart  = findViewById(R.id.btnStartOverlay);
        Button btnStop   = findViewById(R.id.btnStopOverlay);
        TextView tvInfo  = findViewById(R.id.tvOverlayInfo);

        btnStart.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(this)) {
                // Minta izin overlay
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_OVERLAY);
            } else {
                startOverlay();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingWindowService.class));
            tvInfo.setText("Overlay tidak aktif");
            Toast.makeText(this, "Overlay dihentikan", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && Settings.canDrawOverlays(this)) {
                startOverlay();
            } else {
                Toast.makeText(this, "Izin overlay diperlukan!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startOverlay() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        // Pindah ke home supaya overlay terlihat di atas game
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }
}
