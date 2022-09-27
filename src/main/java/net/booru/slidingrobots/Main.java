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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class Main {
    private static final Logger cLogger = LoggerFactory.getLogger(Main.class);

    public static final String ARG_ALG = "--alg";
    public static final String ARG_SOLVE = "--solve";
    public static final String ARG_VERBOSE = "--verbose";
    public static final String ARG_PROFILE = "--profile";

    public static void main(String[] args) throws IOException {
        final String exampleMap = "map:8:8:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:" +
                "helper_robot:3:4:helper_robot:4:1:blocker:4:6:main_robot:5:5:blocker:5:7:" +
                "blocker:7:0:goal:6:7";

        final String exampleMapCompact =
                "m:8:8,b:0:0,b:0:4,b:2:4,b:2:5,b:3:1,h:3:4,h:4:1,b:4:6,r:5:5,b:5:7,b:7:0,g:6:7";

        final var argumentParser = new ArgumentParser()
                .withSpecificArgument(ARG_ALG, List.of("i", "r"), "iterative or recursive solver")
                .withSpecificArgument(ARG_VERBOSE, List.of("0", "1"), "print board solution verbose")
                .withGeneralArgument(ARG_SOLVE, List.of("<map-string>"),
                        "Solve the provided map.\n" +
                                "       example: " + exampleMap + "\n" +
                                "       example: " + exampleMapCompact + "\n\n"
                )
                .withGeneralArgument(ARG_PROFILE, List.of("<runs count>"),
                        "Generate random maps and calculate average time. A value of 0 means infinite, " +
                                "no maps are saved.")
                .setRequired(List.of(ARG_ALG))
                .addConflicts(ARG_SOLVE, List.of(ARG_PROFILE))
                .addConflicts(ARG_PROFILE, List.of(ARG_SOLVE));

        argumentParser.parseArguments(args);

        final ArgumentParser.Argument algorithm = argumentParser.get(ARG_ALG).orElseThrow(
                () -> new IllegalArgumentException("missing " + ARG_ALG));
        final var solve = argumentParser.get(ARG_SOLVE);
        final var profile = argumentParser.get(ARG_PROFILE);
        final int verboseLevel = argumentParser.get(ARG_VERBOSE)
                .map(ArgumentParser.Argument::getValueAsInt)
                .orElse(-1);

        // solve and profile are mutually exclusive by definition above
        if (solve.isPresent()) {
            singleRun(algorithm.getValue(), solve.get().getValue(), verboseLevel);
            System.exit(1);
        }

        if (profile.isPresent()) {
            multiRun(algorithm.getValue(), profile.get().getValueAsInt());
            System.exit(1);
        }

        // fallback no args, run example and print help
        argumentParser.outputHelp();
        cLogger.info("--------------------------------");
        cLogger.info("Now we will run an example problem:");
        singleRun(algorithm.getValue(), exampleMap, verboseLevel);

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

    private static void singleRun(final String algorithmType, final String mapString, final int verboseLevel) {
        final boolean isVerbose = verboseLevel >= 0;
        final Game game = Game.valueOf(mapString);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getRobotsState();

        if (isVerbose) {
            cLogger.info("");
            cLogger.info("------------------------------------------------");
            cLogger.info("Map-string: {}", mapString);
            board.boardToLogLines(robotsState).forEach(cLogger::info);
            cLogger.info("------------------------------------------------");
            cLogger.info("");
        }

        try {
            final SlidingRobotsSearchAlgorithm searchAlgorithm = chooseAlgorithm(algorithmType, board);
            final Solution solution = searchAlgorithm.run(robotsState, game.getEndCriteria());

            if (isVerbose) {
                solution.toStringVerbose(verboseLevel).forEach(cLogger::info);

                if (verboseLevel >= 1) {
                    int i = 0;
                    for (RobotsState s : solution.getSolutionPath()) {
                        cLogger.info("Board i={}", i++);
                        board.boardToLogLines(s).forEach(cLogger::info);
                    }
                }
            } else {
                cLogger.info(solution.toJsonOutputString());
            }

        } catch (NoSolutionException e) {
            cLogger.info("No solution");
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
                searchAlgorithm.run(game.getRobotsState(), game.getEndCriteria());
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
            cLogger.info("Maps dumped to file junk/maps.txt");
        }
        cLogger.info("Total run count =   {}", actualRunCount);
        cLogger.info("  no solution # =   {}", noSolutionCount);
        cLogger.info("Total time (ms) =   {}", time);
        cLogger.info("Average time (ms) = {}", (time / mapStrings.size()));
    }
}
