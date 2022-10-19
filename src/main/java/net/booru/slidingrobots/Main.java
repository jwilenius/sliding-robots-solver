package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.SlidingRobotsSearchAlgorithm;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.ArgumentParser;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.RobotsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Main {
    private static final Logger cLogger = LoggerFactory.getLogger(Main.class);

    public static final String ARG_ADDITIONAL_DEPTH = "--solution-depth";
    public static final String ARG_SOLVE = "--solve";
    public static final String ARG_GENERATE = "--generate";
    public static final String ARG_VERBOSE = "--verbose";
    public static final String ARG_PROFILE = "--profile";
    public static final String ARG_MAPS_FILE = "--maps-file";
    public static final String ARG_DIM_X = "--dimx";
    public static final String ARG_DIM_Y = "--dimy";

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void main(String[] args) throws IOException {
        final String exampleMap = "map:8:8:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:" +
                "helper_robot:3:4:helper_robot:4:1:blocker:4:6:main_robot:5:5:blocker:5:7:" +
                "blocker:7:0:goal:6:7";

        final String exampleMapCompact =
                "m:8:8,b:0:0,b:0:4,b:2:4,b:2:5,b:3:1,h:3:4,h:4:1,b:4:6,r:5:5,b:5:7,b:7:0,g:6:7";
        final String exampleSeed = "seed:8:8:1BC2-EF09";

        final var argumentParser = new ArgumentParser()
                .withGeneralArgument(ARG_ADDITIONAL_DEPTH, "-1", List.of("<n>"), "Keep solution of additional depth best + n")
                .withSpecificArgument(ARG_VERBOSE, "-1", List.of("0", "1"), "print board solution verbose")
                .withGeneralArgument(ARG_SOLVE, null, List.of("<map-string | seed-string>"),
                        """
                                Solve the provided map.
                                               example: %s
                                               example: %s
                                               example: %s"""
                                .formatted(exampleMap, exampleMapCompact, exampleSeed))
                .withGeneralArgument(ARG_GENERATE, null, List.of("<mapsPerMove>|<mapsPerMove,minMoves,maxMoves>"),
                        """                                
                                Generate random maps, <mapsPerMove> maps per solution-moves required, with a range of [2..20] moves,
                                               OR <mapsPerMove> with a range [<minMoves>..<maxMoves>] 
                                               %s is required for saving the generated maps."""
                                .formatted(ARG_MAPS_FILE))
                .withGeneralArgument(ARG_PROFILE, null, List.of("<runs count>"),
                        """                                
                                Generate random maps and calculate average time.
                                               A value of <runs count> must be greater than 0 unless %s is provided."""
                                .formatted(ARG_MAPS_FILE))
                .withGeneralArgument(ARG_MAPS_FILE, "", List.of("<path/file>"),
                        """
                                A file with maps, one per line. Format '<map><space><moveCount>'
                                               Use this map file when %s is used and calculate statistics.
                                               If <runs count> is greater than 0, then the number of rows from the map file what will be run is limited by that number."""
                                .formatted(ARG_PROFILE))
                .withGeneralArgument(ARG_DIM_X, "8", List.of("<n>"), "The board x dimension size. Default 8.")
                .withGeneralArgument(ARG_DIM_Y, "8", List.of("<n>"), "The board y dimension size. Default 8.")

                .addConflicts(ARG_SOLVE, List.of(ARG_PROFILE, ARG_GENERATE))
                .addConflicts(ARG_PROFILE, List.of(ARG_SOLVE, ARG_GENERATE))
                .addConflicts(ARG_GENERATE, List.of(ARG_SOLVE, ARG_PROFILE));
        argumentParser.parseArguments(args);

        // no defaults
        final var solve = argumentParser.get(ARG_SOLVE);
        final var generate = argumentParser.get(ARG_GENERATE);
        final var profile = argumentParser.get(ARG_PROFILE);

        // with defaults
        final var solutionDepth = argumentParser.get(ARG_ADDITIONAL_DEPTH).get().getValueAsInt(); // NOSONAR safe
        final var mapsFile = argumentParser.get(ARG_MAPS_FILE).get().getValue();              // NOSONAR safe
        final var verboseLevel = argumentParser.get(ARG_VERBOSE).get().getValueAsInt();       // NOSONAR safe
        final var dimX = argumentParser.get(ARG_DIM_X).get().getValueAsInt();                 // NOSONAR safe
        final var dimY = argumentParser.get(ARG_DIM_Y).get().getValueAsInt();                 // NOSONAR safe


        // (*) SOLVE
        if (solve.isPresent()) {
            final String mapStringOrSeed = solve.get().getValue();
            singleRun(solutionDepth, mapStringOrSeed, verboseLevel); //NOSONAR
            System.exit(1);
        }

        // (*) PROFILE
        if (profile.isPresent()) {
            final int profileRuns = profile.get().getValueAsInt();
            ProfileRunner.profileRun(profileRuns, mapsFile, board -> getSearchAlgorithm(solutionDepth, board), dimX, dimY);
            System.exit(1);
        }

        // (*) GENERATE
        if (generate.isPresent()) {
            final int mapsPerMove;
            final int mapsMinMoves;
            final int mapsMaxMoves;

            final String argValue = generate.get().getValue();
            if (argValue.contains(",")) {
                final String[] settings = argValue.split(",");
                if (settings.length != 3) {
                    throw new IllegalArgumentException("expected: mapsPerMove,minMoves,maxMoves or just mapsPerMove");
                }
                mapsPerMove = Integer.parseInt(settings[0]);
                mapsMinMoves = Integer.parseInt(settings[1]);
                mapsMaxMoves = Integer.parseInt(settings[2]);
            } else {
                mapsPerMove = generate.get().getValueAsInt();
                mapsMinMoves = 2;
                mapsMaxMoves = 20;
            }

            final boolean isOneWay = false; // possibly allow to select this
            MapStringGenerator.generateToFile(mapsFile, dimX, dimY, mapsPerMove, mapsMinMoves, mapsMaxMoves, isOneWay);

            System.exit(1);
        }


        // (*) FALLBACK  -  no args, run example and print help
        argumentParser.outputHelp();
        cLogger.info("--------------------------------");
        cLogger.info("Now we will run an example problem:");
        singleRun(solutionDepth, exampleMap, Math.max(0, verboseLevel));

        System.exit(1);
    }

    private static void singleRun(final int solutionDepth, final String mapStringOrSeed, final int verboseLevel) {
        final boolean isVerbose = verboseLevel >= 0;
        final Game game = Game.valueOf(mapStringOrSeed);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getInitialRobotsState();

        if (isVerbose) {
            cLogger.info("");
            cLogger.info("------------------------------------------------");
            cLogger.info("Map/seed-string: {}", mapStringOrSeed);
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
}
