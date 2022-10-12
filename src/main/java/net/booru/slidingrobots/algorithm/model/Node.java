package net.booru.slidingrobots.algorithm.model;

import net.booru.slidingrobots.state.RobotsState;


/**
 * Keep track of the path to the end state.
 */
public record Node(RobotsState state, Node previousNode, int depth) {

    public Node withUpdatedGoalsReached() {
        return new Node(state.withNextGoal(), previousNode, depth);
    }

    @Override
    public String toString() {
        return state.toString() + ":" + depth;
    }
}
