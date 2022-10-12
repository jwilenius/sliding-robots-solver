package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.algorithm.model.Node;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.algorithm.model.Statistics;
import net.booru.slidingrobots.algorithm.model.Waypoint;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BreadthFirstSearchIterative implements SlidingRobotsSearchAlgorithm {
    private final Board iBoard;

    /**
     * @param board the static board that we can make moves on
     */
    public BreadthFirstSearchIterative(final Board board) {
        iBoard = board;
    }

    /**
     * @param startState  the initial state of the robots.
     *
     * @return The solution path including to the first goal state.
     */
    @Override
    public Solution run(final RobotsState startState) throws NoSolutionException {
        final Timer timer = new Timer();

        final Statistics mutableStatistics = new Statistics();

        final Node endNode = searchBFS(startState, mutableStatistics);
        final LinkedList<RobotsState> solutionPath = RobotsStateUtil.extractRobotStatesFromNodePath(endNode);
        timer.close();

        mutableStatistics.setSolutionLength(solutionPath.size() - 1); // path includes start state
        mutableStatistics.setTime(timer.getDurationMillis());

        return new Solution(solutionPath, mutableStatistics);
    }


    private Node searchBFS(final RobotsState startState,
                           final Statistics mutableStatistics)
            throws NoSolutionException {

        final LinkedList<Node> nodesToExpand = new LinkedList<>();
        final Set<RobotsState> seenStates = new HashSet<>(100000);
        final Node startNode = new Node(startState, null);
        nodesToExpand.add(startNode);
        seenStates.add(startNode.getState());

        while (!nodesToExpand.isEmpty()) {
            // Get the next node to visit
            final Node currentNode = nodesToExpand.poll();
            final RobotsState currentState = currentNode.getState();
            mutableStatistics.increaseStatesVisited(1);

            // Check if end criteria is met
            if (iBoard.isGoalReached(currentState)) {
                return currentNode;
            }

            // Not done, find neighbors and add them last in the queue
            final List<RobotsState> neighbors = currentState.getNeighbors(iBoard, seenStates);
            for (RobotsState neighbor : neighbors) {
                nodesToExpand.add(new Node(neighbor, currentNode));
                seenStates.add(neighbor);
            }
            mutableStatistics.increaseStatesCreated(neighbors.size());
        }

        throw new NoSolutionException();
    }
}
