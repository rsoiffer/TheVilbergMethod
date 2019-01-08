package world.chunk_generation;

import util.math.Vec3d;
import util.rlestorage.IntConverter;
import static util.rlestorage.IntConverter.pack;
import static util.rlestorage.IntConverter.unpack;
import util.rlestorage.RLEArrayStorage;
import util.rlestorage.RLEStorage;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;

public class LightingStep extends GenerationStep<Chunk> {

    public static final int SUNLIGHT = 40;

    static final Object LIGHTING_LOCK = new Object();
    static final int LARGE_Z = 1000000;
    static final LightFunc DARK_LF = new LightFunc(0, 0);
    static final LightFunc SUNLIGHT_LF = new LightFunc(SUNLIGHT, 0);

    public RLEStorage<LightFunc> light = new RLEArrayStorage(CHUNK_SIZE, new LightFuncConverter());

    public LightingStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void generate() {
        RLEStorage<Vec3d> chunkBlocks = region.require(ConstructionStep.class).blocks;
        synchronized (LIGHTING_LOCK) {
            light = new RLEArrayStorage(CHUNK_SIZE, new LightFuncConverter());
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    int z = chunkBlocks.columnAt(x, y).maxPos();
                    light.setRange(x, y, -LARGE_Z, z, DARK_LF);
                }
            }
        }
    }

    public static class LightFunc {

        public final int lightAtZero, lightSlope;

        public LightFunc(int lightAtZero, int lightSlope) {
            this.lightAtZero = lightAtZero;
            this.lightSlope = lightSlope;
        }

        public LightFunc add(int light) {
            return new LightFunc(lightAtZero + light, lightSlope);
        }

        public LightFunc add(LightFunc lf) {
            return new LightFunc(lightAtZero + lf.lightAtZero, lightSlope + lf.lightSlope);
        }

        public int lightAt(int z) {
            return lightAtZero + z * lightSlope;
        }

        public LightFunc sub(int light) {
            return new LightFunc(lightAtZero - light, lightSlope);
        }

        public LightFunc sub(LightFunc lf) {
            return new LightFunc(lightAtZero - lf.lightAtZero, lightSlope - lf.lightSlope);
        }

        @Override
        public String toString() {
            return "LightFunc{" + "lightAtZero=" + lightAtZero + ", lightSlope=" + lightSlope + '}';
        }

        public double zero() {
            return -lightAtZero / lightSlope;
        }
    }

    public static class LightFuncConverter implements IntConverter<LightFunc> {

        private static final int OFFSET = 1 << 15;

        @Override
        public LightFunc fromInt(int i) {
            if (i == 3) {
                return null;
            }
            int lightAtZero = unpack(i, 2, 30);
            int lightSlope = unpack(i, 0, 2);
            return new LightFunc(lightAtZero - OFFSET, lightSlope - 1);
        }

        @Override
        public int toInt(LightFunc t) {
            if (t == null) {
                return 3;
            }
            return pack(t.lightSlope + 1, 0, 2) + pack(t.lightAtZero + OFFSET, 2, 30);
        }
    }
}
