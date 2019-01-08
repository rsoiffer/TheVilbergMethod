package world;

import static graphics.voxels.VoxelRenderer.DIRS;
import graphics.voxels.VoxelRenderer.VoxelFaceInfo;
import static graphics.voxels.VoxelRenderer.VoxelFaceInfo.NORMAL_TO_DIR1;
import static graphics.voxels.VoxelRenderer.VoxelFaceInfo.NORMAL_TO_DIR2;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.floor;
import util.math.Vec3d;
import util.rlestorage.IntConverter;
import static util.rlestorage.IntConverter.pack;
import static util.rlestorage.IntConverter.unpack;
import util.rlestorage.RLEArrayStorage;
import util.rlestorage.RLEStorage;
import static world.World.WORLD_SIZE;

public class Lighting {

    private static final int SUNLIGHT = 40;
    private static final int LARGE_Z = 1000000;
    private static final boolean SKIP_AO = false;

    private static final Vec3d DIRECTIONAL_LIGHTING_1 = new Vec3d(.92, .97, 1);
    private static final Vec3d DIRECTIONAL_LIGHTING_2 = new Vec3d(.88, .83, .8);

    private static final LightFunc DARK_LF = new LightFunc(0, 0);
    private static final LightFunc SUNLIGHT_LF = new LightFunc(SUNLIGHT, 0);

    private final RLEStorage<Vec3d> worldBlocks;
    private final RLEStorage<LightFunc> light;

