package net.booru.slidingrobots.rank;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.state.Game;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BumpsCounterTest {

    @Test
    void testBumps() throws NoSolutionException {
        final Game game = Game.valueOf2DMap("""
                . . . . . . b .
                . r . . b b . .
                b . . b . . . .
                h . . . . . b .
                . . . . h . . .
                . . . . . . . .
                . . . . . . g .
                b . . b . . . .
                """);

        final Solution solution = new BreadthFirstSearchIterative(game.getBoard())
                .run(game.getInitialRobotsState(), game.getEndCriteria());

        final int bumps = new BumpsCounter().apply(solution);
        assertEquals(6, bumps);
    }
}