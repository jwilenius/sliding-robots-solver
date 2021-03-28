package net.booru.slidingrobots;

/**
 * A mutable end state tracker that knows when we reach the end state in the search.
 */
public class EndCriteria {
    private final boolean iIsOneWay;
    private final Board iBoard;
    private final boolean iIsGoalReached;
    private final boolean iIsStartReached;
    private final Result iResult;

    /**
     * @param board    the board to check the end criteria against
     * @param isOneWay defines if the end is reached when we reach the goal (true) or after we reach the goal and return
     *                 back to start (false).
     */
    public EndCriteria(final Board board, final boolean isOneWay) {
        this(board, isOneWay, false, false, Result.NONE);
    }

    private EndCriteria(final Board board, final boolean isOneWay, final boolean isGoalReached,
                        final boolean isStartReached, final Result result) {
        iBoard = board;
        iIsOneWay = isOneWay;
        iIsGoalReached = isGoalReached;
        iIsStartReached = isStartReached;
        iResult = result;
    }

    /**
     * Check if the search has reached the required end criteria.
     *
     * @param robotsState the state
     *
     * @return true if the end criteria has been reached.
     */
    public EndCriteria update(final RobotsState robotsState) {
        if (iIsOneWay) {
            return isGoalReached(robotsState)
                    ? withStateUpdate(true, false, Result.FULL)
                    : withStateUpdate(false, false, Result.NONE);
        } else { // Two way game
            final boolean isGoalReached = isGoalReached(robotsState);
            if (isGoalReached && !iIsGoalReached) {
                // We have reached the first part of the goal
                return withStateUpdate(true, false, Result.PARTIAL); // only return this once!
            }

            // only check if start is reached after goal was reached.
            if (iIsGoalReached) {
                return isStartReached(robotsState)
                        ? withStateUpdate(true, true, Result.FULL)
                        : withStateUpdate(true, false, Result.NONE); // none since no new result
            }
        }

        return withStateUpdate(false, false, Result.NONE);
    }

    private EndCriteria withStateUpdate(final boolean isGoalReached,
                                        final boolean isStartReached,
                                        final Result result) {
        if (isGoalReached == iIsGoalReached && isStartReached == iIsStartReached && iResult == result) {
            return this;
        }
        return new EndCriteria(iBoard, iIsOneWay, isGoalReached, isStartReached, result);
    }


    private boolean isGoalReached(final RobotsState robotsState) {
        return iIsGoalReached || iBoard.isGoalReached(robotsState);
    }

    private boolean isStartReached(final RobotsState robotsState) {
        return iIsStartReached || iBoard.isStartReached(robotsState);
    }

    public Result getResult() {
        return iResult;
    }

    public enum Result {
        NONE,
        PARTIAL,
        FULL;
    }

}
