package world.regions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import util.Multithreader;
import static util.math.MathUtils.floor;
import util.math.Vec3d;
import world.World;
import static world.World.UNLOAD_DISTANCE;

public class RegionManager<U extends AbstractRegion> {

    private final Map<RegionPos, U> regions = new ConcurrentHashMap();
    private final World world;
    private final BiFunction<World, RegionPos, U> constructor;
    private final int size;

    public RegionManager(World world, BiFunction<World, RegionPos, U> constructor) {
        this.world = world;
        this.constructor = constructor;
        size = constructor.apply(null, null).size();
    }

    public List<RegionPos> allGenerated(Class<? extends GenerationStep<U>> c) {
        return regions.entrySet().stream().filter(e -> e.getValue().stepFinished(c))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public Stream<RegionPos> border(Class<? extends GenerationStep<U>> c) {
        return regions.keySet().stream().flatMap(rp -> rp.nearby(1).stream())
                .filter(rp -> shouldBeBorder(rp, c));
    }

    public <T extends GenerationStep<U>> T get(Vec3d pos, Class<T> c) {
        return get(getPos(pos), c);
    }

    public <T extends GenerationStep<U>> T get(RegionPos pos, Class<T> c) {
        if (!regions.containsKey(pos)) {
            regions.put(pos, constructor.apply(world, pos));
        }
        return (T) regions.get(pos).require(c);
    }

    public RegionPos getPos(Vec3d pos) {
        return new RegionPos(floor(pos.x / size), floor(pos.y / size));
    }

    public U getRegion(RegionPos pos) {
        return regions.get(pos);
    }

    public boolean has(Vec3d pos, Class<? extends GenerationStep<U>> c, boolean requireFinished) {
        return has(getPos(pos), c, requireFinished);
    }

    public boolean has(RegionPos pos, Class<? extends GenerationStep<U>> c, boolean requireFinished) {
        if (requireFinished) {
            return regions.containsKey(pos) && regions.get(pos).stepFinished(c);
        } else {
            return regions.containsKey(pos) && regions.get(pos).stepCreated(c);
        }
    }

    public void lazyGenerate(RegionPos pos, Class<? extends GenerationStep<U>> c) {
        if (has(pos, c, false)) {
            return;
        }
        if (!regions.containsKey(pos)) {
            regions.put(pos, constructor.apply(world, pos));
        }
        Multithreader.run(() -> regions.get(pos).require(c));
    }

    public void remove(RegionPos pos) {
        if (regions.containsKey(pos)) {
            AbstractRegion t = regions.remove(pos);
            t.cleanup();
        }
    }

    public void removeDistant(RegionPos camera) {
        for (RegionPos pos : regions.keySet()) {
            if (camera.distance(pos) * size > UNLOAD_DISTANCE) {
                remove(pos);
            }
        }
    }

    private boolean shouldBeBorder(RegionPos pos, Class<? extends GenerationStep<U>> c) {
        if (has(pos, c, false)) {
            return false;
        }
        for (RegionPos rp : pos.nearby(1)) {
            if (has(rp, c, false)) {
                return true;
            }
        }
        return false;
    }
}
