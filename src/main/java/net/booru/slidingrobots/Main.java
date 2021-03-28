package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchRecursive;
import net.booru.slidingrobots.algorithm.NoSolutionException;
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
        final String exampleMap =
                // "map8:8:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot
                // :5:6:blocker:6:1:blocker:7:7:goal:3:0";
                "map:8:8:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:helper_robot:3:4:helper_robot:4" +
                ":1" +
                ":blocker:4:6:main_robot:5:5:blocker:5:7:blocker:7:0:goal:6:7";
        if (args.length == 0) {
            System.out.println("\nRunning example map!");
            System.out.println(" args: --solve <map-string>    : solve the provided map, se below for map format.");
            System.out.println("       --profile               : generate random maps and calculate average time.");
            singleRun(exampleMap);
        } else {
            switch (args[0]) {
                case "--solve":
                    singleRun(args[1]);
                    break;
                case "--profile":
                    multiRun();
                    break;
            }

        }

    }

    private static void singleRun(final String mapString) {

        final Game game = Game.valueOf(mapString);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getRobotsState();

        System.out.println("Map-string:\n" + mapString);
        System.out.println(board.printBoard(robotsState));

        try {
            final Solution solution = new BreadthFirstSearchRecursive(board).run(robotsState);
            System.out.println(solution.toString());
        } catch (NoSolutionException e) {
            System.out.println("No solution");
        }
    }

    /**
     * For getting stats on average speed
     */
    private static void multiRun() throws IOException {
        final List<String> mapStrings = new LinkedList<>();
        for (int i = 0; i < 1000; i++) {
            mapStrings.add(MapStringGenerator.generate(8));
        }

        double time = 0;
        for (String mapString : mapStrings) {
            System.out.println("Map:\n" + mapString);
            final Game game = Game.valueOf(mapString);
            System.out.println(game.getBoard().printBoard(game.getRobotsState()));
            try {
                final Timer t = new Timer();
                final Solution solution = new BreadthFirstSearchRecursive(game.getBoard()).run(game.getRobotsState());
                t.close();
                time += t.getDurationMillis();
                System.out.println(solution.toString());
            } catch (NoSolutionException e) {
                System.out.println("No solution");
            }
        }

        Files.write(Path.of("junk/maps.txt"), mapStrings, Charset.defaultCharset());
        System.out.println("Total time (ms) = " + time);
        System.out.println("Average time (ms) = " + (time / mapStrings.size()));
        System.out.println("Maps dumped to file junk/maps.txt");
    }
}
