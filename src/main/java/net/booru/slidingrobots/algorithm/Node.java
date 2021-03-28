package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.RobotsState;

/**
 * Keep track of the path to the end state.
 */
public class Node {
    private final Node iPreviousNode;
    private final RobotsState iState;

    public Node(final RobotsState state, final Node previous) {
        iState = state;
        iPreviousNode = previous;
    }

    public RobotsState getState() {
        return iState;
    }

    public Node getPreviousNode() {
        return iPreviousNode;
    }

    @Override
    public String toString() {
        return iState.toString();
    }
}
