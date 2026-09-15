package hgds.epicgrief;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

final class TreeCutter {

    private static final int MAX_BLOCKS = 256;
    private static final int[][] NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private TreeCutter() {
    }

    static List<Block> collectTreeBlocks(Block origin) {
        List<Block> blocks = new ArrayList<>();
        if (!isWood(origin.getType())) {
            return blocks;
        }

        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && blocks.size() < MAX_BLOCKS) {
            Block current = queue.poll();
            blocks.add(current);

            for (int[] offset : NEIGHBORS) {
                Block neighbor = current.getRelative(offset[0], offset[1], offset[2]);
                if (visited.contains(neighbor) || !isWood(neighbor.getType())) {
                    continue;
                }

                visited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return blocks;
    }

    static void breakTree(List<Block> blocks) {
        for (Block block : blocks) {
            block.setType(Material.AIR, false);
        }
    }

    static boolean isWood(Material material) {
        if (Tag.LOGS.isTagged(material)) {
            return true;
        }

        String name = material.name();
        return name.endsWith("_WOOD") || name.startsWith("STRIPPED_") && name.endsWith("_WOOD");
    }
}
