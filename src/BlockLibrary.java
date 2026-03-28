package com.blockblast.solver;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class BlockLibrary {

    public static List<Block> getAllBlocks() {
        List<Block> blocks = new ArrayList<>();

        // 1x2
        blocks.add(new Block("1x2 H", Color.parseColor("#FF9800"), new boolean[][]{
                {true, true}
        }));
        blocks.add(new Block("1x2 V", Color.parseColor("#FF9800"), new boolean[][]{
                {true}, {true}
        }));

        // 1x3
        blocks.add(new Block("1x3 H", Color.parseColor("#FFEB3B"), new boolean[][]{
                {true, true, true}
        }));
        blocks.add(new Block("1x3 V", Color.parseColor("#FFEB3B"), new boolean[][]{
                {true}, {true}, {true}
        }));

        // 1x4
        blocks.add(new Block("1x4 H", Color.parseColor("#4CAF50"), new boolean[][]{
                {true, true, true, true}
        }));
        blocks.add(new Block("1x4 V", Color.parseColor("#4CAF50"), new boolean[][]{
                {true}, {true}, {true}, {true}
        }));

        // 1x5
        blocks.add(new Block("1x5 H", Color.parseColor("#00BCD4"), new boolean[][]{
                {true, true, true, true, true}
        }));
        blocks.add(new Block("1x5 V", Color.parseColor("#00BCD4"), new boolean[][]{
                {true}, {true}, {true}, {true}, {true}
        }));

        // 2x2 Square
        blocks.add(new Block("2x2", Color.parseColor("#9C27B0"), new boolean[][]{
                {true, true}, {true, true}
        }));
        blocks.add(new Block("2x2v", Color.parseColor("#9C27B0"), new boolean[][]{
                {true, false}, {false, true}
        }));
        blocks.add(new Block("2x2h", Color.parseColor("#9C27B0"), new boolean[][]{
                {false, true}, {true, false}
        }));

        // 3x3 Square
        blocks.add(new Block("3x3", Color.parseColor("#E91E63"), new boolean[][]{
                {true, true, true}, {true, true, true}, {true, true, true}
        }));
        blocks.add(new Block("3x3v", Color.parseColor("#E91E63"), new boolean[][]{
                {true, false, false}, {false, true, false}, {false, false, true}
        }));
        blocks.add(new Block("3x3h", Color.parseColor("#E91E63"), new boolean[][]{
                {false, false, true}, {false, true, false}, {true, false, false}
        }));

        // L-shapes
        blocks.add(new Block("L", Color.parseColor("#3F51B5"), new boolean[][]{
                {true, false}, {true, false}, {true, true}
        }));
         blocks.add(new Block("L3", Color.parseColor("#3F51B5"), new boolean[][]{
                {true, true}, {true, false}, {true, false}
        }));
        blocks.add(new Block("J", Color.parseColor("#3F51B5"), new boolean[][]{
                {false, true}, {false, true}, {true, true}
        }));
        blocks.add(new Block("J3", Color.parseColor("#3F51B5"), new boolean[][]{
                {true, true}, {false, true}, {false, true}
        }));
        blocks.add(new Block("L2", Color.parseColor("#2196F3"), new boolean[][]{
                {true, true, true}, {true, false, false}
        }));
        blocks.add(new Block("L4", Color.parseColor("#2196F3"), new boolean[][]{
                {true, false, false}, {true, true, true}
        }));
        blocks.add(new Block("J2", Color.parseColor("#2196F3"), new boolean[][]{
                {true, true, true}, {false, false, true}
        }));
        blocks.add(new Block("J4", Color.parseColor("#2196F3"), new boolean[][]{
                {false, false, true}, {true, true, true}
        }));

        // T-shape
        blocks.add(new Block("T", Color.parseColor("#009688"), new boolean[][]{
                {true, true, true}, {false, true, false}
        }));
        blocks.add(new Block("T2", Color.parseColor("#009688"), new boolean[][]{
                {false, true, false}, {true, true, true}
        }));
        blocks.add(new Block("T3", Color.parseColor("#8BC34A"), new boolean[][]{
                {true, false}, {true, true}, {true, false}
        }));
        blocks.add(new Block("T4", Color.parseColor("#8BC34A"), new boolean[][]{
                {false, true}, {true, true}, {false, true}
        }));

        // S/Z shapes
        blocks.add(new Block("Sh", Color.parseColor("#FF5722"), new boolean[][]{
                {false, true, true}, {true, true, false}
        }));
        blocks.add(new Block("Sv", Color.parseColor("#FF5722"), new boolean[][]{
                {false, true}, {true, true},{true, false}
        }));
        blocks.add(new Block("Z", Color.parseColor("#FF5722"), new boolean[][]{
                {true, true, false}, {false, true, true}
        }));
        blocks.add(new Block("Zv", Color.parseColor("#FF5722"), new boolean[][]{
                {true, false}, {true, true},{false, true}
        }));

        // Corner shapes
        blocks.add(new Block("Corner 1", Color.parseColor("#795548"), new boolean[][]{
                {true, false}, {true, true}
        }));
        blocks.add(new Block("Corner 2", Color.parseColor("#795548"), new boolean[][]{
                {false, true}, {true, true}
        }));
        blocks.add(new Block("Corner 3", Color.parseColor("#607D8B"), new boolean[][]{
                {true, true}, {true, false}
        }));
        blocks.add(new Block("Corner 4", Color.parseColor("#607D8B"), new boolean[][]{
                {true, true}, {false, true}
        }));
        blocks.add(new Block("Corner 11", Color.parseColor("#795548"), new boolean[][]{
                {true, false, false}, {true, false, false}, {true, true, true}
        }));
        blocks.add(new Block("Corner 22", Color.parseColor("#795548"), new boolean[][]{
                {false, false, true}, {false, false, true}, {true, true, true}
        }));
        blocks.add(new Block("Corner 33", Color.parseColor("#607D8B"), new boolean[][]{
                {true, true, true}, {true, false, false}, {true, false, false}
        }));
        blocks.add(new Block("Corner 44", Color.parseColor("#607D8B"), new boolean[][]{
                {true, true, true}, {false, false, true}, {false, false, true}
        }));


        return blocks;
    }
}
