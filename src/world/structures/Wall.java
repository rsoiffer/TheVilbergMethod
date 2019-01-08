package world.structures;

import world.World;
import static world.ColorScheme.STONE;
import static world.ColorScheme.PLANKS;

public class Wall extends Structure {

    public Wall(World world, Rectangle base, int height, boolean horizontal) {
        super(world);
        priority += 20;

        if (horizontal) {
            for (int i = base.x; i <= base.maxX(); i++) {
                for (int j = base.y; j <= base.maxY(); j++) {
                    int z = world.heightmap[i][base.centerY()] + height;
                    blocks.setRange(i, j, world.heightmap[i][j] + 1, z, STONE);
                    if (j > base.y && j < base.maxY()) {
                        blocks.set(i, j, z, PLANKS);
                    } else {
                        blocks.setRange(i, j, z + 1, z + 1 + i % 2, STONE);
                    }
                }
            }
        } else {
            for (int i = base.x; i <= base.maxX(); i++) {
                for (int j = base.y; j <= base.maxY(); j++) {
                    int z = world.heightmap[base.centerX()][j] + height;
                    blocks.setRange(i, j, world.heightmap[i][j] + 1, z, STONE);
                    if (i > base.x && i < base.maxX()) {
                        blocks.set(i, j, z, PLANKS);
                    } else {
                        blocks.setRange(i, j, z + 1, z + 1 + j % 2, STONE);
                    }
                }
            }
        }
    }
}
