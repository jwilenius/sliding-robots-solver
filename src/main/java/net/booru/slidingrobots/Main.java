package net.booru.slidingrobots;

public class Main {

    public static void main(String[] args) {
        final String boardStr =
                "map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot:5:6" +
                ":blocker:6:1:blocker:7:7:goal:3:0";

        final Board board = Board.valueOf(boardStr);
        System.out.println(board.toString());
    }
}
