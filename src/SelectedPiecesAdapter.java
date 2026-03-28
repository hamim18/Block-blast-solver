package com.blockblast.solver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedPiecesAdapter extends RecyclerView.Adapter<SelectedPiecesAdapter.VH> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private List<Block> blocks;
    private OnRemoveListener removeListener;

    public SelectedPiecesAdapter(List<Block> blocks, OnRemoveListener removeListener) {
        this.blocks = blocks;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_piece, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Block block = blocks.get(position);
        holder.pieceView.setBlock(block);
        holder.tvName.setText(block.getName());
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) removeListener.onRemove(pos);
        });
    }

    @Override
    public int getItemCount() { return blocks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        PieceView pieceView;
        TextView tvName;
        ImageButton btnRemove;

        VH(View v) {
            super(v);
            pieceView = v.findViewById(R.id.selectedPieceView);
            tvName = v.findViewById(R.id.tvSelectedPieceName);
            btnRemove = v.findViewById(R.id.btnRemovePiece);
        }
    }
}
