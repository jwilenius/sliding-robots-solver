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
        final RobotsState robotsState = new RobotsState(new byte[]{0, 1, 2, 3, 4, 5}, 3);

        assertEquals(3, robotsState.getRobotCount());

        assertEquals(3, robotsState.getWaypointsReached());

        assertEquals(0, robotsState.getPositionX(0));
        assertEquals(1, robotsState.getPositionY(0));

        assertEquals(2, robotsState.getPositionX(1));
        assertEquals(3, robotsState.getPositionY(1));

        assertEquals(4, robotsState.getPositionX(2));
        assertEquals(5, robotsState.getPositionY(2));
    }

    @Test
    void testNextWaypoint() {
        final byte[] positions = {0, 1, 2, 3, 4, 5};
        assertEquals(1, new RobotsState(positions, 0).withNextWaypoint().getWaypointsReached());
        assertEquals(2, new RobotsState(positions, 1).withNextWaypoint().getWaypointsReached());
        assertEquals(3, new RobotsState(positions, 2).withNextWaypoint().getWaypointsReached());

        final var rs = new RobotsState(positions, RobotsState.MAX_WAYPOINT);
        assertThrows(IllegalStateException.class, rs::withNextWaypoint);

    }

    @Test
    void testIsSamePosition() {
        final RobotsState r1 = new RobotsState(new byte[]{4, 4, 1, 2, 4, 4}, 0);
        final RobotsState r2 = new RobotsState(new byte[]{3, 3, 1, 2, 3, 3}, 0);
        final RobotsState r3 = new RobotsState(new byte[]{5, 5, 0, 2, 5, 5}, 0);

        assertTrue(r1.isSamePosition(r1, 0));
        assertTrue(r1.isSamePosition(r1, 1));
        assertTrue(r1.isSamePosition(r1, 2));

        assertFalse(r1.isSamePosition(r2, 0));
        assertTrue(r1.isSamePosition(r2, 1));
        assertFalse(r1.isSamePosition(r2, 2));

        assertFalse(r1.isSamePosition(r3, 0));
        assertFalse(r1.isSamePosition(r3, 1));
        assertFalse(r1.isSamePosition(r3, 2));

    }

    @Test
    void testSetXAndY() {
        final byte[] positions = {1, 2, 3, 4, 5, 6};
        final var rs = new RobotsState(positions, 0);
        assertEquals(1, rs.getPositionX(0));
        assertEquals(14, rs.withPositionX(0, 14).getPositionX(0));
        assertEquals(3, rs.getPositionX(1));
        assertEquals(14, rs.withPositionX(1, 14).getPositionX(1));
        assertEquals(5, rs.getPositionX(2));
        assertEquals(14, rs.withPositionX(2, 14).getPositionX(2));

        assertEquals(2, rs.getPositionY(0));
        assertEquals(14, rs.withPositionY(0, 14).getPositionY(0));
        assertEquals(4, rs.getPositionY(1));
        assertEquals(14, rs.withPositionY(1, 14).getPositionY(1));
        assertEquals(6, rs.getPositionY(2));
        assertEquals(14, rs.withPositionY(2, 14).getPositionY(2));
    }

    @Test
    void testRobotCount() {
        assertEquals(3, new RobotsState(new byte[]{1, 2, 3, 4, 5, 6}, 0).getRobotCount());
        assertEquals(2, new RobotsState(new byte[]{1, 2, 3, 4}, 1).getRobotCount());
        assertEquals(1, new RobotsState(new byte[]{1, 2}, 1).getRobotCount());
        assertEquals(0, new RobotsState(new byte[]{}, 3).getRobotCount());
    }
}