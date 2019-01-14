package world.structures;

import util.math.MathUtils;
import static util.math.MathUtils.floor;
import util.math.Vec3d;
import world.regions.Chunk;

public class Rock extends Structure {

    public Rock(Chunk chunk, double x, double y, double size) {
        super(chunk);
        int z = chunk.world.getFlattenedHeightmap(floor(x), floor(y)) + 1;

        double lightness = random.nextDouble() * .4 + .5;
        Vec3d color = new Vec3d(lightness, lightness, lightness);
        for (int i = 0; i < 5; i++) {
            Vec3d pos = MathUtils.randomInSphere(random).mul(size);
            double sizeMult = random.nextDouble() * .5 + .5;
            buildOval(pos.add(new Vec3d(x, y, z)), new Vec3d(1, 1, 2).mul(size * sizeMult), color);
        }
    }
}
