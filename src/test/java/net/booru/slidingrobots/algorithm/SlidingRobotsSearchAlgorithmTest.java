package net.booru.slidingrobots.algorithm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.RobotsStateUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlidingRobotsSearchAlgorithmTest {
    private static final Logger cLogger = LoggerFactory.getLogger(SlidingRobotsSearchAlgorithmTest.class);

    private static void println(Object s) {
        cLogger.debug(s.toString());
    }

    private static void println(String s) {
        cLogger.debug(s);
    }

    private List<TestCase> getTestCasesJson() throws IOException {
        final URL resource = getClass().getClassLoader().getResource("tests100.json");
        final String jsonString = Files.readString(Path.of(resource.getPath()));


        final List<TestCase> testCases = new ObjectMapper().readValue(jsonString, new TypeReference<>() {
        });

        return testCases;
    }

    record TestCase(String map, int optimal, String seed) {
    }

    private static void executeTestForMap(final int movesRequired, final String seed, final Game game, final Function<Board, SlidingRobotsSearchAlgorithm> algorithmFactory) throws NoSolutionException {
        final Board board = game.getBoard();
        println("Map: ");
        println(game.getBoard().printBoard(game.getInitialRobotsState()));

        final Solution solution = executeAlgorithm(game, algorithmFactory, board);
        printSolution(movesRequired, seed, game, solution);

        assertEquals(movesRequired, solution.getStatistics().getSolutionLength(),
                "Wrong number of moves: " + solution.getAlgorithmName() + " for seed = " + seed);
    }

    private static void printSolution(final int movesRequired, final String seed, final Game game, final Solution solution) {
        println("Algorithm : " + solution.getAlgorithmName());
        println("  Seed <" + seed + ">");
        println("  Solution Length = " + solution.getStatistics().getSolutionLength());
        println("  Expected Length: " + movesRequired);
        println("  ");
        println("   " + solution.getStatistics());
        println("  ");
        println("   Solution json: " + solution.toJsonOutputString());
        println("   Solution: ");
        for (var d : RobotsStateUtil.toStringHumanReadable(solution.getSolutionPath())) {
            println("       " + d);
        }
    }

    private static Solution executeAlgorithm(final Game game,
                                             final Function<Board, SlidingRobotsSearchAlgorithm> algorithmFactory,
                                             final Board board)
            throws NoSolutionException {
        return algorithmFactory.apply(board).run(game.getInitialRobotsState(), game.getEndCriteria());
    }

    // ###########################################################################################################

    @Test
    void testSmall() {
        final int movesRequired = 10;
        final Game game = Game.valueOfMap("m:4:4,b:1:0,b:1:1,b:2:1,b:0:3,r:0:0,g:2:0");
        println(game.getBoard().printBoard(game.getInitialRobotsState()));
        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, "No seed", game, getAlgorithmFactory()));
    }

    @Test
    void testSmallOneWay() {
        final int movesRequired = 4;
        final Game game = Game.valueOfMap("m:4:4:oneway,b:1:0,b:1:1,b:2:1,b:0:3,r:0:0,g:2:0");
        println(game.getBoard().printBoard(game.getInitialRobotsState()));
        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, "No seed", game, getAlgorithmFactory()));
    }

    @Test
    void testNoSolution() {
        final int movesRequired = 0;
        final Game game = Game.valueOf2DMap("""
                r . b .
                . . b .
                b b h .
                . . . g
                """);
        println(game.getBoard().printBoard(game.getInitialRobotsState()));
        assertThrows(NoSolutionException.class,
                () -> executeTestForMap(movesRequired, "No seed", game, getAlgorithmFactory()));
    }

    @Test
    void testNonGreedyFirstPartSolution() {
        final int movesRequired = 13;
        final Game game = Game.valueOfMap("m:8:8,b:1:0,b:4:0,b:5:1,b:3:3,b:3:4,b:3:5,b:6:7,b:7:7,h:3:0,h:5:7,r:6:1,g:0:1");
        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, "No seed", game, getAlgorithmFactory()));
        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, "No seed", game, getAlgorithmFactory()));
    }

    @Test
    void test7Moves() {
        final int movesRequired = 7;
        final String seed = "seed:8:8:EO8T-0MXD";
        final Game game = Game.valueOfMap("map:8:8:helper_robot:3:0:helper_robot:2:1:blocker:6:1:blocker:7:1:blocker:6:2:main_robot:2:3:blocker:7:4:blocker:6:5:blocker:1:6:goal:6:6:blocker:2:7:blocker:3:7");

        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, seed, game, getAlgorithmFactory()));
        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, seed, game, getAlgorithmFactory()));
    }

    @Test
    void testLevel5Puzzle5() {
        final int movesRequired = 15;
        final String seed = "seed:8:8:EO8T-0MXD";
        final Game game = Game.valueOfMap("m:8:8,b:4:0,b:5:0,b:5:1,b:3:3,b:5:4,b:0:5,b:1:5,b:2:5,h:6:4,h:2:6,r:6:2,g:2:3");

        // 4 bump complexity, many intersecting paths
        assertDoesNotThrow(
                () -> executeTestForMap(movesRequired, seed, game, getAlgorithmFactory()));
    }

    @Test
    void
    test100FromFileSpecificInstanceA() throws IOException {
        final String specificSeed = "seed:9:8:Z8GZ-4KCM";
        final List<TestCase> testCases = getTestCasesJson();
        TestCase testCase = testCases.stream().filter(t -> t.seed.equals(specificSeed)).findFirst().orElseThrow();

        final Timer timer = new Timer();
        final Game game = Game.valueOfMap(testCase.map);
        assertDoesNotThrow(
                () -> executeTestForMap(testCase.optimal, testCase.seed, game, getAlgorithmFactory()));

        timer.stop();
        println(timer);
    }

    @Test
    void
    test100FromFileSpecificInstanceOneWay() throws IOException {
        final String specificSeed = "seed:10:8:oneway:FI3W-IHY3";
        final List<TestCase> testCases = getTestCasesJson();
        TestCase testCase = testCases.stream().filter(t -> t.seed.equals(specificSeed)).findFirst().orElseThrow();

        final Timer timer = new Timer();
        final Game game = Game.valueOfMap(testCase.map);
        assertDoesNotThrow(
                () -> executeTestForMap(testCase.optimal, testCase.seed, game, getAlgorithmFactory()));

        timer.stop();
        println(timer);
    }

    @Test
    void test100FromFile() throws IOException {
        final List<TestCase> testCases = getTestCasesJson();

        final Timer timer = new Timer();
        int testCaseCount = 0;

        for (final TestCase testCase : testCases) {
            println("\n##################################################");
            println("Test #" + testCaseCount++);
            final Game game = Game.valueOfMap(testCase.map);
            assertDoesNotThrow(
                    () -> executeTestForMap(testCase.optimal, testCase.seed, game, getAlgorithmFactory()));
        }

        timer.stop();
        println(timer);
    }

    private static Function<Board, SlidingRobotsSearchAlgorithm> getAlgorithmFactory() {
        return board -> new BreadthFirstSearchIterative(board, -1);
    }

}