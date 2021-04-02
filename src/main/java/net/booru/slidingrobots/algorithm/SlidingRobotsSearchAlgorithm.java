package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.RobotsState;

import java.util.List;

public interface SlidingRobotsSearchAlgorithm {
    Solution run(final RobotsState startState, final List<EndCriterion> endCriteria) throws NoSolutionException;
}
