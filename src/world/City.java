package world;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import static util.math.MathUtils.poissonSample;
import static util.math.MathUtils.round;
import world.structures.House;
import world.structures.Rectangle;
import world.structures.Structure;
import world.structures.Tower;
import world.structures.Wall;

public class City extends Structure {

    private static final int x1 = 256, x2 = 256 + 512, y1 = 256, y2 = 256 + 512;
    private static final int towerWidth = 10, wallWidth = 4, wallHeight = 20;
    private static final int wallBuffer = 4;

    private List<Rectangle> housePlots = new ArrayList();
    private List<Rectangle> towerPlots = new ArrayList();
    private List<Rectangle> wallPlots = new ArrayList();

    public City(World world, int x, int y) {
        super(world);

        towerPlots.add(new Rectangle(x1 - towerWidth, y1 - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1).expand(wallBuffer));
        towerPlots.add(new Rectangle(x2 - towerWidth, y1 - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1).expand(wallBuffer));
        towerPlots.add(new Rectangle(x2 - towerWidth, y2 - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1).expand(wallBuffer));
        towerPlots.add(new Rectangle(x1 - towerWidth, y2 - towerWidth, 2 * towerWidth - 1, 2 * towerWidth - 1).expand(wallBuffer));

        wallPlots.add(new Rectangle(x1 + towerWidth, y1 - wallWidth, x2 - x1 - 2 * towerWidth - 1, 2 * wallWidth - 1).expand(wallBuffer));
        wallPlots.add(new Rectangle(x1 + towerWidth, y2 - wallWidth, x2 - x1 - 2 * towerWidth - 1, 2 * wallWidth - 1).expand(wallBuffer));
        wallPlots.add(new Rectangle(x1 - wallWidth, y1 + towerWidth, 2 * wallWidth - 1, y2 - y1 - 2 * towerWidth - 1).expand(wallBuffer));
        wallPlots.add(new Rectangle(x2 - wallWidth, y1 + towerWidth, 2 * wallWidth - 1, y2 - y1 - 2 * towerWidth - 1).expand(wallBuffer));

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
    }

    public Stream<Rectangle> allPlots() {
        return Stream.of(
                housePlots.stream().map(r -> r.expand(1)),
                towerPlots.stream().map(r -> r.expand(wallBuffer)),
                wallPlots.stream().map(r -> r.expand(wallBuffer))
        ).flatMap(s -> s);
    }

    public Stream<Rectangle> flatPlots() {
        return Stream.concat(housePlots.stream(), towerPlots.stream());
    }

    public List<Structure> getStructures() {
        List<Structure> structures = new LinkedList();
        for (Rectangle r : housePlots) {
            structures.add(new House(world, r));
        }
        for (Rectangle r : towerPlots) {
            structures.add(new Tower(world, r.expand(-wallBuffer)));
        }
        for (Rectangle r : wallPlots) {
            structures.add(new Wall(world, r.expand(-wallBuffer), wallHeight, r.w > r.h));
        }
        return structures;
    }
}
