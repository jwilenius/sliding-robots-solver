package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.state.RobotsState;

public interface SlidingRobotsSearchAlgorithm {
    Solution run(final RobotsState startState) throws NoSolutionException;
}
