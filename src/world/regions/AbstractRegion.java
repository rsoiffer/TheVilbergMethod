package world.regions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import util.math.Vec3d;
import world.World;
import world.chunk_generation.LightingStep;

public abstract class AbstractRegion {

    public final World world;
    public final RegionPos pos;
    public final Random random;

    private final HashMap<Class<? extends GenerationStep>, GenerationStep> completedSteps = new HashMap();
    private final Set<Class<? extends GenerationStep>> shouldGenerate = Collections.newSetFromMap(new ConcurrentHashMap());

    public AbstractRegion(World world, RegionPos pos) {
        this.world = world;
        this.pos = pos;
        if (world != null && pos != null) {
            random = new Random(pos.hashCode() + getClass().hashCode() + world.seed);
        } else {
            random = null;
        }
    }

    public void cleanup() {
        // do something here
    }

    private <T extends GenerationStep> T createStep(Class<T> c) {
        synchronized (this) {
            T s = (T) completedSteps.get(c);
            if (s == null) {
                try {
                    s = (T) c.getConstructors()[0].newInstance(this);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                if (c.equals(LightingStep.class)) {
                }
                shouldGenerate.add(c);
                completedSteps.put(c, s);
            }
            return s;
        }
    }

    public <T extends GenerationStep> T getUnsafe(Class<T> c) {
        return (T) completedSteps.get(c);
    }

    public <T extends GenerationStep> T require(Class<T> c) {
        if (c == null) {
            return null;
        }
        T s = createStep(c);
        synchronized (s) {
            if (shouldGenerate.contains(c)) {
                s.generate();
                shouldGenerate.remove(c);
            }
            return s;
        }
    }

    public abstract int size();

    public boolean stepCreated(Class<? extends GenerationStep> c) {
        return completedSteps.containsKey(c);
    }

    public boolean stepFinished(Class<? extends GenerationStep> c) {
        return completedSteps.containsKey(c) && !shouldGenerate.contains(c);
    }

    public Vec3d worldCenter() {
        return new Vec3d(pos.x + .5, pos.y + .5, 0).mul(size());
    }

    public int worldX() {
        return pos.x * size();
    }

    public int worldY() {
        return pos.y * size();
    }
}
