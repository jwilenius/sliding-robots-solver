package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.SlidingRobotsSearchAlgorithm;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.ArgumentParser;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.MapStringGenerator;
import net.booru.slidingrobots.state.RobotsState;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger cLogger = LoggerFactory.getLogger(Main.class);

    public static final String ADDITIONAL_DEPTH = "--solutionDepth";
    public static final String ARG_SOLVE = "--solve";
    public static final String ARG_VERBOSE = "--verbose";
    public static final String ARG_PROFILE = "--profile";
    public static final String ARG_PROFILE_MAPS = "--profile-maps";

    public static void main(String[] args) throws IOException {
        final String exampleMap = "map:8:8:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:" +
                "helper_robot:3:4:helper_robot:4:1:blocker:4:6:main_robot:5:5:blocker:5:7:" +
                "blocker:7:0:goal:6:7";

        final String exampleMapCompact =
                "m:8:8,b:0:0,b:0:4,b:2:4,b:2:5,b:3:1,h:3:4,h:4:1,b:4:6,r:5:5,b:5:7,b:7:0,g:6:7";

        final var argumentParser = new ArgumentParser()
                .withGeneralArgument(ADDITIONAL_DEPTH, "-1", List.of("<n>"), "Keep solution of additional depth best + n")
                .withSpecificArgument(ARG_VERBOSE, "-1", List.of("0", "1"), "print board solution verbose")
                .withGeneralArgument(ARG_SOLVE, null, List.of("<map-string>"),
                        """
                                Solve the provided map.
                                               example: %s
                                               example: %s"""
                                .formatted(exampleMap, exampleMapCompact))
                .withGeneralArgument(ARG_PROFILE, null, List.of("<runs count>"),
                        """                                
                                Generate random maps and calculate average time.
                                               A value of <runs count> must be greater than 0 unless %s is provided."""
                                .formatted(ARG_PROFILE_MAPS))
                .withGeneralArgument(ARG_PROFILE_MAPS, "", List.of("<path/file>"),
                        """
                                A file with maps, one per line. Format '<map><space><moveCount>'
                                               Use this map file when %s is used and calculate statistics.
                                               If <runs count> is greater than 0, then the number of rows from the map file what will be run is limited by that number."""
                                .formatted(ARG_PROFILE))
                .addConflicts(ARG_SOLVE, List.of(ARG_PROFILE))
                .addConflicts(ARG_PROFILE, List.of(ARG_SOLVE));

        argumentParser.parseArguments(args);

        final var solutionDepth = argumentParser.get(ADDITIONAL_DEPTH);
        final var solve = argumentParser.get(ARG_SOLVE);
        final var profile = argumentParser.get(ARG_PROFILE);
        final var profileMaps = argumentParser.get(ARG_PROFILE_MAPS);
        final var verboseLevel = argumentParser.get(ARG_VERBOSE);

        // solve and profile are mutually exclusive by definition above
        if (solve.isPresent()) {
            singleRun(solutionDepth.get().getValueAsInt(), solve.get().getValue(), verboseLevel.get().getValueAsInt());
            System.exit(1);
        }

        if (profile.isPresent()) {
            profileRun(solutionDepth.get().getValueAsInt(), profile.get().getValueAsInt(), profileMaps.get().getValue());
            System.exit(1);
        }

        // fallback no args, run example and print help
        argumentParser.outputHelp();
        cLogger.info("--------------------------------");
        cLogger.info("Now we will run an example problem:");
        singleRun(solutionDepth.get().getValueAsInt(), exampleMap, Math.max(0, verboseLevel.get().getValueAsInt()));

        System.exit(1);
    }

    private static void singleRun(final int solutionDepth, final String mapString, final int verboseLevel) {
        final boolean isVerbose = verboseLevel >= 0;
        final Game game = Game.valueOf(mapString);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getInitialRobotsState();

        if (isVerbose) {
            cLogger.info("");
            cLogger.info("------------------------------------------------");
            cLogger.info("Map-string: {}", mapString);
            board.boardToLogLines(robotsState).forEach(cLogger::info);
            cLogger.info("------------------------------------------------");
            cLogger.info("");
        }

        try {
            final SlidingRobotsSearchAlgorithm searchAlgorithm = getSearchAlgorithm(solutionDepth, board);
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

    private static SlidingRobotsSearchAlgorithm getSearchAlgorithm(final int solutionDepth, final Board board) {
        return new BreadthFirstSearchIterative(board, solutionDepth);
    }

    /**
     * For getting stats on average speed and running profilers
     */
    private static void profileRun(final int solutionDepth, final int runCount, final String mapsFile) throws IOException {
        cLogger.info("Running statistics gathering. runs = {}", runCount);
        final DescriptiveStatistics timeStats = new DescriptiveStatistics(runCount);

        final boolean isSaveMapStrings = runCount != 0 && mapsFile.isEmpty();

        final List<String> mapStrings = new ArrayList<>(runCount);
        final List<Integer> mapMoves = new ArrayList<>(runCount);
        int noSolutionCount = 0;

        if (mapsFile.isEmpty()) {
            for (int i = 0; i < runCount; i++) {
                mapStrings.add(MapStringGenerator.generate(8));
            }
        } else {
            // map file has format "<mapString><space><moveCount>\n"
            Files.readAllLines(Path.of(mapsFile)).forEach(map -> mapStrings.add(map.split(" ")[0]));
        }

        final List<String> mapStringsToDump = new ArrayList<>(runCount);
        final int actualRunCount =
                runCount == 0
                        ? mapStrings.size()
                        : Math.min(runCount, mapStrings.size());

        for (int i = 0; i < actualRunCount; i++) {
            final Game game = Game.valueOf(mapStrings.get(i));
            try {
                final Timer timer = new Timer();
                final SlidingRobotsSearchAlgorithm searchAlgorithm = getSearchAlgorithm(solutionDepth, game.getBoard());
                final Solution solution = searchAlgorithm.run(game.getInitialRobotsState(), game.getEndCriteria());
                timer.stop();
                final double time = timer.getDurationMillis();
                timeStats.addValue(time);
                if (i % 20 == 0) {
                    cLogger.info("Run {} took {} ms", i, time);
                }
                mapMoves.add(solution.getStatistics().getSolutionLength());
                mapStringsToDump.add(mapStrings.get(i));
            } catch (NoSolutionException e) {
                noSolutionCount++;
            }
        }

        if (isSaveMapStrings) {
            final String path = "junk/";
            final String dumpFile = path + "maps_moves.txt";
            final Path outputDir = Path.of(path);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // map file has format "<mapString><space><moveCount>\n"
            final List<String> output = new ArrayList<>(mapStrings.size());
            for (int i = 0; i < mapStringsToDump.size(); i++) {
                output.add(mapStringsToDump.get(i) + " " + mapMoves.get(i));
            }

            Files.write(Path.of(dumpFile), output, Charset.defaultCharset());
            cLogger.info("Maps dumped to file {}", dumpFile);
        }
        cLogger.info("Total run count =   {}", mapStringsToDump.size());
        cLogger.info("  no solution # =   {}", noSolutionCount);
        cLogger.info("Time Statistics (ms)");
        cLogger.info("      time tot =   {}", timeStats.getSum());
        cLogger.info("      time avg =   {}", timeStats.getMean());
        cLogger.info("      time med =   {}", timeStats.getPercentile(50));
        cLogger.info("      time std =   {}", timeStats.getStandardDeviation());
        cLogger.info("      time var =   {}", timeStats.getVariance());
    }
}
