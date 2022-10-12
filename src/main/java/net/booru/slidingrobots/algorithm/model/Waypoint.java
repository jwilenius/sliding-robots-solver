package net.booru.slidingrobots.algorithm.model;

import net.booru.slidingrobots.state.RobotsState;

public interface Waypoint {
    boolean isSatisfied(RobotsState robotsState);
}
