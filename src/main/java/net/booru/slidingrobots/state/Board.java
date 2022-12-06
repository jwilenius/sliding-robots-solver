package net.booru.slidingrobots.state;

import net.booru.slidingrobots.common.Direction;
import net.booru.slidingrobots.common.Pair;
import net.booru.slidingrobots.common.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the immovable pieces on a board of certain dimensions. A Board is immutable and never changes. The board
 * is used to make moves given a RobotsState, which then returns a new RobotsState.
 */
public final class Board {
    private final int MAIN_ROBOT_INDEX = 0;
    private final int iWidth;
    private final int iHeight;
    private final Piece[][] iImmutableBoard;
    private final Point iStartPosition;
    private final Point iGoalPosition;

    public Board(final List<Pair<Point, Piece>> startPieces, final int width, final int height) {
        iWidth = width;
        iHeight = height;
        iImmutableBoard = new Piece[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                iImmutableBoard[x][y] = Piece.empty;
            }
        }

        Point startPosition = null;
        Point goalPosition = null;
        for (Pair<Point, Piece> entry : startPieces) {
            final Point position = entry.first;
            final Piece piece = entry.second;

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

    public boolean isGoalReached(final RobotsState robotsState) {
        return robotsState.getPositionX(MAIN_ROBOT_INDEX) == iGoalPosition.x &&
                robotsState.getPositionY(MAIN_ROBOT_INDEX) == iGoalPosition.y;
    }

    public boolean isStartReached(final RobotsState robotsState) {
        return robotsState.getPositionX(MAIN_ROBOT_INDEX) == iStartPosition.x &&
                robotsState.getPositionY(MAIN_ROBOT_INDEX) == iStartPosition.y;
    }

    /**
     * Compute neighboring states to {@code this} state and return them as long as they are not the same as {@code this}
     * state and not present in {@code seenStates}
     *
     * @param robotsState the current positions of the robots and other information
     * @return the new unseen neighbors of {@code currentState}, may be empty.
     */
    public List<RobotsState> getNeighbors(final RobotsState robotsState) {
        final int robotCount = robotsState.getRobotCount();
        final Direction[] directions = Direction.values();

        final List<RobotsState> expandedState = new ArrayList<>(robotCount * directions.length);

        for (int robotIndex = 0; robotIndex < robotCount; robotIndex++) {
            for (Direction direction : directions) {
                final RobotsState nextState = makeMove(robotIndex, direction, robotsState);
                if (!nextState.isSamePosition(robotsState, robotIndex)) {
                    expandedState.add(nextState);
                }
            }
        }
        return expandedState;
    }

    /**
     * @param robotIndex  the robot to move
     * @param direction   a direction resulting in possibly a new {@link RobotsState}, or the same {@link RobotsState}.
     * @param robotsState the current state
     * @return the new boardState after robot at {@code robotIndex} was moved in {@code direction}. If the move result
     * in the same state as {@code robotState} then the {@code robotState} object is returned.
     */
    public RobotsState makeMove(final int robotIndex, final Direction direction, final RobotsState robotsState) {
        final int positionBeforeCollision = findPositionBeforeCollision(robotIndex, direction, robotsState);
        switch (direction) {
            case up, down:
                return robotsState.withPositionY(robotIndex, positionBeforeCollision);
            case left, right:
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
     * @return the coordinate value where collision occurs
     */
    protected int findPositionBeforeCollision(final int robotIndex, final Direction direction,
                                              final RobotsState robotsState) {
        final int robotPositionX = robotsState.getPositionX(robotIndex);
        final int robotPositionY = robotsState.getPositionY(robotIndex);

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

    @Override
    public String toString() {
        return toBoardString(new RobotsState(new byte[0], 0));
    }

    public String toBoardString(final RobotsState robotsState) {
        final String border = "-".repeat(2 * iWidth - 1);
        final StringBuilder sb = new StringBuilder();
        sb.append("Board for state_id = " + robotsState.getId()).append('\n');
        for (int y = -1; y < iHeight; y++) {
            for (int x = -1; x < iWidth; x++) {
                // output the coordinates
                if (x == -1) {
                    if (y == -1) {
                        sb.append("   ");
                    } else {
                        sb.append(y).append("| ");
                    }
                    continue;
                }

                if (y == -1) {
                    sb.append(x).append(' ');
                    continue;
                }


                // first output robots then the immutable board
                final int robotIndex = robotsState.getRobotAtPosition(x, y);
                if (robotIndex != -1) {
                    // output robot
                    sb.append(robotIndex).append(' ');
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
            if (y == -1) {
                sb.append("\n   ").append(border);
            }
            sb.append('\n');
        }
        sb.append("   ").append(border).append("\n");

        return sb.toString();
    }

    public List<String> boardToLogLines(final RobotsState robotsState) {
        final List<String> lines = new ArrayList<>();

        for (int y = -1; y < iHeight; y++) {
            final StringBuilder sb = new StringBuilder();

            for (int x = -1; x < iWidth; x++) {
                // output the coordinates
                if (x == -1) {
                    if (y == -1) {
                        sb.append("    ");
                    } else {
                        sb.append(y).append(" | ");
                    }
                    continue;
                }
                if (y == -1) {
                    sb.append(x).append(' ');
                    continue;
                }


                // first output robots then the immutable board
                final int robotIndex = robotsState.getRobotAtPosition(x, y);
                if (robotIndex != -1) {
                    // output robot
                    sb.append(robotIndex).append(' ');
                } else {
                    // output board
                    final Piece piece = iImmutableBoard[x][y];
                    switch (piece) { //NOSONAR intentional skip of main_robot and helper
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
            lines.add(sb.toString());
            if (y == -1) {
                lines.add("    ----------------");
            }
        }
        lines.add("    ----------------");

        return lines;
    }
}
