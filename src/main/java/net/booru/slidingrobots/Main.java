package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchRecursive;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.Solution;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.RobotsState;

public class Main {

    public static void main(String[] args) {
        final String boardStr =
                // "map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot
                // :5:6:blocker:6:1:blocker:7:7:goal:3:0";
                "map:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:helper_robot:3:4:helper_robot:4:1" +
                ":blocker:4:6:main_robot:5:5:blocker:5:7:blocker:7:0:goal:6:7";
        //final String boardStrSmall = "map_3:blocker:2:0:main_robot:2:2:goal:1:0";

        System.out.println("Map:\n" + boardStr);
        final Game game = Game.valueOf(boardStr);
        final Board board = game.getBoard();
        final RobotsState robotsState = game.getInitialRobotsState();

        System.out.println(board.printBoard(robotsState));

        try {
            final Solution solution = new BreadthFirstSearchRecursive(board).run(robotsState);
            System.out.println(solution.toString());
        } catch (NoSolutionException e) {
            System.out.println("No solution");
        }
    }
}
