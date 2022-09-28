package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SlidingRobotsSearchAlgorithmTest {

    private static List<SlidingRobotsSearchAlgorithm> getAlgorithms(final Board board) {
        return List.of(
                new BreadthFirstSearchRecursive(board),
                new BreadthFirstSearchIterative(board)
        );
    }

    private Optional<Solution> getSolution(final SlidingRobotsSearchAlgorithm alg, final Game game) {
        try {
            return Optional.of(alg.run(game.getRobotsState(), game.getEndCriteria()));
        } catch (NoSolutionException e) {
            return Optional.empty();
        }
    }

    @Test
    void testNonGreedyFirstPartSolution() {
        final Game game = Game.valueOf("m:8:8,b:1:0,b:4:0,b:5:1,b:3:3,b:3:4,b:3:5,b:6:7,b:7:7,h:3:0,h:5:7,r:6:1,g:0:1");
        final Board board = game.getBoard();

        final List<SlidingRobotsSearchAlgorithm> algorithms = getAlgorithms(board);
        final List<Solution> solutions =
                algorithms.stream()
                .flatMap(alg -> getSolution(alg, game).stream())
                .collect(Collectors.toUnmodifiableList());

        assertEquals(algorithms.size(), solutions.size());

        solutions.sort(Comparator.comparing(s -> s.getSolutionPath().size()));
    }
}