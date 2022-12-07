package net.booru.slidingrobots.state;

import net.booru.slidingrobots.common.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsStateTest {
    @Test
    void testEncodeDecode() {
        final int waypointsReached = 3;
        final byte[] positions = {0, 1, 2, 3, 4, 5};
        final int state = RobotsState.encodeState(waypointsReached, positions);
        final Pair<Integer, byte[]> integerPair = RobotsState.decodeState(state);

        assertEquals(waypointsReached, integerPair.first);
        assertArrayEquals(positions, integerPair.second);
    }

    @Test
    void testEncodeDecodeOneRobot() {
        final int waypointsReached = 1;
        final byte[] positions = {1, 2};
        final int state = RobotsState.encodeState(waypointsReached, positions);
        final Pair<Integer, byte[]> decoded = RobotsState.decodeState(state);

        assertEquals(waypointsReached, decoded.first);
        assertArrayEquals(positions, decoded.second);
    }

    @Test
    void testGetParts() {
        final int robotsState = RobotsState.valueOf(new byte[]{0, 1, 2, 3, 4, 5}, 3);

        assertEquals(3, RobotsState.getRobotCount(robotsState));

        assertEquals(3, RobotsState.getWaypointsReached(robotsState));

        assertEquals(0, RobotsState.getPositionX(0, robotsState));
        assertEquals(1, RobotsState.getPositionY(0, robotsState));

        assertEquals(2, RobotsState.getPositionX(1, robotsState));
        assertEquals(3, RobotsState.getPositionY(1, robotsState));

        assertEquals(4, RobotsState.getPositionX(2, robotsState));
        assertEquals(5, RobotsState.getPositionY(2, robotsState));
    }

    @Test
    void testNextWaypoint() {
        final byte[] positions = {0, 1, 2, 3, 4, 5};
        final int r1 = RobotsState.valueOf(positions, 0);
        final int r2 = RobotsState.valueOf(positions, 1);
        final int r3 = RobotsState.valueOf(positions, 2);
        assertEquals(1, RobotsState.getWaypointsReached(RobotsState.withNextWaypoint(r1)));
        assertEquals(2, RobotsState.getWaypointsReached(RobotsState.withNextWaypoint(r2)));
        assertEquals(3, RobotsState.getWaypointsReached(RobotsState.withNextWaypoint(r3)));

        final int rs = RobotsState.valueOf(positions, RobotsState.MAX_WAYPOINT);
        assertThrows(IllegalStateException.class, () -> RobotsState.withNextWaypoint(rs));

    }

    @Test
    void testIsSamePosition() {
        final int r1 = RobotsState.valueOf(new byte[]{4, 4, 1, 2, 4, 4}, 0);
        final int r2 = RobotsState.valueOf(new byte[]{3, 3, 1, 2, 3, 3}, 0);
        final int r3 = RobotsState.valueOf(new byte[]{5, 5, 0, 2, 5, 5}, 0);

        assertTrue(RobotsState.isSamePosition(r1, r1, 0));
        assertTrue(RobotsState.isSamePosition(r1, r1, 1));
        assertTrue(RobotsState.isSamePosition(r1, r1, 2));

        assertFalse(RobotsState.isSamePosition(r1, r2, 0));
        assertTrue(RobotsState.isSamePosition(r1, r2, 1));
        assertFalse(RobotsState.isSamePosition(r1, r2, 2));

        assertFalse(RobotsState.isSamePosition(r1, r3, 0));
        assertFalse(RobotsState.isSamePosition(r1, r3, 1));
        assertFalse(RobotsState.isSamePosition(r1, r3, 2));
    }

    @Test
    void testSetXAndY() {
        final byte[] positions = {1, 2, 3, 4, 5, 6};
        final var rs = RobotsState.valueOf(positions, 0);
        assertEquals(1, RobotsState.getPositionX(0, rs));
        assertEquals(14, RobotsState.getPositionX(0, RobotsState.withPositionX(0, 14, rs)));
        assertEquals(3, RobotsState.getPositionX(1, rs));
        assertEquals(14, RobotsState.getPositionX(1, RobotsState.withPositionX(1, 14, rs)));
        assertEquals(5, RobotsState.getPositionX(2, rs));
        assertEquals(14, RobotsState.getPositionX(2, RobotsState.withPositionX(2, 14, rs)));

        assertEquals(2, RobotsState.getPositionY(0, rs));
        assertEquals(14, RobotsState.getPositionY(0, RobotsState.withPositionY(0, 14, rs)));
        assertEquals(4, RobotsState.getPositionY(1, rs));
        assertEquals(14, RobotsState.getPositionY(1, RobotsState.withPositionY(1, 14, rs)));
        assertEquals(6, RobotsState.getPositionY(2, rs));
        assertEquals(14, RobotsState.getPositionY(2, RobotsState.withPositionY(2, 14, rs)));
    }

    @Test
    void testRobotCount() {
        final int r1 = RobotsState.valueOf(new byte[]{1, 2, 3, 4, 5, 6}, 0);
        final int r2 = RobotsState.valueOf(new byte[]{1, 2, 3, 4}, 1);
        final int r3 = RobotsState.valueOf(new byte[]{1, 2}, 1);
        final int r4 = RobotsState.valueOf(new byte[]{}, 3);
        assertEquals(3, RobotsState.getRobotCount(r1));
        assertEquals(2, RobotsState.getRobotCount(r2));
        assertEquals(1, RobotsState.getRobotCount(r3));
        assertEquals(0, RobotsState.getRobotCount(r4));
    }
}