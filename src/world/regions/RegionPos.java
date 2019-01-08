package world.regions;

import java.util.LinkedList;
import java.util.List;

public class RegionPos {

    public final int x, y;

    public RegionPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double distance(RegionPos other) {
        return Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y));
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
        final RegionPos other = (RegionPos) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + this.x;
        hash = 17 * hash + this.y;
        return hash;
    }

    public List<RegionPos> nearby(int dist) {
        List<RegionPos> r = new LinkedList();
        for (int i = -dist; i <= dist; i++) {
            for (int j = -dist; j <= dist; j++) {
                r.add(new RegionPos(x + i, y + j));
            }
        }
        return r;
    }

    @Override
    public String toString() {
        return "RegionPos{" + "x=" + x + ", y=" + y + '}';
    }
}
