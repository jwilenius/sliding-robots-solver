package net.booru.slidingrobots.state;

import net.booru.slidingrobots.common.Pair;
import net.booru.slidingrobots.common.Point;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the current state of robot positions A RobotState is immutable.
 */
// TODO: since we now have only an int as state... we don't need to create RobotsState objects
public final class RobotsState {
    static final int BITS_FOR_WAYPOINTS = 2;
    static final int BITS_PER_POSITION = 4;
    static final int NOT_DEFINED = (int) Math.pow(2, BITS_PER_POSITION) - 1; // positions with this value for robots that do not exist
    static final int MAX_ROBOT_COUNT = 3;
    static final int BITS_TOTAL = BITS_PER_POSITION * 2 * MAX_ROBOT_COUNT + BITS_FOR_WAYPOINTS;
    static final int MAX_WAYPOINT = (int) (Math.pow(2, BITS_FOR_WAYPOINTS) - 1);
    public static final int MAX_STATES = (int) Math.pow(2, BITS_TOTAL) - 1;  // ca 65 million

    static final int MASK_ROBOT_POSITIONS = 0b00_1111_1111_1111_1111_1111_1111;
    static final int MASK_WAYPOINT = 0b11_0000_0000_0000_0000_0000_0000;
    static final int[] MASK_ROBOT_X = {
            0b00_0000_0000_0000_0000_0000_1111,
            0b00_0000_0000_0000_1111_0000_0000,
            0b00_0000_1111_0000_0000_0000_0000,
    };
    static final int[] MASK_ROBOT_Y = {
            0b00_0000_0000_0000_0000_1111_0000,
            0b00_0000_0000_1111_0000_0000_0000,
            0b00_1111_0000_0000_0000_0000_0000,
    };
    static final int[] MASK_ROBOT = {
            0b00_0000_0000_0000_0000_1111_1111,
            0b00_0000_0000_1111_1111_0000_0000,
            0b00_1111_1111_0000_0000_0000_0000,
    };
    static final int[] SHIFTS_ROBOT_X = {0, 8, 16};
    static final int[] SHIFTS_ROBOT_Y = {4, 12, 20};
    static final int[] SHIFTS_ROBOT = {0, 8, 16};
    static final int SHIFTS_WAYPOINT = 24;

    /**
     * @param robotPositions   the positions of robots as follows [main_robot_x, main_robot_y, helper_robot_1_x,
     *                         helper_robot_2_x, ..., helper_robot_n_x, helper_robot_n_x]
     * @param waypointsReached the goals that have been reached starting with the first goal at 0. No goal has the value of -1
     */
    public static int valueOf(final byte[] robotPositions, final int waypointsReached) {
        return encodeState(waypointsReached, robotPositions);
    }

    public static int valueOf(final List<Pair<Point, Piece>> robots) {
        final Pair<Point, Piece> mainRobot = robots.stream().filter(pair -> pair.second == Piece.main_robot).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No main robot"));

        final List<Pair<Point, Piece>> helperRobots = robots.stream().filter(pair -> pair.second == Piece.helper_robot).toList();
        if (helperRobots.size() + 1 != robots.size()) {
            throw new IllegalArgumentException("Input contains other pieces than one main robot and helper robots");
        }

        final byte[] robotPositions = new byte[robots.size() * 2];
        robotPositions[0] = (byte) mainRobot.first.x;
        robotPositions[1] = (byte) mainRobot.first.y;

        for (int i = 0; i < helperRobots.size(); i++) {
            final Pair<Point, Piece> pointAndPiece = helperRobots.get(i);
            final int index = 2 * (i + 1);
            robotPositions[index] = (byte) pointAndPiece.first.x;
            robotPositions[index + 1] = (byte) pointAndPiece.first.y;
        }

        return valueOf(robotPositions, 0);
    }

    public static int getRobotCount(final int state) {
        int count = 0;
        final int positions = state & MASK_ROBOT_POSITIONS;
        for (int i = MAX_ROBOT_COUNT - 1; i >= 0; i--) {
            if ((positions & MASK_ROBOT[i]) != MASK_ROBOT[i]) {
                count = i + 1;
                break;
            }
        }

        return count;
    }

    static int encodeState(final int waypointsReached, final byte[] positions) {
        int state = waypointsReached;

        for (int i = MAX_ROBOT_COUNT * 2; i > positions.length; i--) {
            state = (state << BITS_PER_POSITION) | NOT_DEFINED;
        }

        for (int i = positions.length; i > 0; i--) {
            state = (state << BITS_PER_POSITION) | positions[i - 1];
        }

        return state;
    }

