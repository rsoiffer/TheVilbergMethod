package world;

import engine.Behavior;
import engine.Layer;
import static engine.Layer.RENDER3D;
import graphics.Camera;
import static graphics.Color.WHITE;
import static graphics.voxels.VoxelModel.MODEL_SHADER;
import graphics.voxels.VoxelRenderer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import util.Multithreader;
import util.Noise;
import static util.math.MathUtils.mod;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;
import util.rlestorage.RLEColumn;
import world.chunk_generation.ConstructionStep;
import world.chunk_generation.HeightmapStep;
import world.chunk_generation.LightingStep;
import world.chunk_generation.LightingStep.LightFunc;
import static world.chunk_generation.LightingStep.SUNLIGHT;
import world.chunk_generation.RenderStep;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.Province;
import world.regions.RegionManager;
import world.regions.RegionPos;

public class World extends Behavior {

    public static final int RENDER_DISTANCE = 750;
    public static final int UNLOAD_DISTANCE = 1000;

    public final long seed = new Random().nextLong();

    public final RegionManager<Chunk> chunkManager = new RegionManager<>(this, Chunk::new);
    public final RegionManager<Province> provinceManager = new RegionManager<>(this, Province::new);
    private final HashMap<String, Noise> noiseMap = new HashMap();

    public Vec3d getBlock(Vec3d v) {
        return chunkManager.get(v, ConstructionStep.class).blocks.get(mod((int) v.x, CHUNK_SIZE), mod((int) v.y, CHUNK_SIZE), (int) v.z);
    }

    public RLEColumn<Vec3d> getBlockColumnAt(int x, int y) {
        return chunkManager.get(new Vec3d(x, y, 0), ConstructionStep.class).blocks.columnAt(mod(x, CHUNK_SIZE), mod(y, CHUNK_SIZE));
    }

    public boolean getBlockRangeEquals(int x, int y, int zMin, int zMax, Vec3d v) {
        return chunkManager.get(new Vec3d(x, y, 0), ConstructionStep.class).blocks.rangeEquals(mod(x, CHUNK_SIZE), mod(y, CHUNK_SIZE), zMin, zMax, v);
    }

    public int getHeightmap(int x, int y) {
        return chunkManager.get(new Vec3d(x, y, 0), HeightmapStep.class).heightmap[mod(x, CHUNK_SIZE)][mod(y, CHUNK_SIZE)];
    }

    public int getLight(Vec3d v) {
        LightFunc lf = chunkManager.get(v, LightingStep.class).light.get(mod((int) v.x, CHUNK_SIZE), mod((int) v.y, CHUNK_SIZE), (int) v.z);
        if (lf == null) {
            return SUNLIGHT;
        }
        return lf.lightAtZero + (int) v.z * lf.lightSlope;
    }

    public Noise getNoise(String name) {
        if (!noiseMap.containsKey(name)) {
            noiseMap.put(name, new Noise(new Random(name.hashCode() + seed)));
        }
        return noiseMap.get(name);
    }

    @Override
    public Layer layer() {
        return RENDER3D;
    }

    @Override
    public void step() {
        RegionPos camera = chunkManager.getPos(Camera.camera3d.position);
        chunkManager.get(camera, RenderStep.class);
        chunkManager.removeDistant(camera);

        Optional<RegionPos> nearestUnloaded = chunkManager.border(RenderStep.class)
                .min(Comparator.comparingDouble(camera::distance));
        if (nearestUnloaded.isPresent()) {
            MODEL_SHADER.setUniform("maxFogDist", (float) camera.distance(nearestUnloaded.get()) * CHUNK_SIZE);
        }

        chunkManager.allGenerated(RenderStep.class).forEach(cp -> {
            VoxelRenderer renderer = chunkManager.get(cp, RenderStep.class).renderer;
            renderer.render(Transformation.create(new Vec3d(0, 0, 0), Quaternion.IDENTITY, 1), WHITE);
        });

        if (Multithreader.isFree()) {
            if (nearestUnloaded.isPresent() && camera.distance(nearestUnloaded.get()) * CHUNK_SIZE <= RENDER_DISTANCE) {
                chunkManager.lazyGenerate(nearestUnloaded.get(), RenderStep.class);
            }
        }
    }
}
