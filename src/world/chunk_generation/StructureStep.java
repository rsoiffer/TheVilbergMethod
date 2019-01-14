package world.chunk_generation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import util.math.MathUtils;
import static util.math.MathUtils.floor;
import util.math.Vec2d;
import world.landmarks.Landmark;
import world.province_generation.LandmarkStep;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;
import world.regions.RegionPos;
import world.structures.Rock;
import world.structures.Structure;
import world.structures.Tree;

public class StructureStep extends GenerationStep<Chunk> {

    public final List<Structure> structures = new LinkedList();

    public StructureStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generate() {
        PoissonDiskStep poisson = region.require(PoissonDiskStep.class);
        for (Vec2d v : poisson.getPoints(10)) {
            if (!world.getRivermap(floor(v.x), floor(v.y))) {
                structures.add(new Tree(region, v.x, v.y, 20 + random.nextInt(10)));
            }
        }
        int numRocks = MathUtils.poissonSample(random, CHUNK_SIZE * CHUNK_SIZE * .001);
        for (int i = 0; i < numRocks; i++) {
            double rockX = region.worldX() + random.nextDouble() * CHUNK_SIZE;
            double rockY = region.worldY() + random.nextDouble() * CHUNK_SIZE;
            if (world.getRivermap(floor(rockX), floor(rockY))
                    || random.nextDouble() < -.2 + world.getHeightmap(floor(rockX), floor(rockY)) / 200) {
                structures.add(new Rock(region, rockX, rockY, 3));
            }
        }
        for (RegionPos rp : world.provinceManager.getPos(region.worldCenter()).nearby(1)) {
            for (Landmark l : world.provinceManager.get(rp, LandmarkStep.class).landmarks) {
                for (Entry<Vec2d, Function<Chunk, Structure>> e : l.structurePlans.entrySet()) {
                    if (e.getKey().x >= region.worldX() && e.getKey().x < region.worldX() + CHUNK_SIZE
                            && e.getKey().y >= region.worldY() && e.getKey().y < region.worldY() + CHUNK_SIZE) {
                        structures.add(e.getValue().apply(region));
                    }
                }
            }
        }
    }
}
