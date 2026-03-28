package com.blockblast.solver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PieceSelectorActivity extends AppCompatActivity {

    private RecyclerView rvPieces;
    private Button btnConfirm;
    private TextView tvSelected;
    private PieceLibraryAdapter adapter;

    private final List<String> selectedNames = new ArrayList<>();
    private static final int MAX_PIECES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piece_selector);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pilih 3 Piece");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvPieces   = findViewById(R.id.rvPieces);
        btnConfirm = findViewById(R.id.btnConfirmPieces);
        tvSelected = findViewById(R.id.tvSelectedCount);

        List<Block> allBlocks = BlockLibrary.getAllBlocks();

        adapter = new PieceLibraryAdapter(allBlocks, block -> {
            String name = block.getName();
            if (selectedNames.contains(name)) {
                // De-select
                selectedNames.remove(name);
            } else {
                if (selectedNames.size() >= MAX_PIECES) {
                    Toast.makeText(this,
                            "Maksimal 3 piece! Hapus salah satu dulu.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedNames.add(name);
            }
            adapter.setSelected(selectedNames);
            updateUI();
        });

        rvPieces.setLayoutManager(new GridLayoutManager(this, 3));
        rvPieces.setAdapter(adapter);

        btnConfirm.setEnabled(false);
        btnConfirm.setOnClickListener(v -> {
            if (selectedNames.isEmpty()) return;
            Intent result = new Intent();
            result.putStringArrayListExtra("piece_names", new ArrayList<>(selectedNames));
            setResult(RESULT_OK, result);
            finish();
        });

        updateUI();
    }

    private void updateUI() {
        int n = selectedNames.size();
        tvSelected.setText("Terpilih: " + n + " / " + MAX_PIECES);
        btnConfirm.setEnabled(n > 0);
        btnConfirm.setText(n == MAX_PIECES ? "✓ Konfirmasi (3/3)" : "✓ Konfirmasi (" + n + "/3)");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
