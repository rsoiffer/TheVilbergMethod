package world.structures;

import static world.ColorScheme.LOG;
import static world.ColorScheme.PLANKS;
import static world.ColorScheme.SLATE;
import static world.ColorScheme.STONE;
import world.regions.Chunk;

public class Tower extends Structure {

    public Tower(Chunk chunk, Rectangle base) {
        super(chunk);
        priority += 20;

        int z = chunk.world.getFlattenedHeightmap(base.centerX(), base.centerY());
        int numFloors = 2;
        int floorHeight = 10;
        for (int i = 0; i <= numFloors; i++) {
            int minRoofHeight = z + i * floorHeight;
            buildFloor(base, minRoofHeight, PLANKS);
        }
        int floor = z + numFloors * floorHeight;
        int roof = floor + 8;
        buildWalls(base, z, floor + 2, STONE);
        buildCorners(base.expand(-1), z, roof, LOG);
        buildRoof(base, roof, true, 100, SLATE, STONE);
    }
}
