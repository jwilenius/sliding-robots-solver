package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BreadthFirstSearchRecursive implements SlidingRobotsSearchAlgorithm {
    private final Board iBoard;
    public static final int DEPTH_LIMIT = 1000;

    /**
     * @param board the static board that we can make moves on
     */
    public BreadthFirstSearchRecursive(final Board board) {
        iBoard = board;
    }

    /**
     * @param startState  the initial state of the robotss.
     * @param endCriteria keeps track of the end criteria during search, knows when we have reach search target.
     *
     * @return The solution path including the start state, or empty if no solution was found.
     */
    @Override
    public Solution run(final RobotsState startState, final List<EndCriterion> endCriteria) throws NoSolutionException {
        final Timer timer = new Timer();

        final Statistics mutableStatistics = new Statistics();
        final Node startNode = new Node(startState, null, 0);

        // Set up targets based on game type
        final Deque<EndCriterion> remainingCriteria = new ArrayDeque<>(endCriteria);
        final Node endNode = findPathToAllTargets(List.of(startNode), remainingCriteria, mutableStatistics);
        final LinkedList<RobotsState> solutionPath = RobotsStateUtil.extractRobotStatesFromNodePath(endNode);
        timer.close();

        mutableStatistics.setSolutionLength(solutionPath.size() - 1); // path includes start state
        mutableStatistics.setTime(timer.getDurationMillis());

        return new Solution(solutionPath, mutableStatistics);
    }

    private Node findPathToAllTargets(final List<Node> startNodes, final Deque<EndCriterion> remainingCriteria,
                                      final Statistics mutableStatistics)
            throws NoSolutionException {

        if (startNodes.isEmpty()) {
            throw new NoSolutionException();
        }

        if (remainingCriteria.isEmpty()) {
            return startNodes.get(0);
        }

        final Queue<Node> nodesToExpand = new LinkedList<>(startNodes);
        final Set<RobotsState> seenStates = new HashSet<>(100000);
        startNodes.stream().map(Node::getState).forEach(seenStates::add);
        final EndCriterion endCriterion = remainingCriteria.poll();

        final boolean isFindAllShortestSolutions = !remainingCriteria.isEmpty();
        final List<Node> nextStartNodes = searchBFS(nodesToExpand, seenStates, endCriterion, new LinkedList<>(),
                                                    DEPTH_LIMIT, isFindAllShortestSolutions, mutableStatistics);

        return findPathToAllTargets(nextStartNodes, remainingCriteria, mutableStatistics);
    }

    /**
     * @return all the shortest path solutions that that satisfies {@code endCriterion}.
     */
    private List<Node> searchBFS(final Queue<Node> nodesToExpand,
                                 final Set<RobotsState> seenStates,
                                 final EndCriterion endCriterion,
                                 final List<Node> solutions,
                                 final int solutionDepth,
                                 final boolean isFindAllSolutions,
                                 final Statistics mutableStatistics) {

        if (nodesToExpand.isEmpty()) {
            return solutions;
        }

        final Node currentNode = nodesToExpand.poll();
        mutableStatistics.increaseStatesVisited(1);

        if (endCriterion.isSatisfied(currentNode)) {
            if (!isFindAllSolutions) {
                return List.of(currentNode);
            } else {
                solutions.add(currentNode);

                final int updatedSolutionDepth = Math.min(solutionDepth, currentNode.getDepth());
                if (updatedSolutionDepth < solutionDepth) { // only clear too deep nodes once (ok since BFS)
                    nodesToExpand.removeIf(node -> node.getDepth() > currentNode.getDepth());
                }

                return searchBFS(nodesToExpand, seenStates, endCriterion, solutions,
                                 updatedSolutionDepth, isFindAllSolutions, mutableStatistics);
            }
        } else {
            if (currentNode.getDepth() < solutionDepth) {
                final List<RobotsState> neighbors = currentNode.getState().getNeighbors(iBoard, seenStates);
                for (RobotsState neighbor : neighbors) {
                    nodesToExpand.offer(new Node(neighbor, currentNode, currentNode.getDepth() + 1));
                }
                seenStates.addAll(neighbors);
                mutableStatistics.increaseStatesCreated(neighbors.size());
            }

            return searchBFS(nodesToExpand, seenStates, endCriterion, solutions,
                             solutionDepth, isFindAllSolutions, mutableStatistics);
        }
    }
}
