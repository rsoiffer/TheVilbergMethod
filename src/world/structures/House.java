package world.structures;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import util.math.Vec3d;
import static world.ColorScheme.ERROR;
import static world.ColorScheme.LOG;
import static world.ColorScheme.PLANKS;
import static world.ColorScheme.PLASTER;
import static world.ColorScheme.SLATE;
import world.World;

public class House extends Structure {

    private static final int WALL_EXTENSION = 0;

    public House(World world, Rectangle base) {
        super(world);
        priority += 10;

        int z = world.heightmap[base.centerX()][base.centerY()];
        int floorHeight = 6 + random.nextInt(3);
        int numFloors = 1 + random.nextInt(3);
        Rectangle staircase = new Rectangle(base.x + 1 + random.nextInt(base.w - 6), base.y + 1 + random.nextInt(base.h - 6), 4, 4);

        for (int floor = 0; floor < numFloors; floor++) {
            int wallBottom = z + floor * floorHeight;
            List<Rectangle> rooms = recursivelySubdivide(base, staircase, 5);
            for (Rectangle room : rooms) {
                buildWalls(room, wallBottom, wallBottom + floorHeight - 1, PLASTER);
            }
            for (RoomBorder rb : findRoomBorders(rooms)) {
                if (random.nextDouble() < .2) {
                    rb.buildOpening(wallBottom + 1, wallBottom + floorHeight - 1, rb.length, null);
                } else {
                    rb.buildOpening(wallBottom + 1, wallBottom + 4, 2, null);
                }
            }
            List<RoomBorder> outsideBorders = findOutsideBorders(base, rooms);
            if (floor == 0) {
                int numDoors = 1 + random.nextInt(3);
                for (int i = 0; i < numDoors; i++) {
                    RoomBorder rb = outsideBorders.get(random.nextInt(outsideBorders.size()));
                    int doorPos = rb.buildOpening(wallBottom + 1, wallBottom + 4, 2, null);
//                    terrainObjects.add(new TerrainObjectInstance(getTerrainObject("door"), sc.pos,
//                            x + rb.x + (rb.horizontal ? doorPos : 0), y + rb.y + (rb.horizontal ? 0 : doorPos), z + wallBottom + 1, rb.horizontal ? 0 : 1));
                }
            }
            int numWindows = 4 + random.nextInt(10);
            for (int i = 0; i < numWindows; i++) {
                RoomBorder rb = outsideBorders.get(random.nextInt(outsideBorders.size()));
                rb.buildOpening(wallBottom + 3, wallBottom + 4, 2, null);
            }
            buildCorners(base.expand(WALL_EXTENSION), wallBottom, wallBottom + floorHeight - 1, LOG);
            buildFloor(base, z + floor * floorHeight, PLANKS);
            if (floor > 0) {
                buildFloor(staircase.expand(1), z + floor * floorHeight, null);
                buildWalls(base.expand(WALL_EXTENSION), z + floor * floorHeight, z + floor * floorHeight, LOG);
            }
        }

        int minRoofHeight = z + numFloors * floorHeight;
        int maxRoofHeight = random.nextInt(20);
        buildFloor(base, minRoofHeight, PLANKS);
        buildFloor(staircase.expand(1), minRoofHeight, null);
        buildStaircase(staircase.centerX(), staircase.centerY(), z + 1, minRoofHeight);
        buildWalls(base.expand(WALL_EXTENSION), minRoofHeight, minRoofHeight, LOG);
        buildRoof(base.expand(WALL_EXTENSION), minRoofHeight, random.nextDouble() < .5, maxRoofHeight, SLATE, PLASTER);

        for (int i = base.x; i <= base.maxX(); i++) {
            for (int j = base.y; j <= base.maxY(); j++) {
                if (world.heightmap[i][j] < z) {
                    blocks.setRange(i, j, world.heightmap[i][j], z - 1, ERROR);
                }
            }
        }
    }

    private void buildStaircase(int x, int y, int minZ, int maxZ) {
        blocks.setRange(x, y, minZ, maxZ, LOG);
        for (int z = minZ; z <= maxZ; z++) {
            switch ((z - minZ) % 8) {
                case 0:
                    blocks.set(x + 1, y, z, PLANKS);
                    blocks.set(x + 2, y, z, PLANKS);
                    break;
                case 1:
                    blocks.set(x + 1, y + 1, z, PLANKS);
                    blocks.set(x + 2, y + 1, z, PLANKS);
                    blocks.set(x + 2, y + 2, z, PLANKS);
                    blocks.set(x + 1, y + 2, z, PLANKS);
                    break;
                case 2:
                    blocks.set(x, y + 1, z, PLANKS);
                    blocks.set(x, y + 2, z, PLANKS);
                    break;
                case 3:
                    blocks.set(x - 1, y + 1, z, PLANKS);
                    blocks.set(x - 2, y + 1, z, PLANKS);
                    blocks.set(x - 2, y + 2, z, PLANKS);
                    blocks.set(x - 1, y + 2, z, PLANKS);
                    break;
                case 4:
                    blocks.set(x - 1, y, z, PLANKS);
                    blocks.set(x - 2, y, z, PLANKS);
                    break;
                case 5:
                    blocks.set(x - 1, y - 1, z, PLANKS);
                    blocks.set(x - 2, y - 1, z, PLANKS);
                    blocks.set(x - 2, y - 2, z, PLANKS);
                    blocks.set(x - 1, y - 2, z, PLANKS);
                    break;
                case 6:
                    blocks.set(x, y - 1, z, PLANKS);
                    blocks.set(x, y - 2, z, PLANKS);
                    break;
                case 7:
                    blocks.set(x + 1, y - 1, z, PLANKS);
                    blocks.set(x + 2, y - 1, z, PLANKS);
                    blocks.set(x + 2, y - 2, z, PLANKS);
                    blocks.set(x + 1, y - 2, z, PLANKS);
                    break;
            }
        }
    }

