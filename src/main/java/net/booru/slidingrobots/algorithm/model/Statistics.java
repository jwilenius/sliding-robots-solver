package net.booru.slidingrobots.algorithm.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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

    public double getTime() {
        return iTime;
    }

    @Override
    public String toString() {
        var counts = iSolutionLengthCounts == null
                ? ""
                : iSolutionLengthCounts.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(e -> String.format("(L=%d : #=%d)", e.getKey(), e.getValue()))
                .toList();

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
        final HashMap<Integer, Integer> countMap = new HashMap<>();
        startNodes.stream()
                .takeWhile(node -> node.depth() <= best + solutionCount)
                .forEach(node -> {
                    final int count = countMap.getOrDefault(node.depth(), 0);
                    countMap.put(node.depth(), count + 1);
                });

        iSolutionLengthCounts = countMap;
    }
}
