package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.common.Direction;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.RobotsState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BreadthFirstSearchRecursive {
    private final Board iBoard;
    private final EndCriteria iEndCriteria;

    /**
     * @param board       the static board that we can make moves on
     * @param endCriteria keeps track of the end criteria during search, knows when we have reach search target.
     */
    public BreadthFirstSearchRecursive(final Board board, final EndCriteria endCriteria) {
        iBoard = board;
        iEndCriteria = endCriteria;
    }

    /**
     * @return The solution path including the start state, or empty if no solution was found.
     */
    public Solution run(final RobotsState startState) {
        try {
            final Timer timer = new Timer();

            final LinkedList<Node> nodesToExpand = new LinkedList<>();
            final Set<RobotsState> seenStates = new HashSet<>(100000);
            final Node startNode = new Node(startState, null);
            nodesToExpand.add(startNode);
            seenStates.add(startNode.getState());

            final Statistics mutableStatistics = new Statistics();

            final Node endNode = searchBFS(nodesToExpand, seenStates, iEndCriteria, mutableStatistics);
            final LinkedList<RobotsState> solutionPath = extractRobotStatesFromNodePath(endNode);
            timer.close();

            mutableStatistics.setSolutionLength(solutionPath.size() - 1); // path includes start state
            mutableStatistics.setTime(timer.getDurationMillis());

            return new Solution(solutionPath, mutableStatistics);
        } catch (NoSolutionException e) {
            return new Solution(List.of(), new Statistics());
        }
    }


    private Node searchBFS(final LinkedList<Node> nodesToExpand,
                           final Set<RobotsState> seenState,
                           final EndCriteria endCriteria,
                           final Statistics mutableStatistics)
            throws NoSolutionException {

        if (nodesToExpand.isEmpty()) {
            throw new NoSolutionException();
        }

        final Node currentNode = nodesToExpand.poll();
        mutableStatistics.increaseStatesVisited(1);

        final EndCriteria updatedEndCriteria = endCriteria.update(currentNode.getState());
        final Result result = updatedEndCriteria.getResult();

        if (result == Result.FULL) {
            return currentNode;
        } else if (result == Result.PARTIAL) {
            seenState.clear(); // todo: this may need to be optimized? profile this.
            nodesToExpand.clear(); // todo: this may need to be optimized? profile this.
            seenState.add(currentNode.getState());
        }

        final List<RobotsState> expandedStates = expandFromState(currentNode, seenState);
        for (RobotsState expandedState : expandedStates) {
            nodesToExpand.add(new Node(expandedState, currentNode));
        }
        seenState.addAll(expandedStates);
        mutableStatistics.increaseStatesCreated(expandedStates.size());

        return searchBFS(nodesToExpand, seenState, updatedEndCriteria, mutableStatistics);
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

    private LinkedList<RobotsState> extractRobotStatesFromNodePath(final Node endNode) {
        final LinkedList<RobotsState> path = new LinkedList<>();
        for (Node node = endNode; node != null; node = node.getPreviousNode()) {
            path.addFirst(node.getState());
        }
        return path;
    }

}
