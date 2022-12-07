package net.booru.slidingrobots.algorithm.model;

import net.booru.slidingrobots.state.RobotsState;


/**
 * Keep track of the path to the end state.
 */
public record Node(int state, Node previousNode, int depth) {

    public Node withUpdatedGoalsReached() {
        return new Node(RobotsState.withNextWaypoint(state), previousNode, depth);
    }

    @Override
    public String toString() {
        return RobotsState.toString(state) + ": d=" + depth;
    }
}
