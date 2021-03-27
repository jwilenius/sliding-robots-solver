package net.booru.slidingrobots;

import java.time.Duration;
import java.time.Instant;

public class Timer {
    private final Instant iStart = Instant.now();
    private Duration iDurationMillis;

    public void close() {
        iDurationMillis = Duration.between(iStart, Instant.now());
    }

    public long getDurationMillis() {
        return iDurationMillis.toMillis();
    }

    @Override
    public String toString() {
        return String.format("Time %f (ms)", getDurationMillis());
    }
}
