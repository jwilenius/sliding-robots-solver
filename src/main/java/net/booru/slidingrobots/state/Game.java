package net.booru.slidingrobots.state;

import net.booru.slidingrobots.algorithm.model.Waypoint;
import net.booru.slidingrobots.common.Pair;
import net.booru.slidingrobots.common.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Game {
    private static final Logger cLogger = LoggerFactory.getLogger(Game.class);

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
     * Example compact 8x8 board:
     *  m:8:8,b:0:0,b:0:4,b:2:4,b:2:5,b:3:1,h:3:4,h:4:1,b:4:6,r:5:5,b:5:7,b:7:0,g:6:7
     * </pre>
     *
     * @param mapOrCompactMap a string representation of a {@link Board}.
     * @return the Board corresponding to {@code value}.
     */
    public static Game valueOf(final String mapOrCompactMap) {
        final String mapString;
        if (mapOrCompactMap.startsWith("map:")) {
            mapString = mapOrCompactMap;
        } else if (mapOrCompactMap.startsWith("m:")) {
            mapString = mapOrCompactMap
                    .replace("m:", "map:")
                    .replace(",b:", ":blocker:")
                    .replace(",r:", ":main_robot:")
                    .replace(",h:", ":helper_robot:")
                    .replace(",g:", ":goal:");
            cLogger.info("");
            cLogger.info("Expanded map string: {}", mapString);
            cLogger.info("");
        } else {
            throw new IllegalArgumentException("map string must start with map:sizeX:sizeY (or m:sizeX:sizeY if compact)");
        }

        final String[] tokens = mapString.split(":");
        final int width = Integer.parseInt(tokens[1]);
        final int height = Integer.parseInt(tokens[2]);

        int start = 3;
        boolean isOneWay = false;
        if (tokens[3].equals("oneway")) {
            isOneWay = true;
            start = 4;
        }

        final List<Pair<Point, Piece>> pieces = new ArrayList<>(20);
        for (int i = start; i < tokens.length; i += 3) {
            final Point position = new Point(Integer.parseInt(tokens[i + 1]), Integer.parseInt(tokens[i + 2]));
            final Piece piece = Piece.valueOf(tokens[i]);
            if (piece == Piece.main_robot) { // input string equates main_robot with start position, which makes sense
                pieces.add(Pair.of(position, Piece.start));
                pieces.add(Pair.of(position, Piece.main_robot));
            } else {
                pieces.add(Pair.of(position, piece));
            }
        }

        final List<Pair<Point, Piece>> robotList = pieces.stream()
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

    public List<Waypoint> getEndCriteria() {
        if (iIsOneWay) {
            return List.of(node -> iBoard.isGoalReached(node.getState()));
        } else {
            return List.of(
                    node -> iBoard.isGoalReached(node.getState()),
                    node -> iBoard.isStartReached(node.getState())
            );
        }
    }


}
