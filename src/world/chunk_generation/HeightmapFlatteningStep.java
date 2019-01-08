package world.chunk_generation;

import static util.math.MathUtils.floor;
import util.math.Vec2d;
import world.landmarks.Landmark;
import world.province_generation.LandmarkStep;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;
import world.regions.RegionPos;
import world.structures.Rectangle;

public class HeightmapFlatteningStep extends GenerationStep<Chunk> {

    private static final int FLATTENNING_RADIUS = 15;

    public int[][] flattenedHeightmap = new int[CHUNK_SIZE][CHUNK_SIZE];

    public HeightmapFlatteningStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void generate() {
        double[][] totalHeight = new double[CHUNK_SIZE][CHUNK_SIZE];
        double[][] totalWeight = new double[CHUNK_SIZE][CHUNK_SIZE];
        for (RegionPos rp : world.provinceManager.getPos(region.worldCenter()).nearby(1)) {
            for (Landmark l : world.provinceManager.get(rp, LandmarkStep.class).landmarks) {
                l.flatPlots().forEach(plot -> {
                    plot = new Rectangle(plot.x - region.worldX(), plot.y - region.worldY(), plot.w, plot.h);
                    for (int x = Math.max(0, plot.x - FLATTENNING_RADIUS); x <= Math.min(CHUNK_SIZE - 1, plot.maxX() + FLATTENNING_RADIUS); x++) {
                        for (int y = Math.max(0, plot.y - FLATTENNING_RADIUS); y <= Math.min(CHUNK_SIZE - 1, plot.maxY() + FLATTENNING_RADIUS); y++) {
                            double dist = plot.distanceTo(new Vec2d(x, y));
                            double weight = dist == 0 ? 1e6 : 10 / Math.pow(dist, 2);
                            totalHeight[x][y] += world.getHeightmap(plot.centerX() + region.worldX(),
                                    plot.centerY() + region.worldY()) * weight;
                            totalWeight[x][y] += weight;
                        }
                    }
                });
            }
        }
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                flattenedHeightmap[x][y] = floor((totalHeight[x][y]
                        + world.getHeightmap(x + region.worldX(), y + region.worldY())) / (totalWeight[x][y] + 1));
            }
        }
    }
}
