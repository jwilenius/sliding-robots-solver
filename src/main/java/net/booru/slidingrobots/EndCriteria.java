package net.booru.slidingrobots;

/**
 * A mutable end state tracker that knows when we reach the end state in the search.
 */
public class EndCriteria {
    private final boolean iIsOneWay;
    private final Board iBoard;
    private boolean iIsGoalReached;
    private boolean iIsStartReached;

    /**
     * @param board    the board to check the end criteria against
     * @param isOneWay defines if the end is reached when we reach the goal (true) or after we reach the goal and return
     *                 back to start (false).
     */
    public EndCriteria(final Board board, final boolean isOneWay) {
        iBoard = board;
        iIsOneWay = isOneWay;
    }

    /**
     * Check if the search has reached the required end criteria.
     *
     * @param robotsState the state
     *
     * @return true if the end criteria has been reached.
     */
    public Result checkAndUpdate(final RobotsState robotsState) {
        if (iIsOneWay) {
            iIsGoalReached = isGoalReached(robotsState);
            return iIsGoalReached ? Result.FULL : Result.NONE;
        } else { // Two way game
            final boolean isGoalReached = isGoalReached(robotsState);
            if (isGoalReached && !iIsGoalReached) {
                // We have reached the first part of the goal
                iIsGoalReached = true;
                return Result.PARTIAL; // only return this once!
            }

            if (iIsGoalReached) {
                // only check if in start after goal is reached.
                iIsStartReached = isStartReached(robotsState);
                return iIsStartReached ? Result.FULL : Result.NONE;
            }
        }

        return Result.NONE;
    }

    private boolean isGoalReached(final RobotsState robotsState) {
        return iIsGoalReached || iBoard.isGoalReached(robotsState);
    }

    private boolean isStartReached(final RobotsState robotsState) {
        return iIsStartReached || iBoard.isStartReached(robotsState);
    }

    public enum Result {
        NONE,
        PARTIAL,
        FULL;
    }

}
