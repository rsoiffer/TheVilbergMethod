package world.chunk_generation;

import util.Noise;
import util.math.Vec3d;
import util.rlestorage.IntConverter.Vec3dConverter;
import util.rlestorage.RLEArrayStorage;
import util.rlestorage.RLEStorage;
import static world.ColorScheme.LAND_FUNC;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;
import world.regions.RegionPos;
import world.structures.Structure;

public class ConstructionStep extends GenerationStep<Chunk> {

    public RLEStorage<Vec3d> blocks = new RLEArrayStorage(CHUNK_SIZE, new Vec3dConverter());

    public ConstructionStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generate() {
        Noise colorR = world.getNoise("colorR");
        Noise colorG = world.getNoise("colorG");
        Noise colorB = world.getNoise("colorB");

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                int worldX = x + region.worldX(), worldY = y + region.worldY();
                int z = world.getFlattenedHeightmap(worldX, worldY);
                if (world.getRivermap(worldX, worldY) < .005) {
                    blocks.setRangeInfinite(x, y, z, new Vec3d(.3, .6, 1));
                } else {
                    Vec3d colorNoise = new Vec3d(colorR.fbm2d(worldX, worldY, 2, .01), colorG.fbm2d(worldX, worldY, 2, .01), colorB.fbm2d(worldX, worldY, 2, .01));
                    blocks.setRangeInfinite(x, y, z, LAND_FUNC.apply(colorNoise));
                    blocks.setRangeInfinite(x, y, z - 1, new Vec3d(1, .6, .2));
                }
            }
        }

        for (RegionPos cp : region.pos.nearby(1)) {
            for (Structure s : world.chunkManager.get(cp, StructureValidationStep.class).validStructures) {
                s.blocks.copyTo(blocks, -region.worldX(), -region.worldY(), 0);
            }
        }
//        List<Structure> built = new LinkedList();
//        for (Structure s1 : structures) {
//            if (!built.stream().anyMatch(s2 -> s1.intersects(s2))) {
//                s1.blocks.copyTo(worldBlocks, 0, 0, 0);
//                built.add(s1);
//            }
//        }
    }
}
