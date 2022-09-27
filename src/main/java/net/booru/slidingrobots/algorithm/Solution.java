package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.RobotsState;
import net.booru.slidingrobots.state.RobotsStateUtil;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution {
    private final List<RobotsState> iSolutionPath;
    private final Statistics iStatistics;
    private final String iAlgorithmName;

    public Solution(final List<RobotsState> solutionPath, final Statistics statistics) {
        iSolutionPath = solutionPath;
        iStatistics = statistics;
        iAlgorithmName = "no name";
    }

    public Solution(final List<RobotsState> solutionPath, final Statistics statistics, final String algorithmName) {
        iSolutionPath = solutionPath;
        iStatistics = statistics;
        iAlgorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return iAlgorithmName;
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
