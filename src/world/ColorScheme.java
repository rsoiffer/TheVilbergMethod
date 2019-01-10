package world;

import java.util.function.Function;
import util.math.Vec3d;

public class ColorScheme {

    public static final Vec3d ERROR = new Vec3d(1, 0, 1);

    public static final Vec3d LOG = new Vec3d(.4, .2, .1);
    public static final Vec3d PLANKS = new Vec3d(1, .7, .2);
    public static final Vec3d PLASTER = new Vec3d(1, .95, .7);
    // public static final Vec3d SLATE = new Vec3d(.1, .1, .1);
    public static final Vec3d SLATE = new Vec3d(.5, .1, .1);
    public static final Vec3d STONE = new Vec3d(.6, .6, .6);

    public static final Function<Vec3d, Vec3d> LAND_FUNC = v -> new Vec3d(0, 1, 0).add(v.mul(new Vec3d(1, 0, .6)));
    public static final Function<Double, Vec3d> LEAF_FUNC = d -> new Vec3d(d * .8, .7 - d * .5, .1);
}
