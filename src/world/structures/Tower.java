package world.structures;

import world.World;
import static world.ColorScheme.STONE;
import static world.ColorScheme.LOG;
import static world.ColorScheme.PLANKS;
import static world.ColorScheme.SLATE;

public class Tower extends Structure {

    public Tower(World world, Rectangle base) {
        super(world);
        priority += 20;

        int z = world.heightmap[base.centerX()][base.centerY()];
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
