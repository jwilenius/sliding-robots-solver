package net.booru.slidingrobots.common;

import java.util.Objects;

public class Point {
    private final int iX;
    private final int iY;

    public Point(final int x, final int y) {
        this.iX = x;
        this.iY = y;
    }

    public int getX() {
        return iX;
    }

    public int getY() {
        return iY;
    }

    @Override
    public String toString() {
        return "(" + iX + ',' + iY + ')';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point)) {
            return false;
        }
        final Point point = (Point) o;
        return iX == point.iX && iY == point.iY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(iX, iY);
    }
}
