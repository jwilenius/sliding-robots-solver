package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.RobotsState;

import java.util.Objects;

/**
 * Keep track of the path to the end state.
 */
public class Node {
    private final Node iPreviousNode;
    private final RobotsState iState;
    private final int iDepth;

    public Node(final RobotsState state, final Node previous) {
        this(state, previous, 0);
    }

    public Node(final RobotsState state, final Node previous, final int depth) {
        Objects.requireNonNull(state);
        iState = state;
        iPreviousNode = previous;
        iDepth = depth;
    }

    public boolean hasPreviousNode() {
        return iPreviousNode != null;
    }

    public RobotsState getState() {
        return iState;
    }

    public Node getPreviousNode() {
        return iPreviousNode;
    }

    public int getDepth() {
        return iDepth;
    }

    @Override
    public String toString() {
        return iState.toString() + ":" + iDepth;
    }
}
