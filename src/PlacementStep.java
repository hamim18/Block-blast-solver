package com.blockblast.solver;

public class PlacementStep {
    public final Block block;
    public final int row;
    public final int col;
    public final int blockIndex;

    public PlacementStep(Block block, int blockIndex, int row, int col) {
        this.block = block;
        this.blockIndex = blockIndex;
        this.row = row;
        this.col = col;
    }
}
