package net.booru.slidingrobots.algorithm.model;

import net.booru.slidingrobots.state.RobotsStateUtil;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution {
    private final List<Integer> iSolutionPath;
    private final Statistics iStatistics;
    private final String iAlgorithmName;

    public Solution(final List<Integer> solutionPath, final Statistics statistics) {
        iSolutionPath = solutionPath;
        iStatistics = statistics;
        iAlgorithmName = "no name";
    }

    public Solution(final List<Integer> solutionPath, final Statistics statistics, final String algorithmName) {
        iSolutionPath = List.copyOf(solutionPath);
        iStatistics = statistics;
        iAlgorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return iAlgorithmName;
    }

    /**
     * @return the solution path from (and including) the start state to the end state.
     * The length of the solution path is thus 1+ solution length.
     */
    public List<Integer> getSolutionPath() {
        return iSolutionPath;
    }

    public Statistics getStatistics() {
        return iStatistics;
    }

    public boolean isEmpty() {
        return iSolutionPath.isEmpty();
    }

    public String toJsonOutputString() {
        if (isEmpty()) {
            return "{}";
        } else {
            return RobotsStateUtil.toStringJsonResult(getSolutionPath()) + "\n";
        }
    }

    public List<String> toStringVerbose(final int verboseLevel) {
        if (isEmpty()) {
            return List.of("No solution!");
        } else {
            if (verboseLevel < 0) {
                return List.of();
            }

            final List<String> lines = new ArrayList<>();
            lines.addAll(Arrays.asList(getStatistics().toString().split("\\n")));
            lines.add("Solution: ");
            lines.add("");
            lines.add(RobotsStateUtil.toStringShortMove(getSolutionPath()));
            lines.add("");
            lines.addAll(RobotsStateUtil.toStringHumanReadable(getSolutionPath()));
            lines.add("");
            lines.add(RobotsStateUtil.toStringJsonResult(getSolutionPath()));
            lines.add("");
            return lines;
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "No solution!";
        } else {
            return getStatistics() + "\n" +
                    "Solution: " + "\n" +
                    Strings.join(RobotsStateUtil.toStringHumanReadable(getSolutionPath()), '\n');
        }
    }

}
