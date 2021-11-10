package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.BreadthFirstSearchRecursive;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.SlidingRobotsSearchAlgorithm;
import net.booru.slidingrobots.algorithm.Solution;
import net.booru.slidingrobots.common.ArgumentParser;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.MapStringGenerator;
import net.booru.slidingrobots.state.RobotsState;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class Main {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void main(String[] args) throws IOException {
        final String exampleMap = "map:8:8:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:" +
                                  "helper_robot:3:4:helper_robot:4:1:blocker:4:6:main_robot:5:5:blocker:5:7:" +
                                  "blocker:7:0:goal:6:7";

        final var argumentParser = new ArgumentParser()
                .withSpecificArgument("--alg", List.of("i", "r"), "iterative or recursive solver")
                .withSpecificArgument("--verbose", List.of(), "print board solution verbose")
                .withGeneralArgument("--solve", List.of("<map-string>"),
                        "Solve the provided map.\n       example: " + exampleMap)
                .withGeneralArgument("--profile", List.of("<runs count>"),
                                     "Generate random maps and calculate average time. A value of 0 means infinite, " +
                                     "no maps are saved.")
                .setRequired(List.of("--alg"))
                .addConflicts("--solve", List.of("--profile"))
                .addConflicts("--profile", List.of("--solve"));

        argumentParser.parseArguments(args);

        var alg = argumentParser.get("--alg");
        var solve = argumentParser.get("--solve");
        var profile = argumentParser.get("--profile");
        var verbose = argumentParser.get("--verbose");

        // alg.isPresent since required
        // solve and profile are mutually exclusive by definition above

        if (solve.isPresent()) {
            singleRun(alg.get().getValue(), solve.get().getValue(), verbose.isPresent());
            System.exit(1);
        }

        if (profile.isPresent()) {
            multiRun(alg.get().getValue(), profile.get().getValueAsInt());
            System.exit(1);
        }

        // fallback no args, run example and print help
        argumentParser.outputHelp();
        System.out.println("--------------------------------");
        System.out.println("Now we will run an example problem:");
        singleRun(alg.get().getValue(), exampleMap, true);

        System.exit(1);
    }

    private static SlidingRobotsSearchAlgorithm chooseAlgorithm(final String algorithmType, final Board board) {
        if (algorithmType.equals("i")) {
            return new BreadthFirstSearchIterative(board);
        }
        if (algorithmType.equals("r")) {
            return new BreadthFirstSearchRecursive(board);
        }
        throw new IllegalArgumentException("Expected algorithm 'i' or 'r'");
    }

    private static void singleRun(final String algorithmType, final String mapString, final boolean isVerbose) {

        final Game game = Game.valueOf(mapString);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getRobotsState();

        if (isVerbose) {
            System.out.println("Map-string:\n" + mapString);
            System.out.println(board.printBoard(robotsState));
        }

        try {
            final SlidingRobotsSearchAlgorithm searchAlgorithm = chooseAlgorithm(algorithmType, board);
            final Solution solution = searchAlgorithm.run(robotsState, game.getEndCriteria());

            if (isVerbose) {
                System.out.println(solution.toStringVerbose());
                int i = 0;
                for (RobotsState s : solution.getSolutionPath()) {
                    System.out.println("Board i=" + (i++));
                    System.out.println(board.printBoard(s));
                }
            } else {
                System.out.println(solution.toJsonOutputString());
            }

        } catch (NoSolutionException e) {
            System.out.println("No solution");
        }
    }

    /**
     * For getting stats on average speed and running profilers
     */
    private static void multiRun(final String algorithmType, final int runCountArgument) throws IOException {

        final boolean isSaveMapStrings = runCountArgument != 0;
        final int actualRunCount = runCountArgument == 0 ? Integer.MAX_VALUE : runCountArgument;

        final List<String> mapStrings = new LinkedList<>();
        double time = 0;
        int noSolutionCount = 0;
        for (int i = 0; i < actualRunCount; i++) {
            final String mapString = MapStringGenerator.generate(8);
            if (isSaveMapStrings) {
                mapStrings.add(mapString);
            }

            final Game game = Game.valueOf(mapString);
            try {
                final Timer t = new Timer();
                final SlidingRobotsSearchAlgorithm searchAlgorithm = chooseAlgorithm(algorithmType, game.getBoard());
                final Solution solution = searchAlgorithm.run(game.getRobotsState(), game.getEndCriteria());
                t.close();
                time += t.getDurationMillis();
            } catch (NoSolutionException e) {
                noSolutionCount++;
            }
        }

        if (isSaveMapStrings) {
            final Path outputDir = Path.of("junk/");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            Files.write(Path.of("junk/maps.txt"), mapStrings, Charset.defaultCharset());
            System.out.println("Maps dumped to file junk/maps.txt");
        }
        System.out.println("Total run count =   " + actualRunCount);
        System.out.println("  no solution # =   " + noSolutionCount);
        System.out.println("Total time (ms) =   " + time);
        System.out.println("Average time (ms) = " + (time / mapStrings.size()));
    }
}
