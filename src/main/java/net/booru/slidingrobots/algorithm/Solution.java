package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;

import java.util.List;

public class Solution {
    private final List<RobotsState> iSolutionPath;
    private final Statistics iStatistics;

    public Solution(final List<RobotsState> solutionPath, final Statistics statistics) {
        iSolutionPath = solutionPath;
        iStatistics = statistics;
    }

    public List<RobotsState> getSolutionPath() {
        return iSolutionPath;
    }

    public Statistics getStatistics() {
        return iStatistics;
    }

    public boolean isEmpty() {
        return iSolutionPath.isEmpty();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "No solution!";
        } else {
            return getStatistics() + "\n" +
                   "Solution: " + "\n" +
                   RobotsStateUtil.toMovementsString(getSolutionPath()) + "\n";
        }
    }
}
