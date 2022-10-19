package net.booru.slidingrobots.state.seed;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedUtilsTest {

    @Test
    void testXorshiftMatchesErlangImpl() {
        var input0 = List.of(10, 32533, 32533, 1, 131072, 131071, 122880);
        var input1 = List.of(100, 100, 10, 10, 1, 1, 1);
        var expected = List.of(
                new SeedUtils.XorShiftResult(90, 2703690),
                new SeedUtils.XorShiftResult(72, -205929372),
                new SeedUtils.XorShiftResult(2, -205929372),
                new SeedUtils.XorShiftResult(9, 270369),
                new SeedUtils.XorShiftResult(0, 1078337569),
                new SeedUtils.XorShiftResult(0, -1069678592),
                new SeedUtils.XorShiftResult(0, -1136771584)
        );

        for (int i = 0; i < input0.size(); i++) {
            System.out.println("test-" + i);
            final var a = SeedUtils.xorshift32(input0.get(i), input1.get(i));
            System.out.println(a);
            assertEquals(expected.get(i), a);
        }
    }
}