package net.booru.slidingrobots.common;

public enum Direction {
    up,
    down,
    left,
    right;

    public static Direction valueOf(final int dx, final int dy) {
        if (dx == 0 && dy < 0) {
            return up;
        }
        if (dx == 0 && dy > 0) {
            return down;
        }
        if (dx < 0 && dy == 0) {
            return left;
        }
        if (dx > 0 && dy == 0) {
            return right;
        }
        throw new IllegalArgumentException("Unknown direction: " + dx + ", " + dy);
    }
}
