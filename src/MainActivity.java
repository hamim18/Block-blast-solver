package com.blockblast.solver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;
    private Button btnClear, btnAddPiece, btnSolve, btnUndo, btnScan;
    private RecyclerView rvSelectedPieces;
    private TextView tvPieceCount;

    // Piece yang sedang di-hover (preview posisi di grid)
    private Block previewPiece = null;

    private List<Block> selectedPieces = new ArrayList<>();
    private SelectedPiecesAdapter adapter;

    private static final int REQUEST_PIECE  = 100;
    private static final int REQUEST_SOLVE  = 200;
    private static final int REQUEST_CAMERA = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView    = findViewById(R.id.gridView);
        btnClear    = findViewById(R.id.btnClear);
        btnAddPiece = findViewById(R.id.btnAddPiece);
        btnSolve    = findViewById(R.id.btnSolve);
        btnUndo     = findViewById(R.id.btnUndo);
        btnScan     = findViewById(R.id.btnScan);
        rvSelectedPieces = findViewById(R.id.rvSelectedPieces);
        tvPieceCount     = findViewById(R.id.tvPieceCount);

        adapter = new SelectedPiecesAdapter(selectedPieces, position -> {
            if (previewPiece != null && previewPiece == selectedPieces.get(position)) {
                previewPiece = null;
                gridView.clearHover();
            }
            selectedPieces.remove(position);
            adapter.notifyItemRemoved(position);
            updatePieceCount();
        });
        rvSelectedPieces.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSelectedPieces.setAdapter(adapter);

        // Preview hover saat geser jari di grid
        gridView.setOnTouchListener((v, event) -> {
            if (previewPiece != null) {
                float cellSize = (float) v.getWidth() / 8;
                int col = (int) (event.getX() / cellSize);
                int row = (int) (event.getY() / cellSize);
                gridView.setHoverPiece(previewPiece, row, col);
            }
            return false;
        });

        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivityForResult(intent, REQUEST_CAMERA);
        });

        btnClear.setOnClickListener(v -> {
            gridView.clearGrid();
            previewPiece = null;
            gridView.clearHover();
            Toast.makeText(this, "Grid cleared", Toast.LENGTH_SHORT).show();
        });

        btnUndo.setOnClickListener(v -> {
            if (!gridView.undo())
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
        });

        // Satu klik → buka selector, pilih semua piece sekaligus
        btnAddPiece.setOnClickListener(v -> {
            Intent intent = new Intent(this, PieceSelectorActivity.class);
            startActivityForResult(intent, REQUEST_PIECE);
        });

        btnSolve.setOnClickListener(v -> {
            if (selectedPieces.isEmpty()) {
                Toast.makeText(this, "Tambahkan minimal 1 piece dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            gridView.clearHover();
            previewPiece = null;

            Intent intent = new Intent(this, SolutionActivity.class);
            intent.putExtra("grid", gridToString(gridView.getGrid()));
            intent.putStringArrayListExtra("piece_names", getPieceNames());
            startActivityForResult(intent, REQUEST_SOLVE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // ── Hasil scan kamera ──────────────────────────────────────────────
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK && data != null) {
            String gridStr = data.getStringExtra("grid");
            if (gridStr != null && gridStr.length() == 64) {
                boolean[][] newGrid = new boolean[8][8];
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        newGrid[r][c] = gridStr.charAt(r * 8 + c) == '1';
                gridView.setGrid(newGrid);
                Toast.makeText(this, "Grid berhasil di-scan! Koreksi jika perlu.",
                        Toast.LENGTH_SHORT).show();
            }
        }

        // ── Hasil pilih piece (sekarang kirim array piece_names) ───────────
        if (requestCode == REQUEST_PIECE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> pieceNames = data.getStringArrayListExtra("piece_names");
            if (pieceNames == null || pieceNames.isEmpty()) return;

            // Ganti semua piece yang ada dengan pilihan baru
            selectedPieces.clear();
            previewPiece = null;
            gridView.clearHover();

            List<Block> all = BlockLibrary.getAllBlocks();
            for (String name : pieceNames) {
                for (Block b : all) {
                    if (b.getName().equals(name)) {
                        selectedPieces.add(b);
                        break;
                    }
                }
            }
            adapter.notifyDataSetChanged();
            updatePieceCount();

            // Set piece pertama sebagai aktif preview
            if (!selectedPieces.isEmpty()) {
                previewPiece = selectedPieces.get(0);
                Toast.makeText(this,
                        selectedPieces.size() + " piece dipilih. Geser jari di grid untuk preview.",
                        Toast.LENGTH_SHORT).show();
            }
        }

        // ── Setelah solve: terima grid baru, buka selector langsung ────────
        if (requestCode == REQUEST_SOLVE && resultCode == RESULT_OK && data != null) {
            String finalGridStr = data.getStringExtra("final_grid");
            if (finalGridStr != null) {
                boolean[][] newGrid = new boolean[8][8];
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++)
                        newGrid[r][c] = finalGridStr.charAt(r * 8 + c) == '1';
                gridView.setGrid(newGrid);
            }
            selectedPieces.clear();
            adapter.notifyDataSetChanged();
            previewPiece = null;
            gridView.clearHover();
            updatePieceCount();
            Toast.makeText(this, "Ronde baru! Pilih 3 piece berikutnya.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, PieceSelectorActivity.class);
            startActivityForResult(intent, REQUEST_PIECE);
        }
    }

    private void updatePieceCount() {
        tvPieceCount.setText("Pieces: " + selectedPieces.size() + "/3");
    }

    private String gridToString(boolean[][] g) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                sb.append(g[r][c] ? "1" : "0");
        return sb.toString();
    }

    private ArrayList<String> getPieceNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Block b : selectedPieces) names.add(b.getName());
        return names;
    }

    public List<Block> getSelectedPieces() {
        return selectedPieces;
    }
}
