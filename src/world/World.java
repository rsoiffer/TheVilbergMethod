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
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import util.Multithreader;
import util.Noise;
import static util.math.MathUtils.mod;
import util.math.Quaternion;
import util.math.Transformation;
import util.math.Vec3d;
import util.rlestorage.RLEColumn;
import world.chunk_generation.ConstructionStep;
import world.chunk_generation.HeightmapFlatteningStep;
import world.chunk_generation.HeightmapStep;
import world.chunk_generation.LightingPropagationStep;
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

    public static final double FOG_MULT = 10;
    public static final int RENDER_DISTANCE = 500;
    public static final int UNLOAD_DISTANCE = RENDER_DISTANCE + 500;

    public final long seed = new Random().nextLong();
    public final RegionManager<Chunk> chunkManager = new RegionManager<>(this, Chunk::new);
    public final RegionManager<Province> provinceManager = new RegionManager<>(this, Province::new);

    private final HashMap<String, Noise> noiseMap = new HashMap();
    private final HashSet<RegionPos> renderBorder = new HashSet();

    public Vec3d getBlock(Vec3d v) {
        return chunkManager.get(v, ConstructionStep.class).blocks.get(mod((int) v.x, CHUNK_SIZE), mod((int) v.y, CHUNK_SIZE), (int) v.z);
    }

    public RLEColumn<Vec3d> getBlockColumnAt(int x, int y) {
        return chunkManager.get(new Vec3d(x, y, 0), ConstructionStep.class).blocks.columnAt(mod(x, CHUNK_SIZE), mod(y, CHUNK_SIZE));
    }

    public boolean getBlockRangeEquals(int x, int y, int zMin, int zMax, Vec3d v) {
        return chunkManager.get(new Vec3d(x, y, 0), ConstructionStep.class).blocks.rangeEquals(mod(x, CHUNK_SIZE), mod(y, CHUNK_SIZE), zMin, zMax, v);
    }

    public int getFlattenedHeightmap(int x, int y) {
        return chunkManager.get(new Vec3d(x, y, 0), HeightmapFlatteningStep.class).flattenedHeightmap[mod(x, CHUNK_SIZE)][mod(y, CHUNK_SIZE)];
    }

    public double getHeightmap(int x, int y) {
        return chunkManager.get(new Vec3d(x, y, 0), HeightmapStep.class).heightmap[mod(x, CHUNK_SIZE)][mod(y, CHUNK_SIZE)];
    }

    public int getLight(Vec3d v) {
        chunkManager.getPos(v).nearby(1).forEach(rp -> chunkManager.get(rp, LightingPropagationStep.class));
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

    public double getRivermap(int x, int y) {
        return chunkManager.get(new Vec3d(x, y, 0), HeightmapStep.class).rivermap[mod(x, CHUNK_SIZE)][mod(y, CHUNK_SIZE)];
    }

    @Override
    public Layer layer() {
        return RENDER3D;
    }

    @Override
    public void step() {
        ToDoubleFunction<RegionPos> distanceToChunk = rp -> Camera.camera3d.position
                .sub(new Vec3d(rp.x + .5, rp.y + .5, 0).mul(CHUNK_SIZE)).setZ(0).length();

        // Load camera chunk
        RegionPos camera = chunkManager.getPos(Camera.camera3d.position);
        chunkManager.get(camera, RenderStep.class);
        renderBorder.addAll(camera.nearby(1));
        renderBorder.removeIf(rp -> chunkManager.has(rp, RenderStep.class, false));

        // Unload distant chunks
        chunkManager.removeIf(rp -> distanceToChunk.applyAsDouble(rp) > UNLOAD_DISTANCE);

        // Find nearest unloaded chunk
        Optional<RegionPos> nearestUnloaded = renderBorder.stream().min(Comparator.comparingDouble(distanceToChunk));
        if (nearestUnloaded.isPresent()) {
            MODEL_SHADER.setUniform("maxFogDist", (float) (FOG_MULT * distanceToChunk.applyAsDouble(nearestUnloaded.get())));
        }

        // Render chunks
        chunkManager.allGenerated(RenderStep.class, true).forEach(cp -> {
            if (camera.distance(cp) * CHUNK_SIZE <= RENDER_DISTANCE) {
                VoxelRenderer renderer = chunkManager.get(cp, RenderStep.class).renderer;
                renderer.render(Transformation.create(new Vec3d(0, 0, 0), Quaternion.IDENTITY, 1), WHITE);
            }
        });

        // Load new chunks
        if (Multithreader.isFree()) {
            if (nearestUnloaded.isPresent() && camera.distance(nearestUnloaded.get()) * CHUNK_SIZE <= RENDER_DISTANCE) {
                chunkManager.lazyGenerate(nearestUnloaded.get(), RenderStep.class);
                renderBorder.addAll(nearestUnloaded.get().nearby(1));
            }
        }
    }
}
