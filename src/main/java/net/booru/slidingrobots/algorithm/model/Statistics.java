package net.booru.slidingrobots.algorithm.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mutable statistics object created during search.
 */
public class Statistics {
    private int iStatesCreated;
    private int iStatesVisited;
    private int iStatesSeen;
    private int iSolutionLength;
    private double iTime;
    private HashMap<Integer, Integer> iSolutionLengthCounts;

    public void increaseStatesCreated(int by) {
        iStatesCreated += by;
    }

    public void increaseStatesVisited(int by) {
        iStatesVisited += by;
    }

    public void increaseStatesSeen() {
        iStatesSeen++;
    }

    public void setSolutionLength(int length) {
        iSolutionLength = length;
    }

    public int getStatesCreated() {
        return iStatesCreated;
    }

    public int getStatesVisited() {
        return iStatesVisited;
    }

    public int getStatesSeen() {
        return iStatesSeen;
    }

    public int getSolutionLength() {
        return iSolutionLength;
    }

    public int getSolutionLengthCount(final int additionalMoves) {
        return iSolutionLengthCounts.getOrDefault(iSolutionLength + additionalMoves, 0);
    }

    public List<SolutionLengthCount> getSolutionLengths() {
        return iSolutionLengthCounts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new SolutionLengthCount(e.getKey(), e.getValue())).toList();
    }

    public double getTime() {
        return iTime;
    }

    @Override
    public String toString() {
        var counts = iSolutionLengthCounts == null
                ? ""
                : getSolutionLengths();

        return String.format("""
                        Statistics:
                              Time (ms):       %f
                              Solution length: %d
                              States Created:  %d
                              States Visited:  %d
                              States Seen:     %d
                              Solution Length counts: %s""",
                iTime, iSolutionLength, iStatesCreated, iStatesVisited, iStatesSeen, counts);
    }

    public void setTime(final double time) {
        iTime = time;
    }

    /**
     * @param startNodes the ordered solutions, best first
     */
    public void addSolutionsCounts(final Collection<Node> startNodes, final int solutionCount) {
        final int best = startNodes.iterator().next().depth();
        iSolutionLengthCounts = new HashMap<>();
        startNodes.stream()
                .takeWhile(node -> node.depth() <= best + solutionCount)
                .forEach(node -> {
                    final int count = iSolutionLengthCounts.getOrDefault(node.depth(), 0);
                    iSolutionLengthCounts.put(node.depth(), count + 1);
                });
    }
}
