package fluids;

import java.util.Arrays;
import util.math.MathUtils;

public class Vector {

    public final int dims;
    private final double[] components;

    public Vector(int dims) {
        this.dims = dims;
        components = new double[dims];
    }

    public void add(Vector other) {
        assertDimensionsMatch(dims, other.dims);
        for (int i = 0; i < dims; i++) {
            set(i, get(i) + other.get(i));
        }
    }

    public Vector append(double value) {
        Vector v = new Vector(dims + 1);
        for (int i = 0; i < dims; i++) {
            v.set(i, get(i));
        }
        v.set(dims, value);
        return v;
    }

    public static void assertDimensionsMatch(int... dimsArray) {
        if (dimsArray == null || dimsArray.length < 2) {
            throw new RuntimeException("Bad input");
        }
        int desiredDimension = dimsArray[0];
        for (int i = 1; i < dimsArray.length; i++) {
            if (dimsArray[i] != desiredDimension) {
                throw new RuntimeException("Dimension mismatch: " + Arrays.toString(dimsArray));
            }
        }
    }

    public Vector copy() {
        Vector v = new Vector(dims);
        for (int i = 0; i < dims; i++) {
            v.set(i, get(i));
        }
        return v;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Vector other = (Vector) obj;
        if (this.dims != other.dims) {
            return false;
        }
        if (!Arrays.equals(this.components, other.components)) {
            return false;
        }
        return true;
    }

    public double get(int index) {
        return components[index];
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.dims;
        hash = 59 * hash + Arrays.hashCode(this.components);
        return hash;
    }

    public void lerp(Vector other, double amt) {
        assertDimensionsMatch(dims, other.dims);
        for (int i = 0; i < dims; i++) {
            set(i, MathUtils.lerp(get(i), other.get(i), amt));
        }
    }

    public void mul(double d) {
        for (int i = 0; i < dims; i++) {
            set(i, get(i) * d);
        }
    }

    public void set(int index, double value) {
        components[index] = value;
    }
}
