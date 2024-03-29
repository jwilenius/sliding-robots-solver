package net.booru.slidingrobots.rank.multidim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiDimRankingTest {
    @Test
    void test4SeparateGroups() {
        final MultiDimRanking<Integer> mdRanker = new MultiDimRanking<>(List.of(
                new Rank<>("R1", d -> d, d -> 0)
        ));

        final List<Integer> unordered = List.of(8, 4, 2, 1);
        final List<Integer> ranked = mdRanker.applyRank(unordered);

        assertEquals(1, ranked.get(0));
        assertEquals(2, ranked.get(1));
        assertEquals(4, ranked.get(2));
        assertEquals(8, ranked.get(3));

        final List<List<Integer>> rankedGroups = mdRanker.getFinalGroups(unordered);
        assertEquals(4, rankedGroups.size());
    }

    @Test
    void test2SeparateGroups() {
        final MultiDimRanking<Integer> mdRanker = new MultiDimRanking<>(List.of(
                new Rank<>("R1", d -> d, d -> 1)
        ));

        //                                      G1    G2
        final List<Integer> unordered = List.of(2, 1, 5, 4);
        final List<Integer> ranked = mdRanker.applyRank(unordered);

        assertEquals(1, ranked.get(0));
        assertEquals(2, ranked.get(1));
        assertEquals(4, ranked.get(2));
        assertEquals(5, ranked.get(3));

        final List<List<Integer>> rankedGroups = mdRanker.getFinalGroups(unordered);
        assertEquals(2, rankedGroups.size());

        assertEquals(List.of(1, 2), rankedGroups.get(0));
        assertEquals(List.of(4, 5), rankedGroups.get(1));
    }

    @Test
    void testGroups1Epsilon() {
        final MultiDimRanking<Entity> mdRanker = new MultiDimRanking<>(List.of(
                new Rank<>("R1", d -> d.value, d -> d * 0.1), // 10% of first, == 10
                new Rank<>("R2", d -> d.value2, d -> 1)       // 1 abs
        ));

        final List<Entity> unordered = List.of(
                new Entity(100, 2),
                new Entity(120, 7),
                new Entity(110, 3),
                new Entity(101, 5),
                new Entity(130, 6)
        );

        final List<Entity> ranked = mdRanker.applyRank(unordered);

        assertEquals(new Entity(100, 2), ranked.get(0));
        assertEquals(new Entity(110, 3), ranked.get(1));
        assertEquals(new Entity(101, 5), ranked.get(2));
        assertEquals(new Entity(130, 6), ranked.get(3));
        assertEquals(new Entity(120, 7), ranked.get(4));

        final List<List<Entity>> rankedGroups0 = mdRanker.getFinalGroups(unordered);
        assertEquals(3, rankedGroups0.size());
        assertEquals(List.of(new Entity(100, 2), new Entity(110, 3)), rankedGroups0.get(0)); // 100 and 110 are within 10%, 2 < 3 and in the same group
        assertEquals(List.of(new Entity(101, 5)), rankedGroups0.get(1));                     // 101 is within 10% but 5 puts it in its own group
        assertEquals(List.of(new Entity(130, 6), new Entity(120, 7)), rankedGroups0.get(2)); // 120,130 are not within 10%, 6 & 7 are the same group
    }

    record Entity(
            double value,
            int value2
    ) {
    }
}