package net.booru.slidingrobots;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public enum Piece {
        helper_robot,
        blocker,
        main_robot,
        start,
        goal,
        empty;

        public boolean isBlocking() {
            return this == helper_robot || this == blocker || this == main_robot;
        }

        public boolean isImmovable() {
            return this == Piece.blocker || this == Piece.goal || this == Piece.start;
        }
    }

    public enum Direction {
        up,
        down,
        left,
        right;
    }

    /**
     * Represents the current state of robot positions
     */
    public static final class RobotsState {
        private final byte[] iPositions;

        /**
         * @param robotPositions the positions of robots as follows [main_robot_x, main_robot_y, helper_robot_1_x,
         *                       helper_robot_2_x, ..., helper_robot_n_x, helper_robot_n_x]
         */
        public RobotsState(byte[] robotPositions) {
            iPositions = robotPositions;
        }

        public int getRobotCount() {
            return iPositions.length / 2;
        }

        public RobotsState withPositionX(int robotIndex, int x) {
            if (getPositionX(robotIndex) == x) {
                return this;
            }

            final byte[] positions = new byte[iPositions.length];
            System.arraycopy(iPositions, 0, positions, 0, iPositions.length);
            positions[2 * robotIndex] = (byte) x;
            return new RobotsState(positions);
        }

        public RobotsState withPositionY(int robotIndex, int y) {
            if (getPositionY(robotIndex) == y) {
                return this;
            }

            final byte[] positions = new byte[iPositions.length];
            System.arraycopy(iPositions, 0, positions, 0, iPositions.length);
            positions[2 * robotIndex + 1] = (byte) y;
            return new RobotsState(positions);
        }

        public int getPositionX(int robotIndex) {
            return iPositions[2 * robotIndex];
        }

        public int getPositionY(int robotIndex) {
            return iPositions[2 * robotIndex + 1];
        }

        public boolean isOtherRobotBlocking(final int x, final int y, final int robotIndex) {
            for (int i = 0, len = getRobotCount(); i < len; i++) {
                if (i == robotIndex) {
                    continue;
                }
                final boolean isBlocking = iPositions[2 * i] == x && iPositions[2 * i + 1] == y;
                if (isBlocking) {
                    return true;
                }
            }

            return false;
        }

        /**
         * @param x position
         * @param y position
         *
         * @return the index [0..robotCount-1] of the robot at position x,y. -1 if no robot is found.
         */
        public int getRobotAtPosition(final int x, final int y) {
            for (int i = 0, len = iPositions.length / 2; i < len; i++) {
                final boolean isBlocking = iPositions[2 * i] == x && iPositions[2 * i + 1] == y;
                if (isBlocking) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Represents the immovable pieces on a board of certain dimensions. A Board is immutable and never changes. The
     * board is used to make moves given a RobotsState, which then returns a new RobotsState.
     */
    public static final class Board {
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
         * @param direction   a direction resulting in possibly a new {@link RobotsState}, or the same {@link
         *                    RobotsState}.
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
         * Get the directional position where robotIndex finds a collision. In the vertical directions (up down) this
         * would be the value of the y-coordinate and vice versa.
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

    public static void main(String[] args) {
        final String boardStr =
                "map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot:5:6" +
                ":blocker:6:1:blocker:7:7:goal:3:0";

        final Board board = Board.valueOf(boardStr);
        System.out.println(board.toString());
    }
}
