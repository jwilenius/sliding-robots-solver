package net.booru.slidingrobots;

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
            StringBuilder sb = new StringBuilder();
            sb.append(getStatistics()).append("\n");
            sb.append("Solution: ").append("\n")
              .append(RobotsState.toMovementsString(getSolutionPath())).append("\n");
            return sb.toString();
        }
    }
}
