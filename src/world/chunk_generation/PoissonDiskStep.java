package world.chunk_generation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import util.math.MathUtils;
import util.math.Vec2d;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;
import world.regions.RegionPos;

public class PoissonDiskStep extends GenerationStep<Chunk> {

    private static final double DENSITY = .01;

    private final TreeMap<Double, Vec2d> points = new TreeMap();

    public PoissonDiskStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generate() {
        int num = MathUtils.poissonSample(random, DENSITY * CHUNK_SIZE * CHUNK_SIZE);
        for (int i = 0; i < num; i++) {
            points.put(random.nextDouble(), new Vec2d(region.pos.x + random.nextDouble(),
                    region.pos.y + random.nextDouble()).mul(CHUNK_SIZE));
        }
    }

    public List<Vec2d> getPoints(double minDist) {
        List<Vec2d> r = new LinkedList();
        for (Entry<Double, Vec2d> e : points.entrySet()) {
            boolean keepPoint = true;
            for (RegionPos cp : region.pos.nearby(1)) {
                for (Entry<Double, Vec2d> e2 : world.chunkManager.get(cp, PoissonDiskStep.class).points.entrySet()) {
                    if (e2.getKey() >= e.getKey()) {
                        break;
                    }
                    if (e2.getValue().sub(e.getValue()).lengthSquared() < minDist * minDist) {
                        keepPoint = false;
                        break;
                    }
                }
                if (!keepPoint) {
                    break;
                }
            }
            if (keepPoint) {
                r.add(e.getValue());
            }
        }
        return r;
    }
}
