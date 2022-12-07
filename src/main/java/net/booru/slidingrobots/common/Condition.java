package net.booru.slidingrobots.common;

public class Condition {
    public static void require(boolean test) {
        if (!test) {
            throw new IllegalArgumentException("Not valid.");
        }
    }

    public static void require(final boolean test, final String message) {
        if (!test) {
            throw new IllegalArgumentException("Not valid: " + message);
        }
    }
}
