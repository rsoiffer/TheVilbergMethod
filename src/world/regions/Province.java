package world.regions;

import world.World;

public class Province extends AbstractRegion {

    public static int PROVINCE_SIZE = 1024;

    public Province(World world, RegionPos pos) {
        super(world, pos);
    }

    @Override
    public int size() {
        return PROVINCE_SIZE;
    }
}
