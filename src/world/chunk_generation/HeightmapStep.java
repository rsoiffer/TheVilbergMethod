package world.chunk_generation;

import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;

public class HeightmapStep extends GenerationStep<Chunk> {

    public double[][] heightmap = new double[CHUNK_SIZE][CHUNK_SIZE];
    public double[][] rivermap = new double[CHUNK_SIZE][CHUNK_SIZE];

    public HeightmapStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void generate() {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                heightmap[x][y] = 100 * world.getNoise("heightmap").fbm2d(x + region.worldX(), y + region.worldY(), 5, .001);
                boolean isElevated = .4 > world.getNoise("isElevated").fbm2d(x + region.worldX(), y + region.worldY(), 2, .002);
                if (isElevated) {
                    heightmap[x][y] += Math.max(0,
                            -20 + 50 * world.getNoise("elevated").fbm2d(x + region.worldX(), y + region.worldY(), 2, .005));
                }
            }
        }
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                double d = 2 * world.getNoise("rivermap").fbm2d(x + region.worldX(), y + region.worldY(), 2, .003) - 1;
                rivermap[x][y] = 1;//d * d;
                if (rivermap[x][y] < .005) {
                    heightmap[x][y] = 0;
                } else {
                    double c = world.getNoise("canyons").fbm2d(x + region.worldX(), y + region.worldY(), 2, .01);
                    heightmap[x][y] *= Math.pow(rivermap[x][y], c + .1);
                }
            }
        }
    }
}
