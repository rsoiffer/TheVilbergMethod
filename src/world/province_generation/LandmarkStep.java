package world.province_generation;

import java.util.LinkedList;
import java.util.List;
import world.landmarks.City;
import world.landmarks.Landmark;
import world.regions.GenerationStep;
import world.regions.Province;

public class LandmarkStep extends GenerationStep<Province> {

    public final List<Landmark> landmarks = new LinkedList();

    public LandmarkStep(Province region) {
        super(region);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generate() {
        landmarks.add(new City(region, region.worldX() + 512, region.worldY() + 512));
    }
}
