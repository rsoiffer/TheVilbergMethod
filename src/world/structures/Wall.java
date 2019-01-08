package world.structures;

import static util.math.MathUtils.mod;
import static world.ColorScheme.PLANKS;
import static world.ColorScheme.STONE;
import world.regions.Chunk;

public class Wall extends Structure {

    public Wall(Chunk chunk, Rectangle base, int height, boolean horizontal) {
        super(chunk);
        priority += 20;

        if (horizontal) {
            for (int i = base.x; i <= base.maxX(); i++) {
                for (int j = base.y; j <= base.maxY(); j++) {
                    int z = chunk.world.getFlattenedHeightmap(i, base.centerY()) + height;
                    blocks.setRange(i, j, chunk.world.getFlattenedHeightmap(i, j) + 1, z, STONE);
                    if (j > base.y && j < base.maxY()) {
                        blocks.set(i, j, z, PLANKS);
                    } else {
                        blocks.setRange(i, j, z + 1, z + 1 + mod(i, 2), STONE);
                    }
                }
            }
        } else {
            for (int i = base.x; i <= base.maxX(); i++) {
                for (int j = base.y; j <= base.maxY(); j++) {
                    int z = chunk.world.getFlattenedHeightmap(base.centerX(), j) + height;
                    blocks.setRange(i, j, chunk.world.getFlattenedHeightmap(i, j) + 1, z, STONE);
                    if (i > base.x && i < base.maxX()) {
                        blocks.set(i, j, z, PLANKS);
                    } else {
                        blocks.setRange(i, j, z + 1, z + 1 + mod(j, 2), STONE);
                    }
                }
            }
        }
    }
}
