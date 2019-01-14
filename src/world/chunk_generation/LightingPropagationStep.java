package world.chunk_generation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.floor;
import static util.math.MathUtils.mod;
import util.math.Vec3d;
import util.rlestorage.RLEStorage;
import world.World;
import static world.chunk_generation.LightingStep.LARGE_Z;
import world.chunk_generation.LightingStep.LightFunc;
import static world.chunk_generation.LightingStep.SUNLIGHT_LF;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;

public class LightingPropagationStep extends GenerationStep<Chunk> {

    public LightingPropagationStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void generate() {
        RLEStorage<Vec3d> chunkBlocks = region.require(ConstructionStep.class).blocks;
        Queue<LightRay> toUpdate = new LinkedList();
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                int z = chunkBlocks.columnAt(x, y).maxPos();
                toUpdate.add(new LightRay(region.worldX() + x, region.worldY() + y, z + 1, LARGE_Z, SUNLIGHT_LF));
            }
        }
        updateLighting(world, toUpdate);
    }

    private static void updateLighting(World world, Queue<LightRay> toUpdate) {
        while (!toUpdate.isEmpty()) {
            LightRay lr = toUpdate.poll();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    int newX = lr.x - 1 + i + j;
                    int newY = lr.y - i + j;
                    Iterator<Entry<Integer, Vec3d>> iter = world.chunkManager.get(new Vec3d(newX, newY, 0),
                            ConstructionStep.class).blocks.columnAt(mod(newX, CHUNK_SIZE), mod(newY, CHUNK_SIZE)).iterator();
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
                                newLR.update(world, toUpdate, prev.getKey() + 1, current.getKey());
                            }
                        }
                        prev = current;
                    }
                }
            }
        }
    }

    private static class LightRay {

        public final int x, y, zMin, zMax;
        public final LightFunc lf;

        private LightRay(int x, int y, int zMin, int zMax, LightFunc lf) {
            this.x = x;
            this.y = y;
            this.zMin = zMin;
            this.zMax = zMax;
            this.lf = lf;
        }

        private void create(World world, Queue<LightRay> toUpdate) {
            toUpdate.add(this);
            LightingStep ls = world.chunkManager.get(new Vec3d(x, y, 0), LightingStep.class);
            synchronized (ls.light) {
                ls.light.setRange(mod(x, CHUNK_SIZE), mod(y, CHUNK_SIZE), zMin, zMax, lf);
            }
        }

        @Override
        public String toString() {
            return "LightRay{" + "x=" + x + ", y=" + y + ", zMin=" + zMin + ", zMax=" + zMax + ", lf=" + lf + '}';
        }

        private void update(World world, Queue<LightRay> toUpdate, int spaceMin, int spaceMax) {
            if (lf.lightSlope == 1 && Math.max(spaceMin, lf.zero() + 1) < zMin) {
                new LightRay(x, y, Math.max(spaceMin, (int) lf.zero() + 1), zMax, lf).update(world, toUpdate, spaceMin, spaceMax);
                return;
            } else if (lf.lightSlope == -1 && Math.min(spaceMax, lf.zero() - 1) > zMax) {
                new LightRay(x, y, zMin, Math.min(spaceMax, (int) lf.zero() - 1), lf).update(world, toUpdate, spaceMin, spaceMax);
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

            List<LightRay> newLightRays = new LinkedList();
            Iterator<Entry<Integer, LightFunc>> iter = world.chunkManager.get(new Vec3d(x, y, 0), LightingStep.class).light
                    .columnAt(mod(x, CHUNK_SIZE), mod(y, CHUNK_SIZE)).iterator();
            Entry<Integer, LightFunc> prev = iter.next();
            while (iter.hasNext()) {
                Entry<Integer, LightFunc> current = iter.next();
                for (LightRay newLR : newRays) {
                    int partZMin = Math.max(prev.getKey() + 1, newLR.zMin);
                    int partZMax = Math.min(current.getKey(), newLR.zMax);
                    if (partZMin <= partZMax) {
                        if (newLR.lf.lightAt(partZMin) > current.getValue().lightAt(partZMin)) {
                            if (newLR.lf.lightAt(partZMax) >= current.getValue().lightAt(partZMax)) {
                                newLightRays.add(new LightRay(x, y, partZMin, partZMax, newLR.lf));
                            } else {
                                partZMax = floor(newLR.lf.sub(current.getValue()).zero());
                                newLightRays.add(new LightRay(x, y, partZMin, partZMax, newLR.lf));
                            }
                        } else if (newLR.lf.lightAt(partZMax) > current.getValue().lightAt(partZMax)) {
                            if (newLR.lf.lightAt(partZMin) >= current.getValue().lightAt(partZMin)) {
                                newLightRays.add(new LightRay(x, y, partZMin, partZMax, newLR.lf));
                            } else {
                                partZMin = ceil(newLR.lf.sub(current.getValue()).zero());
                                newLightRays.add(new LightRay(x, y, partZMin, partZMax, newLR.lf));
                            }
                        }
                    }
                }
                prev = current;
            }
            for (LightRay lr : newLightRays) {
                lr.create(world, toUpdate);
            }
        }
    }
}
