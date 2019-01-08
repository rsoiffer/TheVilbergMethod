package world.chunk_generation;

import static util.math.MathUtils.floor;
import static util.math.MathUtils.round;
import util.math.Vec2d;
import world.landmarks.Landmark;
import world.province_generation.LandmarkStep;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;
import world.regions.RegionPos;
import world.structures.Rectangle;

public class HeightmapStep extends GenerationStep<Chunk> {

    public int[][] heightmap = new int[CHUNK_SIZE][CHUNK_SIZE];

    public HeightmapStep(Chunk region) {
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
                    for (int x = Math.max(0, plot.x - 10); x <= Math.min(CHUNK_SIZE - 1, plot.maxX() + 10); x++) {
                        for (int y = Math.max(0, plot.y - 10); y <= Math.min(CHUNK_SIZE - 1, plot.maxY() + 10); y++) {
                            double dist = plot.distanceTo(new Vec2d(x, y));
                            double weight = dist == 0 ? 1e6 : 10 / Math.pow(dist, 2);
                            totalHeight[x][y] += heightRaw(plot.centerX(), plot.centerY()) * weight;
                            totalWeight[x][y] += weight;
                        }
                    }
                });
            }
        }
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                heightmap[x][y] = round((totalHeight[x][y] + heightRaw(x, y)) / (totalWeight[x][y] + 1));
            }
        }
    }

    private int heightRaw(double x, double y) {
        return floor(100 * world.getNoise("heightmap").fbm2d(x + region.worldX(), y + region.worldY(), 5, .001));
    }
}
