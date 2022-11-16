package net.booru.slidingrobots.rank;

import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.Direction;
import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Counts the moves and the number of bumps against other robots.
 */
public class BumpsCounter {

    public int apply(final Solution solution) {
        final Deque<RobotsState> solutionPath = new LinkedList<>(solution.getSolutionPath());
        RobotsState previousState = solutionPath.poll(); // discard start state

        int bumps = 0;
        while (!solutionPath.isEmpty()) {
            final RobotsState current = solutionPath.poll();
            bumps += countBumps(current, previousState);
            previousState = current;
        }

        return bumps;
    }

    private int countBumps(final RobotsState state, final RobotsState previousState) {

        final int robotThatMoved = RobotsStateUtil.getRobotIndexOfFirstDifference(previousState, state);
        final Direction moveDirection = RobotsStateUtil.getRobotMovementDirection(previousState, state);
        final int positionX = state.getPositionX(robotThatMoved);
        final int positionY = state.getPositionY(robotThatMoved);

        int bumpCount = 0;
        for (int otherRobot = 0; otherRobot < state.getRobotCount(); otherRobot++) {
            if (otherRobot == robotThatMoved) {
                continue;
            }

            final int xDist = Math.abs(positionX - state.getPositionX(otherRobot));
            final int yDist = Math.abs(positionY - state.getPositionY(otherRobot));
            if (moveDirection == Direction.down || moveDirection == Direction.up) {
                if (xDist == 0 && yDist == 1) {
                    bumpCount++;
                }
            } else { // left/right
                if (xDist == 1 && yDist == 0) {
                    bumpCount++;
                }
            }
        }

        return bumpCount;
    }
}
