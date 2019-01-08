package world.landmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import static util.math.MathUtils.poissonSample;
import static util.math.MathUtils.round;
import util.math.Vec2d;
import world.regions.Province;
import world.structures.House;
import world.structures.Rectangle;
import world.structures.Tower;
import world.structures.Wall;

public class City extends Landmark {

    private static final int size = 256;
    private static final int towerWidth = 10, wallWidth = 4, wallHeight = 20;
    private static final int wallBuffer = 4;

    private List<Rectangle> housePlots = new ArrayList();
    private List<Rectangle> towerPlots = new ArrayList();
    private List<Rectangle> wallPlots = new ArrayList();

    public City(Province province, int x, int y) {
        super(province);

        towerPlots.add(new Rectangle(x - size - towerWidth, y - size - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1));
        towerPlots.add(new Rectangle(x + size - towerWidth, y - size - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1));
        towerPlots.add(new Rectangle(x + size - towerWidth, y + size - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1));
        towerPlots.add(new Rectangle(x - size - towerWidth, y + size - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1));

        wallPlots.add(new Rectangle(x - size + towerWidth, y - size - wallWidth, 2 * size - 2 * towerWidth - 1, 2 * wallWidth - 1));
        wallPlots.add(new Rectangle(x - size + towerWidth, y + size - wallWidth, 2 * size - 2 * towerWidth - 1, 2 * wallWidth - 1));
        wallPlots.add(new Rectangle(x - size - wallWidth, y - size + towerWidth, 2 * wallWidth - 1, 2 * size - 2 * towerWidth - 1));
        wallPlots.add(new Rectangle(x + size - wallWidth, y - size + towerWidth, 2 * wallWidth - 1, 2 * size - 2 * towerWidth - 1));

        int numHouses = poissonSample(random, 250);
        for (int j = 0; j < numHouses; j++) {
            int width = 15 + random.nextInt(10);
            int height = 15 + random.nextInt(10);
            double distance = 0;
            while (true) {
                double theta = random.nextDouble() * 360;
                int houseX = x + round(distance * Math.cos(theta) - width / 2.);
                int houseY = y + round(distance * Math.sin(theta) - height / 2.);
                Rectangle h = new Rectangle(houseX, houseY, width, height);
                if (!allPlots().anyMatch(h.expand(1)::intersects)) {
                    housePlots.add(h);
                    break;
                }
                distance += random.nextDouble() * 15;
            }
        }

        for (Rectangle r : housePlots) {
            // double distToCenter2 = (r.centerX() - 512) * (r.centerX() - 512) + (r.centerY() - 512) * (r.centerY() - 512);
            int numFloors = 1 + random.nextInt(3);// + (int) (5 * (random.nextDouble() + 1) / (distToCenter2 * .0001 + 1));
            structurePlans.put(new Vec2d(r.centerX(), r.centerY()), chunk -> new House(chunk, r, numFloors));
        }
        for (Rectangle r : towerPlots) {
            structurePlans.put(new Vec2d(r.centerX(), r.centerY()), chunk -> new Tower(chunk, r));
        }
        for (Rectangle r : wallPlots) {
            for (int i = 0; i < r.w; i += 50) {
                for (int j = 0; j < r.h; j += 50) {
                    Rectangle r2 = new Rectangle(r.x + i, r.y + j, Math.min(r.w - i, 49), Math.min(r.h - j, 49));
                    structurePlans.put(new Vec2d(r2.centerX(), r2.centerY()), chunk -> new Wall(chunk, r2, wallHeight, r.w > r.h));
                }
            }
        }
    }

    private Stream<Rectangle> allPlots() {
        return Stream.of(
                housePlots.stream().map(r -> r.expand(1)),
                towerPlots.stream().map(r -> r.expand(wallBuffer)),
                wallPlots.stream().map(r -> r.expand(wallBuffer))
        ).flatMap(s -> s);
    }

    @Override
    public Stream<Rectangle> flatPlots() {
        return Stream.concat(housePlots.stream(), towerPlots.stream());
    }
}
