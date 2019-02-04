package fluids;

import engine.Behavior;
import engine.Core;
import engine.Layer;
import static engine.Layer.RENDER3D;

public class FluidManager extends Behavior {

    private VectorField velocity = new VectorField(3, 3);
    private VectorField density = new VectorField(3, 1);
    private double timer = 0;

    @Override
    public Layer layer() {
        return RENDER3D;
    }

    @Override
    public void step() {
        timer -= Core.dt();
        if (timer < 0) {
            timer += .1;
            velocity.advect(velocity.copy(), .1);
        }
    }
}