    public Lighting(RLEStorage<Vec3d> worldBlocks) {
        this.worldBlocks = worldBlocks;
        light = new RLEArrayStorage(WORLD_SIZE, new LightFuncConverter());

        Queue<LightRay> toUpdate = new LinkedList();
        for (int x = 0; x < WORLD_SIZE; x++) {
            for (int y = 0; y < WORLD_SIZE; y++) {
                int z = worldBlocks.columnAt(x, y).maxPos();
                light.setRange(x, y, -LARGE_Z, worldBlocks.columnAt(x, y).maxPos(), DARK_LF);
                toUpdate.add(new LightRay(x, y, z, LARGE_Z, SUNLIGHT_LF));
            }
        }

        while (!toUpdate.isEmpty()) {
            LightRay lr = toUpdate.poll();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    int newX = lr.x - 1 + i + j;
                    int newY = lr.y - i + j;
                    if (newX >= 0 && newY >= 0 && newX < WORLD_SIZE && newY < WORLD_SIZE) {
                        Iterator<Entry<Integer, Vec3d>> iter = worldBlocks.columnAt(newX, newY).iterator();
                        Entry<Integer, Vec3d> prev = iter.next();
                        while (iter.hasNext()) {
                            Entry<Integer, Vec3d> current = iter.next();
                            if (prev.getValue() != null && current.getValue() == null) {
                                int zMin = Math.max(lr.zMin, prev.getKey() + 1);
                                int zMax = Math.min(lr.zMax, current.getKey());
                                if (lr.lf.lightAt(zMin) == 1) {
                                    zMin++;
                                }
                                if (lr.lf.lightAt(zMax) == 1) {
                                    zMax--;
                                }
                                if (zMin <= zMax) {
                                    LightRay newLR = new LightRay(newX, newY, zMin, zMax, lr.lf.add(-1));
                                    newLR.update(this, toUpdate, prev.getKey() + 1, current.getKey());
                                }
                            }
                            prev = current;
                        }
                    }
                }
            }
        }
    }

    public float[] ambientOcclusion(VoxelFaceInfo vfi, Vec3d dir) {
        int size = 3;

        Vec3d pos = new Vec3d(vfi.x, vfi.y, vfi.z).add(dir);
        int normal = DIRS.indexOf(dir);
        Vec3d dir1 = NORMAL_TO_DIR1[normal];
        Vec3d dir2 = NORMAL_TO_DIR2[normal];

        boolean[][] solid = new boolean[2 * size + 1][2 * size + 1];
        double[][] lightLevel = new double[2 * size + 1][2 * size + 1];
        for (int i = -size; i <= size; i++) {
            for (int j = -size; j <= size; j++) {
                Vec3d v = pos.add(dir1.mul(i)).add(dir2.mul(j));
                solid[i + size][j + size] = worldBlocks.get((int) v.x, (int) v.y, (int) v.z) != null;
                lightLevel[i + size][j + size] = lightAt(v);
            }
        }

        float[] returnVals = new float[4];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                Vec3d total = aoQuadrant(size, solid, lightLevel, size + i, size + j, 1, 1)
                        .add(aoQuadrant(size, solid, lightLevel, size + i - 1, size + j, -1, 1))
                        .add(aoQuadrant(size, solid, lightLevel, size + i - 1, size + j - 1, -1, -1))
                        .add(aoQuadrant(size, solid, lightLevel, size + i, size + j - 1, 1, -1));
                double totalLight = total.x;
                double numEmpty = total.y;
                double numSolid = total.z;

                double directionalLighting = Math.max(dir.dot(DIRECTIONAL_LIGHTING_1), -dir.dot(DIRECTIONAL_LIGHTING_2));
                double darkPerc = 1 - totalLight / (numEmpty * SUNLIGHT);
                double solidPerc = numSolid / (numEmpty + numSolid);
                // returnVals[i + 2 * j] = (float) (Math.exp(darkPerc * -3 + solidPerc * -.6) * Math.pow(directionalLighting, 2));
                returnVals[i + 2 * j] = (float) (Math.exp(solidPerc * -.6) * Math.pow(darkPerc * 1.5 + 1, -2) * Math.pow(directionalLighting, 2));
            }
        }
        return returnVals;
    }

    private Vec3d aoQuadrant(int size, boolean[][] solid, double[][] lightLevel, int x1, int y1, int dx, int dy) {
        double totalLight = 0;
        double numEmpty = 0;
        double numSolid = 0;
        boolean[][] solidCopy = new boolean[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double weight = 1. / ((i + 1) * (i + 1) + (j + 1) * (j + 1));
                int x = x1 + i * dx;
                int y = y1 + j * dy;
                if (!solid[x][y] && ((i == 0 && j == 0) || (i > 0 && !solidCopy[i - 1][j]) || (j > 0 && !solidCopy[i][j - 1]))) {
                    totalLight += lightLevel[x][y] * weight;
                    numEmpty += weight;
                } else {
                    numSolid += weight;
                    solidCopy[i][j] = true;
                }
            }
        }
        return new Vec3d(totalLight, numEmpty, numSolid);
    }

    public int lightAt(int x, int y, int z) {
        LightFunc lf = light.get(x, y, z);
        if (lf == null) {
            return SUNLIGHT;
        }
        return lf.lightAtZero + z * lf.lightSlope;
    }

    public int lightAt(Vec3d pos) {
        return lightAt((int) pos.x, (int) pos.y, (int) pos.z);
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

        public double zero() {
            return -lightAtZero / lightSlope;
        }
    }

    public static class LightFuncConverter implements IntConverter<LightFunc> {

        private static final int[] SIZES = {2, 30};
        private static final int OFFSET = 1 << 15;

        @Override
        public LightFunc fromInt(int i) {
            if (i == 3) {
                return null;
            }
            int[] values = unpack(SIZES, i);
            return new LightFunc(values[1] - OFFSET, values[0] - 1);
        }

        @Override
        public int toInt(LightFunc t) {
            if (t == null) {
                return 3;
            }
            return pack(SIZES, t.lightSlope + 1, t.lightAtZero + OFFSET);
        }
    }

    public static class LightRay {

        public final int x, y, zMin, zMax;
        public final LightFunc lf;

        public LightRay(int x, int y, int zMin, int zMax, LightFunc lf) {
            this.x = x;
            this.y = y;
            this.zMin = zMin;
            this.zMax = zMax;
            this.lf = lf;
        }

        private void create(Lighting lighting, Queue<LightRay> toUpdate) {
            toUpdate.add(this);
            lighting.light.setRange(x, y, zMin, zMax, lf);
        }

        @Override
        public String toString() {
            return "LightRay{" + "x=" + x + ", y=" + y + ", zMin=" + zMin + ", zMax=" + zMax + ", lf=" + lf + '}';
        }

        private void update(Lighting lighting, Queue<LightRay> toUpdate, int spaceMin, int spaceMax) {
            if (lf.lightSlope == 1 && Math.max(spaceMin, lf.zero() + 1) < zMin) {
                new LightRay(x, y, Math.max(spaceMin, (int) lf.zero() + 1), zMax, lf).update(lighting, toUpdate, spaceMin, spaceMax);
                return;
            } else if (lf.lightSlope == -1 && Math.min(spaceMax, lf.zero() - 1) > zMax) {
                new LightRay(x, y, zMin, Math.min(spaceMax, (int) lf.zero() - 1), lf).update(lighting, toUpdate, spaceMin, spaceMax);
                return;
            }
            List<LightRay> newRays = new LinkedList();
            newRays.add(this);
            if (lf.lightAt(zMin) > 1 && zMin > spaceMin) {
                newRays.add(new LightRay(x, y, Math.max(spaceMin, zMin - lf.lightAt(zMin) + 1), zMin - 1, new LightFunc(lf.lightAt(zMin) - zMin, 1)));
            }
            if (lf.lightAt(zMax) > 1 && zMax < spaceMax) {
                newRays.add(new LightRay(x, y, zMax + 1, Math.min(spaceMax, zMax + lf.lightAt(zMax) - 1), new LightFunc(lf.lightAt(zMax) + zMax, -1)));
            }

            List<Entry<Integer, LightFunc>> existing = new LinkedList();
            lighting.light.columnAt(x, y).forEach(existing::add);
            Iterator<Entry<Integer, LightFunc>> iter = existing.iterator();
            Entry<Integer, LightFunc> prev = iter.next();
            while (iter.hasNext()) {
                Entry<Integer, LightFunc> current = iter.next();
                for (LightRay newLR : newRays) {
                    int partZMin = Math.max(prev.getKey() + 1, newLR.zMin);
                    int partZMax = Math.min(current.getKey(), newLR.zMax);
                    if (partZMin <= partZMax) {
                        if (newLR.lf.lightAt(partZMin) > current.getValue().lightAt(partZMin)) {
                            if (newLR.lf.lightAt(partZMax) >= current.getValue().lightAt(partZMax)) {
                                new LightRay(x, y, partZMin, partZMax, newLR.lf).create(lighting, toUpdate);
                            } else {
                                partZMax = floor(newLR.lf.sub(current.getValue()).zero());
                                new LightRay(x, y, partZMin, partZMax, newLR.lf).create(lighting, toUpdate);
                            }
                        } else if (newLR.lf.lightAt(partZMax) > current.getValue().lightAt(partZMax)) {
                            if (newLR.lf.lightAt(partZMin) >= current.getValue().lightAt(partZMin)) {
                                new LightRay(x, y, partZMin, partZMax, newLR.lf).create(lighting, toUpdate);
                            } else {
                                partZMin = ceil(newLR.lf.sub(current.getValue()).zero());
                                new LightRay(x, y, partZMin, partZMax, newLR.lf).create(lighting, toUpdate);
                            }
                        }
                    }
                }
                prev = current;
            }
        }
    }
}
