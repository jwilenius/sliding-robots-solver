package net.booru.slidingrobots;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the immovable pieces on a board of certain dimensions. A Board is immutable and never changes. The board
 * is used to make moves given a RobotsState, which then returns a new RobotsState.
 */
public final class Board {
    private final int iWidth;
    private final int iHeight;
    private final HashMap<Point, Piece> iStartPieces;
    private final Piece[][] iImmutableBoard;
    private final Point iStartPosition;
    private final Point iGoalPosition;

    public Board(final HashMap<Point, Piece> startPieces, final int width, final int height) {
        iWidth = width;
        iHeight = height;
        iStartPieces = startPieces;
        iImmutableBoard = new Piece[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                iImmutableBoard[x][y] = Piece.empty;
            }
        }

        Point startPosition = null;
        Point goalPosition = null;
        for (Map.Entry<Point, Piece> entry : startPieces.entrySet()) {
            final Point position = entry.getKey();
            final Piece piece = entry.getValue();

            if (piece.isImmovable()) {
                iImmutableBoard[position.x][position.y] = piece;
                if (piece == Piece.goal) {
                    goalPosition = position;
                } else if (piece == Piece.start) {
                    startPosition = position;
                }
            }
        }
        if (startPosition == null) {
            throw new IllegalArgumentException("startPieces must contain the start position");
        }
        if (goalPosition == null) {
            throw new IllegalArgumentException("startPieces must contain the goal position");
        }

        iGoalPosition = goalPosition;
        iStartPosition = startPosition;
    }

    /**
     * Parse a string description of a board setup.
     * <p>
     * <pre>
     * Example: map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:
     *          blocker:2:3:blocker:4:0:helper_robot:5:6:blocker:6:1:"blocker:7:7:goal:3:0
     * </pre>
     *
     * @param value a string representation of a {@link Board}.
     *
     * @return the Board corresponding to {@code value}.
     */
    public static Board valueOf(final String value) {
        final HashMap<Point, Piece> pieces = new HashMap<>(20);
        final String[] tokens = value.split(":");
        final int start = tokens[0].equals("map") ? 1 : 0;

        for (int i = start; i < tokens.length; i += 3) {
            final Point position = new Point(Integer.parseInt(tokens[i + 1]), Integer.parseInt(tokens[i + 2]));
            final Piece piece = Piece.valueOf(tokens[i]);
            if (piece == Piece.main_robot) {
                pieces.put(position, Piece.start);
            } else {
                pieces.put(position, piece);
            }
        }

        return new Board(pieces, 8, 8);
    }

    public boolean isGoalReached(final RobotsState robotsState) {
        return robotsState.getPositionX(0) == iGoalPosition.x && robotsState.getPositionY(0) == iGoalPosition.y;
    }

    public boolean isStartReached(final RobotsState robotsState) {
        return robotsState.getPositionX(0) == iStartPosition.x && robotsState.getPositionY(0) == iStartPosition.y;
    }

    /**
     * @param robotIndex  the robot to move
     * @param direction   a direction resulting in possibly a new {@link RobotsState}, or the same {@link RobotsState}.
     * @param robotsState the current state
     *
     * @return the new boardState after robot at {@code robotIndex} was moved in {@code direction}
     */
    public RobotsState makeMove(final int robotIndex, final Direction direction, final RobotsState robotsState) {
        final int positionBeforeCollision = findPositionBeforeCollision(robotIndex, direction, robotsState);
        switch (direction) {
            case up:
            case down:
                return robotsState.withPositionY(robotIndex, positionBeforeCollision);
            case left:
            case right:
                return robotsState.withPositionX(robotIndex, positionBeforeCollision);
            default:
                throw new IllegalStateException("Unknown Case Exception.");
        }
    }

    /**
     * Get the directional position where robotIndex finds a collision. In the vertical directions (up down) this would
     * be the value of the y-coordinate and vice versa.
     *
     * @param robotIndex  the robot we are checking the collision for
     * @param direction   the direction we are looking for collision
     * @param robotsState the current state of the robots
     *
     * @return the coordinate value where collision occurs
     */
    private int findPositionBeforeCollision(final int robotIndex, final Direction direction,
                                            final RobotsState robotsState) {
        int robotPositionX = robotsState.getPositionX(robotIndex);
        int robotPositionY = robotsState.getPositionY(robotIndex);

        switch (direction) {
            case up:
                for (int y = robotPositionY - 1; y >= 0; --y) {
                    if (iImmutableBoard[robotPositionX][y].isBlocking() ||
                        robotsState.isOtherRobotBlocking(robotPositionX, y, robotIndex)) {
                        return y + 1;
                    }
                }
                return 0; // we hit the edge of the board

            case down:
                for (int y = robotPositionY + 1; y < iHeight; ++y) {
                    if (iImmutableBoard[robotPositionX][y].isBlocking() ||
                        robotsState.isOtherRobotBlocking(robotPositionX, y, robotIndex)) {
                        return y - 1;
                    }
                }
                return iHeight - 1; // we hit the edge of the board

            case left:
                for (int x = robotPositionX - 1; x >= 0; --x) {
                    if (iImmutableBoard[x][robotPositionY].isBlocking() ||
                        robotsState.isOtherRobotBlocking(x, robotPositionY, robotIndex)) {
                        return x + 1;
                    }
                }
                return 0; // we hit the edge of the board

            case right:
                for (int x = robotPositionX + 1; x < iWidth; ++x) {
                    if (iImmutableBoard[x][robotPositionY].isBlocking() ||
                        robotsState.isOtherRobotBlocking(x, robotPositionY, robotIndex)) {
                        return x - 1;
                    }
                }
                return iWidth - 1; // we hit the edge of the board

            default:
                throw new IllegalStateException("Unknown Case Exception.");
        }
    }

    /**
     * @return true if the move does not move the robot outside the board or onto an occupied position
     */
    private boolean isLegalMove(final int robotIndex, final Direction direction, final RobotsState robotsState) {
        return false;
    }

    @Override
    public String toString() {
        return printBoard(new RobotsState(new byte[0]));
    }

    public String printBoard(final RobotsState robotsState) {
        final StringBuilder sb = new StringBuilder();
        for (int y = 0; y < iHeight; y++) {
            for (int x = 0; x < iWidth; x++) {
                // first output robots then the immutable board
                final int robotIndex = robotsState.getRobotAtPosition(x, y);
                if (robotIndex != -1) {
                    // output robot
                    if (robotIndex == 0) {
                        sb.append("@ ");
                    } else if (robotIndex > 0) {
                        sb.append("h ");
                    }
                } else {
                    // output board
                    final Piece piece = iImmutableBoard[x][y];
                    switch (piece) {
                        case empty:
                            sb.append(". ");
                            break;
                        case blocker:
                            sb.append("# ");
                            break;
                        case goal:
                            sb.append("G ");
                            break;
                        case start: // needs to be after main_robot since this is ascii...
                            sb.append("S ");
                            break;
                    }
                }
            } // end row
            sb.append('\n');
        }

        return sb.toString();
    }
}
