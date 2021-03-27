package net.booru.slidingrobots;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BreadthFirstSearchRecursive {
    private final Board iBoard;

    /**
     * Keep track of the path to the end state.
     */
    public static class Node {
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


    public BreadthFirstSearchRecursive(final Board board) {
        iBoard = board;
    }

    /**
     * @return The solution path including the start state, or empty if no solution was found.
     */
    public List<RobotsState> run(final RobotsState startState) {
        try {
            final LinkedList<Node> nodesToExpand = new LinkedList<>();
            final Set<RobotsState> seenStates = new HashSet<>(100000);
            final Node startNode = new Node(startState, null);
            nodesToExpand.add(startNode);
            seenStates.add(startNode.getState());

            final Node endNode = searchBFS(nodesToExpand, seenStates);

            return extractRobotStatesFromNodePath(endNode);
        } catch (NoSolutionException e) {
            return List.of();
        }
    }

    private LinkedList<RobotsState> extractRobotStatesFromNodePath(final Node endNode) {
        final LinkedList<RobotsState> path = new LinkedList<>();
        for (Node node = endNode; node != null; node = node.getPreviousNode()) {
            path.addFirst(node.getState());
        }
        return path;
    }

    private Node searchBFS(final LinkedList<Node> nodesToExpand, final Set<RobotsState> seenState)
            throws NoSolutionException {
        if (nodesToExpand.isEmpty()) {
            throw new NoSolutionException();
        }

        final Node currentNode = nodesToExpand.poll();

        if (iBoard.isGoalReached(currentNode.getState())) {
            return currentNode;
        }

        final List<RobotsState> expandedStates = expandFromState(currentNode, seenState);
        for (RobotsState expandedState : expandedStates) {
            nodesToExpand.add(new Node(expandedState, currentNode));
        }
        seenState.addAll(expandedStates);

        return searchBFS(nodesToExpand, seenState);
    }

    private List<RobotsState> expandFromState(final Node currentNode, final Set<RobotsState> seenStates) {
        final RobotsState currentState = currentNode.getState();
        final int robotCount = currentState.getRobotCount();
        final Direction[] directions = Direction.values();

        final List<RobotsState> expandedState = new ArrayList<>(robotCount * directions.length);

        for (int robotIndex = 0; robotIndex < robotCount; robotIndex++) {
            for (Direction direction : directions) {
                //TODO possibly cache iBoard.makeMove
                final RobotsState state = iBoard.makeMove(robotIndex, direction, currentState);
                if (state != currentState && !seenStates.contains(state)) {
                    expandedState.add(state);
                }
            }
        }
        // todo: apply strategy to expanded states before returning... e.g. randomize
        return expandedState;
    }

    private static class NoSolutionException extends Exception {
        public NoSolutionException() {
        }
    }
}
