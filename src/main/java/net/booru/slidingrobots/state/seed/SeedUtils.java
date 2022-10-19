package net.booru.slidingrobots.state.seed;

import java.util.Arrays;
import java.util.Random;

public class SeedUtils {
    public static final Random RANDOM = new Random();

    // Compatible with preexisting implentations
    public static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    // Compatible with preexisting implentations
    public static final int SEED_LENGTH = 9;

    private SeedUtils() {
    }

    /**
     * Generate a map seed that is compatible with the erlang backed.
     *
     * @param dimX     size of the map to generate in x dim
     * @param dimY     size of the map to generate in y dim
     * @param isOneWay true if we want to generate a game map that is one way.
     * @return the seed string that us used to generate a map
     */
    public static String generateSeedString(final int dimX, final int dimY, final boolean isOneWay) {
        final char[] seedString = new char[SEED_LENGTH];
        for (int i = 0; i < SEED_LENGTH; i++) {
            if (i == 4) {
                seedString[i] = '-';
            } else {
                final int pos = RANDOM.nextInt(ALPHABET.length());
                seedString[i] = ALPHABET.charAt(pos);
            }
        }

        return new Seed(dimX, dimY, isOneWay, new String(seedString), 0).toString();
    }

    /**
     * Parse a seed string into a seed object
     *
     * @param seedString the seed string
     * @return a {@link Seed} from the the {@code seedString}
     */
    public static Seed parseSeedString(final String seedString) {
        final String[] splitSeedString = seedString.split(":");
        final boolean isOneWay = Arrays.asList(splitSeedString).contains("oneway");
        final int offset = isOneWay ? 1 : 0;
        final int dimX = Integer.parseInt(splitSeedString[1 + offset]);
        final int dimY = Integer.parseInt(splitSeedString[2 + offset]);

        final String actualSeedString = splitSeedString[splitSeedString.length - 1];
        final int hash = SeedUtils.seedStringHash(actualSeedString);

        return new Seed(dimX, dimY, isOneWay, actualSeedString, hash);
    }

    /**
     * Generate the next pseudo random number from a seed, in the range 0 inclusive and bound exclusive.
     * <br>
     * Note: This is the xorshift32 impl that is used in the preexisting implementations.
     *
     * @param seed  the seed to start from
     * @param bound singed upper bound, noninclusive
     * @return a tuple [X, newSeed], where X is a random number in the range [0, bound)
     */
    public static XorShiftResult xorshift32(int seed, int bound) {
        //@formatter:off
        final int seed0Shift = seed << 13;
        final int seed1      = seed ^ seed0Shift;
        final int seed1Shift = seed1 >> 17;
        final int seed2      = seed1 ^ seed1Shift;
        final int seed2Shift = seed2 << 5;
        final int seed3      = seed2 ^ seed2Shift;
        //@formatter:on

        if (seed3 < 0) {
            return new XorShiftResult(-seed3 % bound, seed3);
        }

        return new XorShiftResult(seed3 % bound, seed3);
    }

    public record XorShiftResult(int randomNumber, int seed) {
    }

    /**
     * Compute hash of a seed string from the erlang backend.
     *
     * @param seedString a seed string, e.g. ABCD-EFGH
     * @return an integer hash of the seed string
     */
    public static int seedStringHash(final String seedString) {
        final byte[] bytes = seedString.getBytes();
        int hash = 0;
        for (int i = 0; i < bytes.length; i++) {
            hash = (hash << 5) - hash + bytes[i];
        }

        return hash;
    }
}
