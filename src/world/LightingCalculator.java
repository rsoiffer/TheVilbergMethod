package world;

import static graphics.voxels.VoxelRenderer.DIRS;
import graphics.voxels.VoxelRenderer.VoxelFaceInfo;
import static graphics.voxels.VoxelRenderer.VoxelFaceInfo.NORMAL_TO_DIR1;
import static graphics.voxels.VoxelRenderer.VoxelFaceInfo.NORMAL_TO_DIR2;
import util.math.Vec3d;
import static world.chunk_generation.LightingStep.SUNLIGHT;

public class LightingCalculator {

    private static final int AO_SIZE = 3;
    private static final Vec3d DIRECTIONAL_LIGHTING_1 = new Vec3d(.92, .97, 1);
    private static final Vec3d DIRECTIONAL_LIGHTING_2 = new Vec3d(.88, .83, .8);

    public static float[] ambientOcclusion(World world, VoxelFaceInfo vfi, Vec3d dir) {
        Vec3d pos = new Vec3d(vfi.x, vfi.y, vfi.z).add(dir);
        int normal = DIRS.indexOf(dir);
        Vec3d dir1 = NORMAL_TO_DIR1[normal];
        Vec3d dir2 = NORMAL_TO_DIR2[normal];

        boolean[][] solid = new boolean[2 * AO_SIZE + 1][2 * AO_SIZE + 1];
        double[][] lightLevel = new double[2 * AO_SIZE + 1][2 * AO_SIZE + 1];
        for (int i = -AO_SIZE; i <= AO_SIZE; i++) {
            for (int j = -AO_SIZE; j <= AO_SIZE; j++) {
                Vec3d v = pos.add(dir1.mul(i)).add(dir2.mul(j));
                solid[i + AO_SIZE][j + AO_SIZE] = world.getBlock(v) != null;
                lightLevel[i + AO_SIZE][j + AO_SIZE] = world.getLight(v);
            }
        }

        float[] returnVals = new float[4];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                Vec3d total = aoQuadrant(AO_SIZE, solid, lightLevel, AO_SIZE + i, AO_SIZE + j, 1, 1)
                        .add(aoQuadrant(AO_SIZE, solid, lightLevel, AO_SIZE + i - 1, AO_SIZE + j, -1, 1))
                        .add(aoQuadrant(AO_SIZE, solid, lightLevel, AO_SIZE + i - 1, AO_SIZE + j - 1, -1, -1))
                        .add(aoQuadrant(AO_SIZE, solid, lightLevel, AO_SIZE + i, AO_SIZE + j - 1, 1, -1));
                double totalLight = total.x;
                double numEmpty = total.y;
                double numSolid = total.z;

                double directionalLighting = Math.max(dir.dot(DIRECTIONAL_LIGHTING_1), -dir.dot(DIRECTIONAL_LIGHTING_2));
                double darkPerc = 1 - totalLight / (numEmpty * SUNLIGHT);
                double solidPerc = numSolid / (numEmpty + numSolid);
                returnVals[i + 2 * j] = (float) (Math.exp(solidPerc * -.6) * Math.pow(darkPerc * 1.5 + 1, -2) * Math.pow(directionalLighting, 2));
            }
        }
        return returnVals;
    }

    private static Vec3d aoQuadrant(int size, boolean[][] solid, double[][] lightLevel, int x1, int y1, int dx, int dy) {
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
}
