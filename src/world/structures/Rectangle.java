package world.structures;

import util.math.Vec2d;

public class Rectangle {

    public final int x, y, w, h;

    public Rectangle(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int centerX() {
        return x + w / 2;
    }

    public int centerY() {
        return y + h / 2;
    }

    public double distanceTo(Vec2d pos) {
        Vec2d pos2 = pos.clamp(new Vec2d(x, y), new Vec2d(maxX(), maxY()));
        return pos.sub(pos2).length();
    }

    public Rectangle expand(int amt) {
        return new Rectangle(x - amt, y - amt, w + 2 * amt, h + 2 * amt);
    }

    public boolean intersects(Rectangle other) {
        return !(x > other.maxX() || y > other.maxY() || other.x > maxX() || other.y > maxY());
    }

    public int maxX() {
        return x + w;
    }

    public int maxY() {
        return y + h;
    }

    @Override
    public String toString() {
        return "Rectangle{" + "x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + '}';
    }
}
