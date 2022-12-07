package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.algorithm.model.Waypoint;

import java.util.List;

public interface SlidingRobotsSearchAlgorithm {
    Solution run(final int startState, final List<Waypoint> endCriteria) throws NoSolutionException;
}
