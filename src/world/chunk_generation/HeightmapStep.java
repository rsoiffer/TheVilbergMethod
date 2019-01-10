package world.chunk_generation;

import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;

public class HeightmapStep extends GenerationStep<Chunk> {

    public double[][] heightmap = new double[CHUNK_SIZE][CHUNK_SIZE];
    public boolean[][] rivermap = new boolean[CHUNK_SIZE][CHUNK_SIZE];

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
                double dryness = Math.pow(2 * fbm("riverness", x, y, 2, .001) - 1, 2);
                double altitude = (dryness + .2) * Math.pow(fbm("altitude", x, y, 5, .001), 3) + .05;
                heightmap[x][y] = 300 * altitude * 1;//fbm("heightmap", x, y, 3, .005);
                heightmap[x][y] += 50 * Math.pow(altitude, 3) * fbm("heightmapDetail", x, y, 3, .05);
                if (.3 > fbm("isElevated", x, y, 2, .002)) {
                    heightmap[x][y] += Math.max(0, -10 + 50 * Math.pow(dryness, .2) * fbm("elevated", x, y, 2, .005));
                }
                if (rivermap[x][y] = dryness < .001) {
                    double c = fbm("riverCliffs", x, y, 2, .01);
                    heightmap[x][y] -= c * 4;
                }
            }
        }
    }

    private double fbm(String name, int x, int y, int octaves, double frequency) {
        return world.getNoise(name).fbm2d(x + region.worldX(), y + region.worldY(), octaves, frequency);
    }
}
