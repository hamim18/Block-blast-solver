package com.blockblast.solver;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PieceLibraryAdapter extends RecyclerView.Adapter<PieceLibraryAdapter.VH> {

    public interface OnPieceClickListener {
        void onClick(Block block);
    }

    private final List<Block> blocks;
    private final OnPieceClickListener listener;
    private List<String> selectedNames = new java.util.ArrayList<>();

    public PieceLibraryAdapter(List<Block> blocks, OnPieceClickListener listener) {
        this.blocks   = blocks;
        this.listener = listener;
    }

    public void setSelected(List<String> names) {
        this.selectedNames = names;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_piece, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Block block = blocks.get(position);
        holder.pieceView.setBlock(block);
        holder.tvName.setText(block.getName());

        boolean selected = selectedNames.contains(block.getName());

        // Visual: background terang + border saat terpilih
        if (selected) {
            holder.itemView.setBackgroundColor(Color.argb(200, 137, 180, 250)); // biru muda
            holder.tvBadge.setVisibility(View.VISIBLE);
            int idx = selectedNames.indexOf(block.getName()) + 1;
            holder.tvBadge.setText(String.valueOf(idx)); // nomor urut pilihan
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#1E1E2E"));
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(block));
    }

    @Override
    public int getItemCount() { return blocks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        PieceView pieceView;
        TextView  tvName;
        TextView  tvBadge; // nomor urut (1/2/3)

        VH(View v) {
            super(v);
            pieceView = v.findViewById(R.id.pieceView);
            tvName    = v.findViewById(R.id.tvPieceName);
            tvBadge   = v.findViewById(R.id.tvPieceBadge);
        }
    }
}
