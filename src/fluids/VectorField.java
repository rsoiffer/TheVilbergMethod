package fluids;

import static fluids.Vector.assertDimensionsMatch;
import java.util.HashMap;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.floor;

public class VectorField {

    public final int inDims, outDims;
    private final HashMap<Vector, Vector> data = new HashMap();

    public VectorField(int inDims, int outDims) {
        this.inDims = inDims;
        this.outDims = outDims;
    }

    public void advect(VectorField velocity, double dt) {
        assertDimensionsMatch(inDims, velocity.inDims, velocity.outDims);
        for (Vector pos : data.keySet()) {
            Vector vel = velocity.interp(pos);
            vel.mul(-dt);
            vel.add(pos);
            set(pos, interp(vel));
        }
    }

    public VectorField copy() {
        VectorField vf = new VectorField(inDims, outDims);
        for (Vector pos : data.keySet()) {
            vf.set(pos, get(pos));
        }
        return vf;
    }

    public VectorField divergence(double dt) {
        VectorField vf = new VectorField(inDims, outDims);
        for (Vector pos : data.keySet()) {
            Vector qPos = pos.copy();
            Vector newVal = new Vector(outDims);
            for (int i = 0; i < inDims; i++) {
                qPos.set(i, pos.get(i) + 1);
                newVal.add(get(qPos));
                qPos.set(i, pos.get(i) - 1);
                newVal.mul(-1);
                newVal.add(get(qPos));
                newVal.mul(-1);
                qPos.set(i, pos.get(i));
            }
            newVal.mul(-2 / dt);
            vf.set(pos, newVal);
        }
        return vf;
    }

    public Vector get(Vector pos) {
        assertDimensionsMatch(inDims, pos.dims);
        if (data.containsKey(pos)) {
            return data.get(pos).copy();
        } else {
            return new Vector(outDims);
        }
    }

    public Vector interp(Vector pos) {
        assertDimensionsMatch(inDims, pos.dims);
        VectorField vf = this;
        for (int i = pos.dims - 1; i >= 0; i--) {
            vf = new InterpSlice(vf, pos.get(i));
        }
        return vf.get(new Vector(0));
    }

    public void set(Vector pos, Vector value) {
        assertDimensionsMatch(inDims, pos.dims);
        assertDimensionsMatch(outDims, value.dims);
        data.put(pos, value);
    }

    private static class InterpSlice extends VectorField {

        private final VectorField parent;
        private final double slicePos;

        public InterpSlice(VectorField parent, double slicePos) {
            super(parent.inDims - 1, parent.outDims);
            this.parent = parent;
            this.slicePos = slicePos;
        }

        @Override
        public Vector get(Vector pos) {
            assertDimensionsMatch(inDims, pos.dims);
            Vector v0 = parent.get(pos.append(floor(slicePos)));
            Vector v1 = parent.get(pos.append(ceil(slicePos)));
            v0.lerp(v1, slicePos - floor(slicePos));
            return v0;
        }
    }
}
