package world.chunk_generation;

import java.util.LinkedList;
import java.util.List;
import world.regions.Chunk;
import world.regions.GenerationStep;
import world.regions.RegionPos;
import world.structures.Structure;

public class StructureValidationStep extends GenerationStep<Chunk> {

    public final List<Structure> validStructures = new LinkedList();

    public StructureValidationStep(Chunk region) {
        super(region);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void generate() {
        List<Structure> allStructures = new LinkedList();
        for (RegionPos rp : region.pos.nearby(1)) {
            allStructures.addAll(world.chunkManager.get(rp, StructureStep.class).structures);
        }
        for (Structure s : region.require(StructureStep.class).structures) {
            boolean keep = true;
            for (Structure s2 : allStructures) {
                if (s2.priority > s.priority && s2.intersects(s)) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                validStructures.add(s);
            }
        }
    }
}
