package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.algorithm.model.Node;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.algorithm.model.Statistics;
import net.booru.slidingrobots.algorithm.model.Waypoint;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a somewhat naive solution that is generalized with {@link Waypoint} abstraction.
 * A list of {@link Waypoint} must be met in order. The search is (suboptimally) split into searching
 * breadth first to each goal. This is suboptimal as we need to save "all" partial solutions and use them as
 * starts nodes in the next step.
 */
public class BreadthFirstSearchIterative implements SlidingRobotsSearchAlgorithm {
    private static final Logger cLogger = LoggerFactory.getLogger(BreadthFirstSearchIterative.class);

    private final Board iBoard;
    private final int iDepthsToKeep;
    private final boolean iIsFindFirstSolution;

    /**
     * @param board the static board that we can make moves on
     */
    public BreadthFirstSearchIterative(final Board board) {
        iBoard = board;
        iDepthsToKeep = 0;
        iIsFindFirstSolution = true;
    }

    /**
     * @param board        the static board that we can make moves on
     * @param depthsToKeep keep looking for solutions until reaching best solution + {@code depthsToKeep}
     *                     if {@code depthsToKeep} is < 0 then stop at first solution
     */
    public BreadthFirstSearchIterative(final Board board, final int depthsToKeep) {
        iBoard = board;
        iIsFindFirstSolution = depthsToKeep < 0;
        iDepthsToKeep = Math.max(0, depthsToKeep);
    }

    /**
     * @param startState the initial state of the robots.
     * @param waypoints  the definition of the sequential targets we must reach in the game
     * @return The solution path including the start state, or empty if no solution was found.
     */
    @Override
    public Solution run(final int startState, final List<Waypoint> waypoints) throws NoSolutionException {
        final Timer timer = new Timer();
        final Statistics mutableStatistics = new Statistics();

        final Waypoint[] waypointMap = new Waypoint[waypoints.size() + 1];
        for (int i = 1; i <= waypoints.size(); i++) {
            waypointMap[i] = waypoints.get(i - 1);
        }

        final Node startNode = new Node(startState, null, 0);
        final List<Node> solutions = searchBFS(startNode, waypointMap, mutableStatistics);
        if (solutions.isEmpty()) {
            throw new NoSolutionException();
        }

        final List<Integer> solutionPath = RobotsStateUtil.extractRobotStatesFromNodePath(solutions.get(0));
        timer.stop();

        mutableStatistics.setSolutionLength(solutionPath.size() - 1); // path includes start state
        mutableStatistics.setTime(timer.getDurationMillis());
        mutableStatistics.addSolutionsCounts(solutions, Math.max(iDepthsToKeep, 0));

        return new Solution(solutionPath, mutableStatistics, this.getClass().getSimpleName());
    }

    /**
     * Expand in breath first order from startNode
     */
    private List<Node> searchBFS(final Node startNode,
                                 final Waypoint[] waypointMap,
                                 final Statistics mutableStatistics) {
        final BitSet seenStates = new BitSet(RobotsState.MAX_STATES);
        seenStates.set(startNode.state());

        final List<Node> solutions = new ArrayList<>(100);
        final Deque<Node> nodesToExpand = new LinkedList<>();
        nodesToExpand.add(startNode);

        final int finalWaypoint = waypointMap.length - 1;

        int bestSolutionDepth = Integer.MAX_VALUE;

        while (!nodesToExpand.isEmpty()) {
            final Node currentNode = nodesToExpand.poll();
            mutableStatistics.increaseStatesVisited(1);

            final int nextWaypoint = RobotsState.getWaypointsReached(currentNode.state()) + 1;
            final boolean isWaypointReached = waypointMap[nextWaypoint].isSatisfied(currentNode.state());

            if (isWaypointReached) {
                final Node currentNodeAdditionalGoal = currentNode.withUpdatedGoalsReached();
                if (finalWaypoint != nextWaypoint) {
                    nodesToExpand.addFirst(currentNodeAdditionalGoal); // expand the updated node next.
                } else {
                    bestSolutionDepth = Math.min(bestSolutionDepth, currentNodeAdditionalGoal.depth());

                    final boolean isTooDeep = currentNodeAdditionalGoal.depth() > bestSolutionDepth + iDepthsToKeep;
                    final boolean isStopSearching = isTooDeep || (iIsFindFirstSolution && !solutions.isEmpty());
                    if (isStopSearching) {
                        return solutions;
                    }

                    solutions.add(currentNodeAdditionalGoal);
                }
            } else {
                //TODO: we can save also the direction taken from parent to get to current, and not walk that way when generating
                // neighbors, can also be used to extract moves better.
                final List<Integer> neighbors = iBoard.getNeighbors(currentNode.state());
                for (int i = 0; i < neighbors.size(); i++) {
                    final int neighbor = neighbors.get(i);
                    if (seenStates.get(neighbor)) {
                        continue;
                    }

                    nodesToExpand.add(new Node(neighbor, currentNode, currentNode.depth() + 1));
                    seenStates.set(neighbor);
                    mutableStatistics.increaseStatesSeen();
                }
                mutableStatistics.increaseStatesCreated(neighbors.size());
            }
        }

        return solutions;
    }
}
