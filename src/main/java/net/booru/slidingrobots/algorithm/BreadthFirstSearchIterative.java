package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BreadthFirstSearchIterative implements SlidingRobotsSearchAlgorithm {
    public static final int DEPTH_LIMIT = 1000;

    private final Board iBoard;

    /**
     * @param board the static board that we can make moves on
     */
    public BreadthFirstSearchIterative(final Board board) {
        iBoard = board;
    }

    /**
     * @param startState  the initial state of the robotss.
     * @param endCriteria the definition of the sequential targets we must reach in the game
     *
     * @return The solution path including the start state, or empty if no solution was found.
     */
    @Override
    public Solution run(final RobotsState startState, final List<EndCriterion> endCriteria) throws NoSolutionException {
        final Timer timer = new Timer();

        final Statistics mutableStatistics = new Statistics();
        final Node startNode = new Node(startState, null, 0);
        final Node endNode = findPathToAllTargets(startNode, endCriteria, mutableStatistics);
        final LinkedList<RobotsState> solutionPath = RobotsStateUtil.extractRobotStatesFromNodePath(endNode);
        timer.close();

        mutableStatistics.setSolutionLength(solutionPath.size() - 1); // path includes start state
        mutableStatistics.setTime(timer.getDurationMillis());

        return new Solution(solutionPath, mutableStatistics);
    }

    private Node findPathToAllTargets(final Node startNode, final List<EndCriterion> endCriteria,
                                      final Statistics mutableStatistics)
            throws NoSolutionException {

        final Queue<EndCriterion> remainingCriteria = new ArrayDeque<>(endCriteria);

        List<Node> startNodes = new LinkedList<>(List.of(startNode));
        while (!remainingCriteria.isEmpty()) {
            // For all intermediate targets we need all shortest solutions
            final EndCriterion endCriterion = remainingCriteria.poll();
            final boolean isFindAllShortestSolutions = !remainingCriteria.isEmpty();

            startNodes = searchBFS(startNodes, endCriterion, isFindAllShortestSolutions, mutableStatistics);

            if (startNodes.isEmpty()) {
                throw new NoSolutionException();
            } else if (remainingCriteria.isEmpty()) {
                // final target was reached, pick first best solution
                return startNodes.get(0);
            }
        }
        throw new NoSolutionException();
    }

    private List<Node> searchBFS(final List<Node> startNodes,
                                 final EndCriterion endCriterion,
                                 final boolean isFindAllShortestSolutions,
                                 final Statistics mutableStatistics) {

        final Queue<Node> nodesToExpand = new LinkedList<>(startNodes);
        final Set<RobotsState> seenStates = new HashSet<>(100000);
        for (var node : nodesToExpand) {
            seenStates.add(node.getState());
        }

        final List<Node> solutions = new LinkedList<>();

        int solutionDepth = DEPTH_LIMIT;
        while (!nodesToExpand.isEmpty()) {
            final Node currentNode = nodesToExpand.poll();
            mutableStatistics.increaseStatesVisited(1);

            if (endCriterion.isSatisfied(currentNode)) {
                if (!isFindAllShortestSolutions) {
                    return List.of(currentNode);
                }

                solutions.add(currentNode);
                if (currentNode.getDepth() < solutionDepth) { // only clear too deep nodes once (ok since BFS)
                    nodesToExpand.removeIf(node -> node.getDepth() > currentNode.getDepth());
                    solutionDepth = currentNode.getDepth();
                }

            } else if (currentNode.getDepth() < solutionDepth) {
                // Not done, find not too deep neighbors and add them last in the queue
                final List<RobotsState> neighbors = currentNode.getState().getNeighbors(iBoard, seenStates);
                for (RobotsState neighbor : neighbors) {
                    nodesToExpand.offer(new Node(neighbor, currentNode, currentNode.getDepth() + 1));
                    seenStates.add(neighbor);
                }
                mutableStatistics.increaseStatesCreated(neighbors.size());
            }
        }

        return solutions;
    }
}
