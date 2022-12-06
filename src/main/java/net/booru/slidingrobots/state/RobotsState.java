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
    static final int MAX_ROBOTS_COUNT = 3;
    static final int BITS_TOTAL = BITS_PER_POSITION * 2 * MAX_ROBOTS_COUNT + BITS_FOR_WAYPOINTS;
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

    private int iActualRobotCount = -1;

    private final int iState;

    public static void resetRobotsState(final int maxValueAnyDimension) {
        // bit-string length... this is not done often
        if (Integer.toBinaryString(maxValueAnyDimension).length() > BITS_PER_POSITION) {
            throw new IllegalArgumentException("We can support a maximum of 4 bits per position element");
        }
    }


    /**
     * @param robotPositions   the positions of robots as follows [main_robot_x, main_robot_y, helper_robot_1_x,
     *                         helper_robot_2_x, ..., helper_robot_n_x, helper_robot_n_x]
     * @param waypointsReached the goals that have been reached starting with the first goal at 0. No goal has the value of -1
     */
    public RobotsState(final byte[] robotPositions, final int waypointsReached) {
        iState = encodeState(waypointsReached, robotPositions);
    }

    public static RobotsState valueOf(final List<Pair<Point, Piece>> robots) {
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

        return new RobotsState(robotPositions, 0);
    }

    private RobotsState(final int state) {
        iState = state;
    }

    public static int encodeState(final int waypointsReached, final byte[] positions) {
        int state = waypointsReached;

        for (int i = MAX_ROBOTS_COUNT * 2; i > positions.length; i--) {
            state = (state << BITS_PER_POSITION) | NOT_DEFINED;
        }

        for (int i = positions.length; i > 0; i--) {
            state = (state << BITS_PER_POSITION) | positions[i - 1];
        }

        return state;
    }

    public static Pair<Integer, byte[]> decodeState(final int state) {

        final byte[] positions = new byte[MAX_ROBOTS_COUNT * 2];
        for (int i = 0; i < MAX_ROBOTS_COUNT; i++) {
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

    public static int increaseWaypoint(final int state) {
        final int currentWaypoint = (state & MASK_WAYPOINT) >> SHIFTS_WAYPOINT;
        if (currentWaypoint >= MAX_WAYPOINT) {
            throw new IllegalStateException("waypoint maximum reached");
        }
        final int updatedWaypoint = (currentWaypoint + 1) << SHIFTS_WAYPOINT;
        final int newState = (state & MASK_ROBOT_POSITIONS) | updatedWaypoint;

        return newState;
    }

    public static int decodeWaypoint(final int stateId) {
        final int currentWaypoint = (stateId & MASK_WAYPOINT) >> SHIFTS_WAYPOINT;
        return currentWaypoint;
    }

    public static int decodeX(int robotIndex, int state) {
        return (state & MASK_ROBOT_X[robotIndex]) >> SHIFTS_ROBOT_X[robotIndex];
    }

    public static int decodeY(int robotIndex, int state) {
        return (state & MASK_ROBOT_Y[robotIndex]) >> SHIFTS_ROBOT_Y[robotIndex];
    }

    private int decodePosition(final int robotIndex, final int state) {
        return (state & MASK_ROBOT[robotIndex]) >> SHIFTS_ROBOT[robotIndex];
    }

    public static int encodeX(int x, int robotIndex, int state) {
        final int encodedX = x << SHIFTS_ROBOT_X[robotIndex];
        final int newState = (state & ~MASK_ROBOT_X[robotIndex]) | encodedX;
        return newState;
    }

    public static int encodeY(int y, int robotIndex, int state) {
        final int encodedY = y << SHIFTS_ROBOT_Y[robotIndex];
        final int newState = (state & ~MASK_ROBOT_Y[robotIndex]) | encodedY;
        return newState;
    }

    public int getRobotCount() {
        if (iActualRobotCount == -1) {
            iActualRobotCount = 0;
            final int positions = iState & MASK_ROBOT_POSITIONS;
            for (int i = MAX_ROBOTS_COUNT - 1; i >= 0; i--) {
                if ((positions & MASK_ROBOT[i]) != MASK_ROBOT[i]) {
                    iActualRobotCount = i + 1;
                    break;
                }
            }
        }
        return iActualRobotCount;
    }

    public int getId() {
        return iState;
    }

    public int getWaypointsReached() {
        return decodeWaypoint(iState);
    }

    public RobotsState withNextWaypoint() {
        return new RobotsState(increaseWaypoint(iState));
    }

    public RobotsState withPositionX(final int robotIndex, final int x) {
        if (getPositionX(robotIndex) == x) {
            return this;
        }

        return new RobotsState(encodeX(x, robotIndex, iState));
    }

    public RobotsState withPositionY(final int robotIndex, final int y) {
        if (getPositionY(robotIndex) == y) {
            return this;
        }

        return new RobotsState(encodeY(y, robotIndex, iState));
    }

    public int getPositionX(final int robotIndex) {
        return decodeX(robotIndex, iState);
    }

    public int getPositionY(final int robotIndex) {
        return decodeY(robotIndex, iState);
    }

    public boolean isSamePosition(final RobotsState robotsState, final int robotIndex) {
        return decodePosition(robotIndex, robotsState.iState) == decodePosition(robotIndex, iState);
    }

    /**
     * @param x          position
     * @param y          position
     * @param robotIndex the robot to ignore
     * @return true if another robot (not robotIndex) is blocking position (x,y) else false
     */
    public boolean isOtherRobotBlocking(final int x, final int y, final int robotIndex) {
        for (int i = 0, len = getRobotCount(); i < len; i++) {
            if (i == robotIndex) {
                continue;
            }

            if (isBlocking(i, x, y)) {
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
    public int getRobotAtPosition(final int x, final int y) {
        for (int i = 0, len = getRobotCount(); i < len; i++) {
            if (isBlocking(i, x, y)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBlocking(final int robotIndex, final int x, final int y) {
        return getPositionX(robotIndex) == x && getPositionY(robotIndex) == y;
    }

    @Override
    public String toString() {
        return "%s : [%s]".formatted(toStringBin(iState), iState);
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RobotsState that = (RobotsState) o;
        return iState == that.iState; // since unique per state
    }

    @Override
    public int hashCode() {
        return iState; // since unique per state
    }
}
