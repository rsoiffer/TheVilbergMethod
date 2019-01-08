package world.regions;

import java.util.Random;
import world.World;

public abstract class GenerationStep<T extends AbstractRegion> {

    public final T region;
    public final World world;
    public final Random random;

    public GenerationStep(T region) {
        this.region = region;
        world = region.world;
        random = region.random;
    }

    public abstract void cleanup();

    public abstract void generate();
}
