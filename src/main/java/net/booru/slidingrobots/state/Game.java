package net.booru.slidingrobots.state;

import net.booru.slidingrobots.common.Pair;
import net.booru.slidingrobots.common.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Game {
    private final boolean iIsOneWay;
    private final Board iBoard;
    private final RobotsState iInitialRobotsState;

    public Game(final boolean isOneWay, final Board board, final RobotsState initialRobotsState) {
        iIsOneWay = isOneWay;
        iBoard = board;
        iInitialRobotsState = initialRobotsState;
    }

    /**
     * Parse a string description of a board setup.
     * <p>
     * <pre>
     * Example 8x8 board:
     *  map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot:5:6:blocker:6:1:"blocker:7:7:goal:3:0
     * Example 9x9 board:
     *  map_9:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot:5:6:blocker:6:1:"blocker:7:7:goal:3:0
     * </pre>
     *
     * @param value a string representation of a {@link Board}.
     *
     * @return the Board corresponding to {@code value}.
     */
    public static Game valueOf(final String value) {
        final java.util.List<Pair<Point, Piece>> pieces = new ArrayList<>(20);
        final String[] tokens = value.split(":");

        if (!tokens[0].startsWith("map")) {
            throw new IllegalArgumentException("map string must start with map:sizeX:sizeY");
        }

        final int width = Integer.parseInt(tokens[1]);
        final int height = Integer.parseInt(tokens[1]);

        int start = 3;
        boolean isOneWay = false;
        if (tokens[3].equals("oneway")) {
            isOneWay = true;
            start = 4;
        }

        for (int i = start; i < tokens.length; i += 3) {
            final Point position = new Point(Integer.parseInt(tokens[i + 1]), Integer.parseInt(tokens[i + 2]));
            final Piece piece = Piece.valueOf(tokens[i]);
            if (piece ==
                Piece.main_robot) { // input string equates main_robot with start position, which makes sense
                pieces.add(Pair.of(position, Piece.start));
                pieces.add(Pair.of(position, Piece.main_robot));
            } else {
                pieces.add(Pair.of(position, piece));
            }
        }

        final List<Pair<Point, Piece>> robotList =
                pieces.stream()
                      .filter(pointPieceEntry -> !pointPieceEntry.second.isImmovable())
                      .collect(Collectors.toList());

        return new Game(isOneWay, new Board(pieces, width, height), RobotsState.valueOf(robotList));
    }

    public Board getBoard() {
        return iBoard;
    }

    public RobotsState getRobotsState() {
        return iInitialRobotsState;
    }

    public boolean isOneWay() {
        return iIsOneWay;
    }
}
