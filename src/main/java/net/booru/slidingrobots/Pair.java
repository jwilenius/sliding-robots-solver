package net.booru.slidingrobots;

import java.util.Objects;

public final class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(T first, U second) {
        preconditionNotNull(first);
        preconditionNotNull(second);

        this.first = first;
        this.second = second;
    }

    private static void preconditionNotNull(final Object object) {
        if (object == null) {
            throw new IllegalArgumentException("argument is null");
        }
    }

    public static <T, U> Pair<T, U> of(T first, U second) {
        return new Pair<>(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Pair<?, ?> pair = (Pair<?, ?>) o;
        return first.equals(pair.first) && second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}