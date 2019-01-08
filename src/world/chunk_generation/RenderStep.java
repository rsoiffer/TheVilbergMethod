package world.chunk_generation;

import static graphics.voxels.VoxelModel.MODEL_SHADER;
import graphics.voxels.VoxelRenderer;
import static graphics.voxels.VoxelRenderer.DIRS;
import graphics.voxels.VoxelRenderer.VoxelRendererParams;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import util.math.Vec2d;
import util.math.Vec3d;
import world.regions.Chunk;
import static world.regions.Chunk.CHUNK_SIZE;
import world.regions.GenerationStep;

public class RenderStep extends GenerationStep<Chunk> {

    public VoxelRenderer renderer;

    public RenderStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generate() {
        region.pos.nearby(2).forEach(rp -> world.chunkManager.get(rp, LightingStep.class));
        region.pos.nearby(1).forEach(rp -> world.chunkManager.get(rp, LightingStep.class).propagateInitial());

        List<Vec2d> toDraw = new LinkedList();
        for (int i = 0; i < CHUNK_SIZE; i++) {
            for (int j = 0; j < CHUNK_SIZE; j++) {
                toDraw.add(new Vec2d(i + region.worldX(), j + region.worldY()));
            }
        }

        VoxelRendererParams<Vec3d> params = new VoxelRendererParams();
        params.columnsToDraw = toDraw;
        params.shader = MODEL_SHADER;
        params.vertexAttribSizes = Arrays.asList(3, 1, 3, 4);
        params.columnAt = world::getBlockColumnAt;
        params.voxelFaceToData = (vfi, dir) -> {
            int normal = DIRS.indexOf(dir);
            float r = (float) vfi.voxel.x;
            float g = (float) vfi.voxel.y;
            float b = (float) vfi.voxel.z;
            float[] ao = LightingStep.ambientOcclusion(world, vfi, dir);
            return new float[]{
                vfi.x, vfi.y, vfi.z,
                normal,
                r, g, b,
                ao[0], ao[1], ao[3], ao[2]
            };
        };
        renderer = new VoxelRenderer(params);
    }
}
