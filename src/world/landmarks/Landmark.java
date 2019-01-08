package world.landmarks;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import util.math.Vec2d;
import world.regions.Chunk;
import world.regions.Province;
import world.structures.Rectangle;
import world.structures.Structure;

public abstract class Landmark {

    public final Province province;
    public final Random random;
    public final HashMap<Vec2d, Function<Chunk, Structure>> structurePlans = new HashMap();

    public Landmark(Province province) {
        this.province = province;
        random = province.random;
    }

    public Stream<Rectangle> flatPlots() {
        return Stream.of();
    }
}