    private RoomBorder findRoomBorder(Rectangle r1, Rectangle r2) {
        if (r1.maxX() == r2.x || r2.maxX() == r1.x) {
            int x = (r1.maxX() == r2.x) ? r2.x : r1.x;
            int minY = Math.max(r1.y, r2.y) + 1;
            int maxY = Math.min(r1.maxY(), r2.maxY()) - 1;
            if (maxY - minY >= 1) {
                return new RoomBorder(r1, r2, x, minY, maxY - minY + 1, false);
            }
        }
        if (r1.maxY() == r2.y || r2.maxY() == r1.y) {
            int y = (r1.maxY() == r2.y) ? r2.y : r1.y;
            int minX = Math.max(r1.x, r2.x) + 1;
            int maxX = Math.min(r1.maxX(), r2.maxX()) - 1;
            if (maxX - minX >= 1) {
                return new RoomBorder(r1, r2, minX, y, maxX - minX + 1, true);
            }
        }
        return null;
    }

    private List<RoomBorder> findRoomBorders(List<Rectangle> rooms) {
        List<RoomBorder> r = new LinkedList();
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                RoomBorder rb = findRoomBorder(rooms.get(i), rooms.get(j));
                if (rb != null) {
                    r.add(rb);
                }
            }
        }
        return r;
    }

    private List<RoomBorder> findOutsideBorders(Rectangle base, List<Rectangle> rooms) {
        List<RoomBorder> r = new LinkedList();
        for (Rectangle room : rooms) {
            if (room.x == base.x) {
                r.add(new RoomBorder(room, null, room.x, room.y + 1, room.h - 1, false));
            }
            if (room.maxX() == base.maxX()) {
                r.add(new RoomBorder(room, null, room.maxX(), room.y + 1, room.h - 1, false));
            }
            if (room.y == base.y) {
                r.add(new RoomBorder(room, null, room.x + 1, room.y, room.w - 1, true));
            }
            if (room.maxY() == base.maxY()) {
                r.add(new RoomBorder(room, null, room.x + 1, room.maxY(), room.w - 1, true));
            }
        }
        return r;
    }

    private List<Rectangle> recursivelySubdivide(Rectangle r, Rectangle preserve, int minSize) {
        if (r.w * r.h < minSize * minSize * 4) {
            return Arrays.asList(r);
        }
        int wChanges = Math.max(r.w - minSize * 2, 0);
        int hChanges = Math.max(r.h - minSize * 2, 0);
        if (wChanges + hChanges == 0) {
            return Arrays.asList(r);
        }
        while (true) {
            int choice = random.nextInt(wChanges + hChanges + minSize);
            if (choice < wChanges) {
                int w1 = choice + minSize;
                Rectangle r1 = new Rectangle(r.x, r.y, w1, r.h);
                Rectangle r2 = new Rectangle(r.x + w1, r.y, r.w - w1, r.h);
                if (preserve.intersects(r1) && preserve.intersects(r2)) {
                    continue;
                }
                List<Rectangle> l = new LinkedList();
                l.addAll(recursivelySubdivide(r1, preserve, minSize));
                l.addAll(recursivelySubdivide(r2, preserve, minSize));
                return l;
            } else if (choice < wChanges + hChanges) {
                int h1 = choice - wChanges + minSize;
                Rectangle r1 = new Rectangle(r.x, r.y, r.w, h1);
                Rectangle r2 = new Rectangle(r.x, r.y + h1, r.w, r.h - h1);
                if (preserve.intersects(r1) && preserve.intersects(r2)) {
                    continue;
                }
                List<Rectangle> l = new LinkedList();
                l.addAll(recursivelySubdivide(r1, preserve, minSize));
                l.addAll(recursivelySubdivide(r2, preserve, minSize));
                return l;
            } else {
                return Arrays.asList(r);
            }
        }
    }

    private class RoomBorder {

        public final Rectangle r1, r2;
        public final int x, y, length;
        public final boolean horizontal;

        public RoomBorder(Rectangle r1, Rectangle r2, int x, int y, int length, boolean horizontal) {
            this.r1 = r1;
            this.r2 = r2;
            this.x = x;
            this.y = y;
            this.length = length;
            this.horizontal = horizontal;
        }

        public int buildOpening(int zMin, int zMax, int length, Vec3d bt) {
            int pos = random.nextInt(this.length - length + 1);
            for (int i = pos; i < pos + length; i++) {
                blocks.setRange(x + (horizontal ? i : 0), y + (horizontal ? 0 : i), zMin, zMax, bt);
            }
            return pos;
        }
    }
}
