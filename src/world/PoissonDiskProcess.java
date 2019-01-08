package world;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import util.math.MathUtils;
import util.math.Vec2d;

public class PoissonDiskProcess {

    private final List<Vec2d> points = new LinkedList();

    public PoissonDiskProcess(Random random, double density, double size) {
        int num = MathUtils.poissonSample(random, density * size * size);
        for (int i = 0; i < num; i++) {
            points.add(new Vec2d(random.nextDouble() * size, random.nextDouble() * size));
        }
    }

    public List<Vec2d> getPoints(double minDist) {
        List<Vec2d> r = new LinkedList();
        for (Vec2d v1 : points) {
            boolean keepPoint = true;
            for (Vec2d v2 : r) {
                if (v1.sub(v2).lengthSquared() < minDist * minDist) {
                    keepPoint = false;
                    break;
                }
            }
            if (keepPoint) {
                r.add(v1);
            }
        }
        return r;
    }
}
