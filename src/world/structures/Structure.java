package world.structures;

import static graphics.voxels.VoxelRenderer.DIRS;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import static util.math.MathUtils.min;
import static util.math.MathUtils.vecMap;
import util.math.Vec3d;
import util.rlestorage.IntConverter.Vec3dConverter;
import util.rlestorage.RLEMapStorage;
import util.rlestorage.RLEStorage;
import world.regions.Chunk;

public class Structure implements Comparable<Structure> {

    public final Chunk chunk;
    public final Random random;
    public final RLEStorage<Vec3d> blocks;
    public double priority;

    private Vec3d maxPos, minPos;

    public Structure(Chunk chunk) {
        this.chunk = chunk;
        random = chunk.random;
        blocks = new RLEMapStorage(new Vec3dConverter());
        priority = chunk.random.nextDouble();
    }

    public void buildCorners(Rectangle r, int zMin, int zMax, Vec3d color) {
        blocks.setRange(r.x, r.y, zMin, zMax, color);
        blocks.setRange(r.maxX(), r.y, zMin, zMax, color);
        blocks.setRange(r.x, r.maxY(), zMin, zMax, color);
        blocks.setRange(r.maxX(), r.maxY(), zMin, zMax, color);
    }

    public void buildFill(Rectangle r, int zMin, int zMax, Vec3d color) {
        for (int i = r.x; i <= r.maxX(); i++) {
            for (int j = r.y; j <= r.maxY(); j++) {
                blocks.setRange(i, j, zMin, zMax, color);
            }
        }
    }

    public void buildFloor(Rectangle r, int z, Vec3d color) {
        for (int i = r.x + 1; i < r.maxX(); i++) {
            for (int j = r.y + 1; j < r.maxY(); j++) {
                blocks.set(i, j, z, color);
            }
        }
    }

    public void buildRoof(Rectangle r, int z, boolean pyramidStyle, int maxRoofHeight, Vec3d roofColor, Vec3d wallColor) {
        if (pyramidStyle) {
            for (int i = -1; i < r.w + 2; i++) {
                for (int j = -1; j < r.h + 2; j++) {
                    int roofHeight = z + min(i + 1, r.w + 1 - i, j + 1, r.h + 1 - j, maxRoofHeight);
                    blocks.setRange(i + r.x, j + r.y, Math.max(z, roofHeight - 1), roofHeight, roofColor);
                }
            }
        } else {
            boolean roofDir = chunk.random.nextDouble() < .5;
            for (int i = -1; i < r.w + 2; i++) {
                for (int j = -1; j < r.h + 2; j++) {
                    int roofHeight = z + (roofDir ? min(i + 1, r.w + 1 - i, maxRoofHeight) : min(j + 1, r.h + 1 - j, maxRoofHeight));
                    blocks.setRange(i + r.x, j + r.y, Math.max(z, roofHeight - 1), roofHeight, roofColor);
                    if ((i == 0 || i == r.w || j == 0 || j == r.h) && roofHeight > z + 2) {
                        blocks.setRange(i + r.x, j + r.y, z + 1, roofHeight - 2, wallColor);
                    }
                }
            }
        }
    }

    public void buildWalls(Rectangle r, int zMin, int zMax, Vec3d color) {
        for (int i = r.x; i <= r.maxX(); i++) {
            blocks.setRange(i, r.y, zMin, zMax, color);
            blocks.setRange(i, r.maxY(), zMin, zMax, color);
        }
        for (int i = r.y + 1; i < r.maxY(); i++) {
            blocks.setRange(r.x, i, zMin, zMax, color);
            blocks.setRange(r.maxX(), i, zMin, zMax, color);
        }
    }

    @Override
    public int compareTo(Structure o) {
        return Double.compare(o.priority, priority);
    }

    private synchronized void computeMaxMin() {
        if (maxPos == null) {
            Set<Vec3d> occupancy = computeOccupancy();
            maxPos = minPos = occupancy.iterator().next();
            for (Vec3d v : occupancy) {
                if (v.x > maxPos.x || v.y > maxPos.y || v.z > maxPos.z) {
                    maxPos = vecMap(maxPos, v, Math::max);
                }
                if (v.x < minPos.x || v.y < minPos.y || v.z < minPos.z) {
                    minPos = vecMap(minPos, v, Math::min);
                }
            }
        }
    }

    private Set<Vec3d> computeOccupancy() {
        Set<Vec3d> r = new HashSet();
        blocks.allColumns().forEach(c -> {
            if (!c.isEmpty()) {
                for (int z = c.minPos() + 1; z <= c.maxPos(); z++) {
                    r.add(new Vec3d(c.x, c.y, z));
                }
            }
        });
        return r;
    }

    public boolean intersects(Structure other) {
        computeMaxMin();
        other.computeMaxMin();
        if (minPos.x > other.maxPos.x || minPos.y > other.maxPos.y || minPos.z > other.maxPos.z
                || maxPos.x < other.minPos.x || maxPos.y < other.minPos.y || maxPos.z < other.minPos.z) {
            return false;
        }
        Set<Vec3d> occupancy = new HashSet(computeOccupancy());
        occupancy.retainAll(other.computeOccupancy());
        return !occupancy.isEmpty();
    }

    public void removeDisconnected(Vec3d start) {
        Set<Vec3d> connectedComponent = new HashSet();
        connectedComponent.add(start);
        Queue<Vec3d> toCheck = new LinkedList();
        toCheck.add(start);
        while (!toCheck.isEmpty()) {
            Vec3d v = toCheck.poll();
            for (Vec3d dir : DIRS) {
                Vec3d v2 = v.add(dir);
                if (!connectedComponent.contains(v2)) {
                    if (blocks.get((int) v2.x, (int) v2.y, (int) v2.z) != null) {
                        connectedComponent.add(v2);
                        toCheck.add(v2);
                    }
                }
            }
        }
        blocks.allColumns().forEach(c -> {
            if (!c.isEmpty()) {
                List<Integer> toRemove = new LinkedList();
                Iterator<Map.Entry<Integer, Vec3d>> i = c.iterator();
                Map.Entry<Integer, Vec3d> prev = i.next();
                while (i.hasNext()) {
                    Map.Entry<Integer, Vec3d> e = i.next();
                    if (e.getValue() != null) {
                        if (!connectedComponent.contains(new Vec3d(c.x, c.y, e.getKey()))) {
                            for (int z = prev.getKey() + 1; z <= e.getKey(); z++) {
                                toRemove.add(z);
                            }
                        }
                    }
                    prev = e;
                }
                for (int z : toRemove) {
                    blocks.set(c.x, c.y, z, null);
                }
            }
        });
    }
}
