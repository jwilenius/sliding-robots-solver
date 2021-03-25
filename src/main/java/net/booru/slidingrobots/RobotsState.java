package net.booru.slidingrobots;

/**
 * Represents the current state of robot positions
 */
public final class RobotsState {
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
