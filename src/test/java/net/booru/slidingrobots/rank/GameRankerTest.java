package net.booru.slidingrobots.rank;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.state.Game;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRankerTest {

    private static Solution solve(final Game game) throws NoSolutionException {
        return new BreadthFirstSearchIterative(game.getBoard())
                .run(game.getInitialRobotsState(), game.getEndCriteria());
    }

    @Test
    void testRank1() throws NoSolutionException {
        final Game game1 = Game.valueOf2DMap("""
                . . . . . . b .
                . r . . b b . .
                b . . b . . . .
                h . . . . . b .
                . . . . h . . .
                . . . . . . . .
                . . . . . . g .
                b . . b . . . .
                """);

        final Solution solution1 = solve(game1);

        final Game game2 = Game.valueOfMap("m:4:4:oneway,b:1:0,b:1:1,b:2:1,b:0:3,r:0:0,g:2:0");
        final Solution solution2 = solve(game2);

        final List<GameWithSolution> ranked = new GameRanker(

        ).apply(List.of(game1, game2), List.of(solution1, solution2));
        assertEquals(game2, ranked.get(0).game());
        assertEquals(game1, ranked.get(1).game());
    }
}