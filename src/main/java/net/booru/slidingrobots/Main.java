package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.BreadthFirstSearchRecursive;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.SlidingRobotsSearchAlgorithm;
import net.booru.slidingrobots.algorithm.Solution;
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

    public static void main(String[] args) throws IOException {
        final String exampleMap = "map:8:8:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:" +
                                  "helper_robot:3:4:helper_robot:4:1:blocker:4:6:main_robot:5:5:blocker:5:7:" +
                                  "blocker:7:0:goal:6:7";

        if (args.length == 0) {
            System.out.println("\nRunning example map!");
            System.out.println("args: --alg <i|r> (--solve <str> | --profile <N>)" +
                               "  --alg <i|r>            : iterative or recursive" +
                               "  --solve <map-string>   : Solve the provided map, se below for map format.");
            System.out.println("  --profile <runs count> : Generate random maps and calculate average time.");
            System.out.println("                         : A value of 0 means infinite, no maps are saved.");
            singleRun("i", exampleMap);
        } else {
            if (!args[0].equals("--alg")) {
                throw new IllegalArgumentException("expected --arg <i|r>");
            }
            switch (args[2]) {
                case "--solve":
                    singleRun(args[1], args[3]);
                    break;
                case "--profile":
                    multiRun(args[1], Integer.parseInt(args[3]));
                    break;
            }
        }
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

    private static void singleRun(final String algorithmType, final String mapString) {

        final Game game = Game.valueOf(mapString);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getRobotsState();
        final boolean isOneWay = game.isOneWay();

        System.out.println("Map-string:\n" + mapString);
        System.out.println(board.printBoard(robotsState));

        try {
            final SlidingRobotsSearchAlgorithm searchAlgorithm = chooseAlgorithm(algorithmType, board);
            final Solution solution = searchAlgorithm.run(robotsState, game.getEndCriteria());
            System.out.println(solution.toString());

            int i = 0;
            for (RobotsState s : solution.getSolutionPath()) {
                System.out.println("Board i=" + (i++));
                System.out.println(board.printBoard(s));
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
