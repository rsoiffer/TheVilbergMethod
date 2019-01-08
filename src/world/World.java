package world;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import static graphics.Color.WHITE;
import static graphics.voxels.VoxelModel.MODEL_SHADER;
import graphics.voxels.VoxelRenderer;
import static graphics.voxels.VoxelRenderer.DIRS;
import graphics.voxels.VoxelRenderer.VoxelRendererParams;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import util.Noise;
import static util.math.MathUtils.floor;
import static util.math.MathUtils.round;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec2d;
import util.math.Vec3d;
import util.rlestorage.IntConverter.Vec3dConverter;
import util.rlestorage.RLEArrayStorage;
import util.rlestorage.RLEStorage;
import static world.ColorScheme.LAND_FUNC;
import world.structures.Structure;
import world.structures.Tree;

public class World extends Behavior {

    public static final int WORLD_SIZE = 1024;
    public static final int BUFFER = 3;

    public final Random random = new Random();
    public final int[][] heightmap = new int[WORLD_SIZE][WORLD_SIZE];
    public final RLEStorage<Vec3d> worldBlocks = new RLEArrayStorage(WORLD_SIZE, new Vec3dConverter());

    private final Noise heightmapNoise = new Noise(random);
    private final SortedSet<Structure> structures = new TreeSet();
    private VoxelRenderer renderer;

    @Override
    public void createInner() {
        generate();

        List<Vec2d> toDraw = new LinkedList();
        Noise colorR = new Noise(random);
        Noise colorG = new Noise(random);
        Noise colorB = new Noise(random);

        for (int x = 0; x < WORLD_SIZE; x++) {
            for (int y = 0; y < WORLD_SIZE; y++) {
                if (x >= BUFFER && y >= BUFFER && x < WORLD_SIZE - BUFFER && y < WORLD_SIZE - BUFFER) {
                    toDraw.add(new Vec2d(x, y));
                }
                Vec3d colorNoise = new Vec3d(colorR.fbm2d(x, y, 2, .01), colorG.fbm2d(x, y, 2, .01), colorB.fbm2d(x, y, 2, .01));
                worldBlocks.setRangeInfinite(x, y, heightmap[x][y], LAND_FUNC.apply(colorNoise));
            }
        }

        List<Structure> built = new LinkedList();
        for (Structure s1 : structures) {
            if (!built.stream().anyMatch(s2 -> s1.intersects(s2))) {
                s1.blocks.copyTo(worldBlocks, 0, 0, 0);
                built.add(s1);
            }
        }

        Lighting lighting = new Lighting(worldBlocks);

        VoxelRendererParams<Vec3d> params = new VoxelRendererParams();
        params.columnsToDraw = toDraw;
        params.shader = MODEL_SHADER;
        params.vertexAttribSizes = Arrays.asList(3, 1, 3, 4);
        params.columnAt = worldBlocks::columnAt;
        params.voxelFaceToData = (vfi, dir) -> {
            int normal = DIRS.indexOf(dir);
            float r = (float) vfi.voxel.x;
            float g = (float) vfi.voxel.y;
            float b = (float) vfi.voxel.z;
            float[] ao = lighting.ambientOcclusion(vfi, dir);
            return new float[]{
                vfi.x, vfi.y, vfi.z,
                normal,
                r, g, b,
                ao[0], ao[1], ao[3], ao[2]
            };
        };
        renderer = new VoxelRenderer(params);
    }

    private void generate() {
        City city = new City(this, WORLD_SIZE / 2, WORLD_SIZE / 2);

        double[][] totalHeight = new double[WORLD_SIZE][WORLD_SIZE];
        double[][] totalWeight = new double[WORLD_SIZE][WORLD_SIZE];
        city.flatPlots().forEach(plot -> {
            for (int x = Math.max(0, plot.x - 10); x <= Math.min(WORLD_SIZE - 1, plot.maxX() + 10); x++) {
                for (int y = Math.max(0, plot.y - 10); y <= Math.min(WORLD_SIZE - 1, plot.maxY() + 10); y++) {
                    double dist = plot.distanceTo(new Vec2d(x, y));
                    double weight = dist == 0 ? 1e6 : 10 / Math.pow(dist, 2);
                    totalHeight[x][y] += heightRaw(plot.centerX(), plot.centerY()) * weight;
                    totalWeight[x][y] += weight;
                }
            }
        });
        for (int x = 0; x < WORLD_SIZE; x++) {
            for (int y = 0; y < WORLD_SIZE; y++) {
                heightmap[x][y] = round((totalHeight[x][y] + heightRaw(x, y)) / (totalWeight[x][y] + 1));
            }
        }

        PoissonDiskProcess treeProcess = new PoissonDiskProcess(random, .01, WORLD_SIZE - 40);
        for (Vec2d v : treeProcess.getPoints(20)) {
            int x = (int) v.x + 20;
            int y = (int) v.y + 20;
            structures.add(new Tree(this, x, y, 20));
        }

        structures.addAll(city.getStructures());
    }

    private int heightRaw(double x, double y) {
        return floor(100 * heightmapNoise.fbm2d(x, y, 5, .001));
    }

    @Override
    public Layer layer() {
        return RENDER3D;
    }

    @Override
    public void step() {
        renderer.render(Transformation.create(new Vec3d(0, 0, 0), Quaternion.IDENTITY, 1), WHITE);
    }
}
