package com.blockblast.solver;

import java.util.Arrays;

public class Block {
    private String name;
    private int color;
    private boolean[][] shape;

    public Block(String name, int color, boolean[][] shape) {
        this.name = name;
        this.color = color;
        this.shape = shape;
    }

    public String getName() { return name; }
    public int getColor() { return color; }
    public boolean[][] getShape() { return shape; }

    public int getRows() { return shape.length; }
    public int getCols() { return shape[0].length; }

    public Block copy() {
        boolean[][] newShape = new boolean[shape.length][shape[0].length];
        for (int i = 0; i < shape.length; i++)
            newShape[i] = Arrays.copyOf(shape[i], shape[i].length);
        return new Block(name, color, newShape);
    }
}
