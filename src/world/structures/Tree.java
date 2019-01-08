package world.structures;

import static util.math.MathUtils.floor;
import static util.math.MathUtils.round;
import util.math.Vec3d;
import static world.ColorScheme.LEAF_FUNC;
import static world.ColorScheme.LOG;
import world.regions.Chunk;

public class Tree extends Structure {

    public Tree(Chunk chunk, double x, double y, double height) {
        super(chunk);
        int z = chunk.world.getFlattenedHeightmap(floor(x), floor(y)) + 1;

        Vec3d scale = new Vec3d(1, 1, random.nextDouble() > .5 ? 1 : .5);
        double redness = Math.pow(random.nextDouble(), 2);
        Vec3d leaf = LEAF_FUNC.apply(redness);

        if (random.nextDouble() < .5) {
            height *= 1.5;
            for (int k = 0; k < 4; k++) {
                double phase = random.nextDouble() * 2 * Math.PI;
                for (int i = 0; i < 20; i++) {
                    double branchZ = z + 5 + random.nextDouble() * (height + 5);
                    double angle = phase + branchZ * 1.2;
                    double dist = 5 * (1 - (branchZ - 5 - z) / height) + random.nextDouble();
                    constructOval(new Vec3d(x + dist * Math.cos(angle), y + dist * Math.sin(angle), branchZ), new Vec3d(dist, dist, .3), leaf);
                }
            }
        } else {
            constructOval(new Vec3d(x, y, z + (int) height), scale.mul(2), leaf);
            int numOvals = 15 + random.nextInt(5);
            for (int i = 0; i < numOvals; i++) {
                Vec3d v = new Vec3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
                v = v.mul(10).sub(5).mul(height / 20).mul(scale).add(new Vec3d(0, 0, height));
                double size = (random.nextDouble() * 4 + 3) * height / 20;
                constructOval(v.add(new Vec3d(x, y, z)), scale.mul(size), leaf);
            }
        }

        for (int i = round(x - 1); i <= round(x); i++) {
            for (int j = round(y - 1); j <= round(y); j++) {
                blocks.setRange(i, j, z - 3, z + (int) height, LOG);
            }
        }
    }

    public void constructOval(Vec3d pos, Vec3d size, Vec3d leaf) {
        for (int x = floor(pos.x - size.x); x < pos.x + size.x; x++) {
            for (int y = floor(pos.y - size.y); y < pos.y + size.y; y++) {
                double distSquared = pos.sub(new Vec3d(x, y, pos.z)).div(size).lengthSquared();
                if (distSquared < 1) {
                    double z = Math.sqrt(1 - distSquared) * size.z;
                    blocks.setRange(x, y, round(pos.z - z), round(pos.z + z), leaf);
                }
            }
        }
    }
}