    static Pair<Integer, byte[]> decodeState(final int state) {

        final byte[] positions = new byte[MAX_ROBOT_COUNT * 2];
        for (int i = 0; i < MAX_ROBOT_COUNT; i++) {
            positions[2 * i] = (byte) decodeX(i, state);
            positions[2 * i + 1] = (byte) decodeY(i, state);
        }

        final int waypointsReached = decodeWaypoint(state);

        int definedPositionsCount = 0;
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] == NOT_DEFINED) {
                break;
            }
            definedPositionsCount++;
        }

        final byte[] actualPositions = Arrays.copyOfRange(positions, 0, definedPositionsCount);
        return Pair.of(waypointsReached, actualPositions);
    }

    static int increaseWaypoint(final int state) {
        final int currentWaypoint = (state & MASK_WAYPOINT) >> SHIFTS_WAYPOINT;
        if (currentWaypoint >= MAX_WAYPOINT) {
            throw new IllegalStateException("waypoint maximum reached");
        }
        final int updatedWaypoint = (currentWaypoint + 1) << SHIFTS_WAYPOINT;
        final int newState = (state & MASK_ROBOT_POSITIONS) | updatedWaypoint;

        return newState;
    }

    static int decodeWaypoint(final int stateId) {
        final int currentWaypoint = (stateId & MASK_WAYPOINT) >> SHIFTS_WAYPOINT;
        return currentWaypoint;
    }

    static int decodeX(int robotIndex, int state) {
        return (state & MASK_ROBOT_X[robotIndex]) >> SHIFTS_ROBOT_X[robotIndex];
    }

    static int decodeY(int robotIndex, int state) {
        return (state & MASK_ROBOT_Y[robotIndex]) >> SHIFTS_ROBOT_Y[robotIndex];
    }

    static int decodePosition(final int robotIndex, final int state) {
        return (state & MASK_ROBOT[robotIndex]) >> SHIFTS_ROBOT[robotIndex];
    }

    static int encodeX(int x, int robotIndex, int state) {
        final int encodedX = x << SHIFTS_ROBOT_X[robotIndex];
        final int newState = (state & ~MASK_ROBOT_X[robotIndex]) | encodedX;
        return newState;
    }

    static int encodeY(int y, int robotIndex, int state) {
        final int encodedY = y << SHIFTS_ROBOT_Y[robotIndex];
        final int newState = (state & ~MASK_ROBOT_Y[robotIndex]) | encodedY;
        return newState;
    }

    public static int getWaypointsReached(int state) {
        return decodeWaypoint(state);
    }

    public static int withNextWaypoint(int state) {
        return increaseWaypoint(state);
    }

    public static int withPositionX(final int robotIndex, final int x, final int state) {
        if (getPositionX(robotIndex, state) == x) {
            return state;
        }

        return encodeX(x, robotIndex, state);
    }

    public static int withPositionY(final int robotIndex, final int y, final int state) {
        if (getPositionY(robotIndex, state) == y) {
            return state;
        }

        return encodeY(y, robotIndex, state);
    }

    public static int getPositionX(final int robotIndex, final int state) {
        return decodeX(robotIndex, state);
    }

    public static int getPositionY(final int robotIndex, final int state) {
        return decodeY(robotIndex, state);
    }

    public static boolean isSamePosition(final int state1, final int state2, final int robotIndex) {
        return decodePosition(robotIndex, state1) == decodePosition(robotIndex, state2);
    }

    /**
     * @param x          position
     * @param y          position
     * @param robotIndex the robot to ignore
     * @return true if another robot (not robotIndex) is blocking position (x,y) else false
     */
    public static boolean isOtherRobotBlocking(final int x, final int y, final int robotIndex, final int state) {
        for (int otherRobotIndex = 0; otherRobotIndex < MAX_ROBOT_COUNT; otherRobotIndex++) {
            if (otherRobotIndex == robotIndex) {
                continue;
            }
            if (getPositionX(otherRobotIndex, state) == NOT_DEFINED) {
                return false;
            }

            if (isBlocking(otherRobotIndex, x, y, state)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param x position
     * @param y position
     * @return the index [0..robotCount-1] of the robot at position x,y. -1 if no robot is found.
     */
    public static int getRobotAtPosition(final int x, final int y, final int state) {
        for (int robotIndex = 0; robotIndex < MAX_ROBOT_COUNT; robotIndex++) {
            if (getPositionX(robotIndex, state) == NOT_DEFINED) {
                return -1;
            }

            if (isBlocking(robotIndex, x, y, state)) {
                return robotIndex;
            }
        }
        return -1;
    }

    private static boolean isBlocking(final int robotIndex, final int x, final int y, final int state) {
        return getPositionX(robotIndex, state) == x && getPositionY(robotIndex, state) == y;
    }

    public static String toString(final int state) {
        return "%s : [%s]".formatted(toStringBin(state), state);
    }

    public static String toStringBin(final int stateIn) {
        final StringBuilder sb = new StringBuilder(32);
        int state = stateIn;
        for (int i = 0; i < BITS_TOTAL; i++) {
            if ((state & 0b1) == 0b1) {
                sb.append('1');
            } else {
                sb.append('0');
            }

            state = state >> 1;
        }
        final String s = sb.reverse().toString();
        return "STATE = %s_%s_%s_%s_%s_%s_%s".formatted(
                s.substring(0, 2),
                s.substring(2, 6),
                s.substring(6, 10),
                s.substring(10, 14),
                s.substring(14, 18),
                s.substring(18, 22),
                s.substring(22, 26)
        );
    }
}
