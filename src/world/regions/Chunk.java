package world.regions;

import world.World;

public class Chunk extends AbstractRegion {

    public static final int CHUNK_SIZE = 64;

    public Chunk(World world, RegionPos pos) {
        super(world, pos);
    }

    @Override
    public int size() {
        return CHUNK_SIZE;
    }
}
