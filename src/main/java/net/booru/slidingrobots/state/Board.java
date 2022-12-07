package net.booru.slidingrobots.state;

import net.booru.slidingrobots.common.Condition;
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
    private final int iRobotCount;

    public Board(final List<Pair<Point, Piece>> startPieces, final int width, final int height, final int robotCount) {
        Condition.require(width > 0 && width < 15);
        Condition.require(height > 0 && height < 15);
        Condition.require(robotCount > 0 && robotCount <= RobotsState.MAX_ROBOT_COUNT);
        iWidth = width;
        iHeight = height;
        iRobotCount = robotCount;
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
        Condition.require(startPosition != null, "startPieces must contain the start position");
        Condition.require(goalPosition != null, "startPieces must contain the goal position");

        iGoalPosition = goalPosition;
        iStartPosition = startPosition;
    }

    public boolean isGoalReached(final int robotsState) {
        return RobotsState.getPositionX(MAIN_ROBOT_INDEX, robotsState) == iGoalPosition.x &&
                RobotsState.getPositionY(MAIN_ROBOT_INDEX, robotsState) == iGoalPosition.y;
    }

    public boolean isStartReached(final int robotsState) {
        return RobotsState.getPositionX(MAIN_ROBOT_INDEX, robotsState) == iStartPosition.x &&
                RobotsState.getPositionY(MAIN_ROBOT_INDEX, robotsState) == iStartPosition.y;
    }

    /**
     * Compute neighboring states to {@code this} state and return them as long as they are not the same as {@code this}
     * state and not present in {@code seenStates}
     *
     * @param robotsState the current positions of the robots and other information
     * @return the new unseen neighbors of {@code currentState}, may be empty.
     */
    public List<Integer> getNeighbors(final int robotsState) {
        final int robotCount = iRobotCount;
        final Direction[] directions = Direction.values();

        final List<Integer> expandedState = new ArrayList<>(robotCount * directions.length);

        for (int robotIndex = 0; robotIndex < robotCount; robotIndex++) {
            for (Direction direction : directions) {
                final int nextState = makeMove(robotIndex, direction, robotsState);
                if (!RobotsState.isSamePosition(nextState, robotsState, robotIndex)) {
                    //if (!nextState.isSamePosition(robotsState, robotIndex)) {
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
     * @return the new state after robot at {@code robotIndex} was moved in {@code direction}. If the move result
     * in the same state as {@code robotState} then the {@code robotState} object is returned.
     */
    public int makeMove(final int robotIndex, final Direction direction, final int robotsState) {
        final int positionBeforeCollision = findPositionBeforeCollision(robotIndex, direction, robotsState);
        switch (direction) {
            case up, down:
                return RobotsState.withPositionY(robotIndex, positionBeforeCollision, robotsState);
            case left, right:
                return RobotsState.withPositionX(robotIndex, positionBeforeCollision, robotsState);
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
    protected int findPositionBeforeCollision(final int robotIndex, final Direction direction, final int robotsState) {
        final int robotPositionX = RobotsState.getPositionX(robotIndex, robotsState);
        final int robotPositionY = RobotsState.getPositionY(robotIndex, robotsState);

        switch (direction) {
            case up:
                for (int y = robotPositionY - 1; y >= 0; --y) {
                    if (iImmutableBoard[robotPositionX][y].isBlocking() ||
                            RobotsState.isOtherRobotBlocking(robotPositionX, y, robotIndex, robotsState)) {
                        return y + 1;
                    }
                }
                return 0; // we hit the edge of the board

            case down:
                for (int y = robotPositionY + 1; y < iHeight; ++y) {
                    if (iImmutableBoard[robotPositionX][y].isBlocking() ||
                            RobotsState.isOtherRobotBlocking(robotPositionX, y, robotIndex, robotsState)) {
                        return y - 1;
                    }
                }
                return iHeight - 1; // we hit the edge of the board

            case left:
                for (int x = robotPositionX - 1; x >= 0; --x) {
                    if (iImmutableBoard[x][robotPositionY].isBlocking() ||
                            RobotsState.isOtherRobotBlocking(x, robotPositionY, robotIndex, robotsState)) {
                        return x + 1;
                    }
                }
                return 0; // we hit the edge of the board

            case right:
                for (int x = robotPositionX + 1; x < iWidth; ++x) {
                    if (iImmutableBoard[x][robotPositionY].isBlocking() ||
                            RobotsState.isOtherRobotBlocking(x, robotPositionY, robotIndex, robotsState)) {
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
        return toBoardString(null);
    }

    /**
     * @param robotsState may be null, and if so we only return the immutable board string
     * @return the string of the board
     */
    public String toBoardString(final Integer robotsState) {
        final String border = "-".repeat(2 * iWidth - 1);
        final StringBuilder sb = new StringBuilder();

        sb.append("Board for state_id = ")
                .append(robotsState == null ? "<none>" : robotsState)
                .append("%n");

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
                final int robotIndex = robotsState == null ? -1 : RobotsState.getRobotAtPosition(x, y, robotsState);
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

    public List<String> boardToLogLines(final int robotsState) {
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
                final int robotIndex = RobotsState.getRobotAtPosition(x, y, robotsState);
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
