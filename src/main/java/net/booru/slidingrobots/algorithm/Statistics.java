package net.booru.slidingrobots.algorithm;

/**
 * A mutable statistics object created during search.
 */
public class Statistics {
    private int iStatesCreated;
    private int iStatesVisited;
    private int iSolutionLength;
    private double iTime;

    public void increaseStatesCreated(int by) {
        iStatesCreated += by;
    }

    public void increaseStatesVisited(int by) {
        iStatesVisited += by;
    }

    public void setSolutionLength(int by) {
        iSolutionLength = by;
    }

    public int getStatesCreated() {
        return iStatesCreated;
    }

    public int getStatesVisited() {
        return iStatesVisited;
    }

    public int getSolutionLength() {
        return iSolutionLength;
    }

    public double getTime() {
        return iTime;
    }

    @Override
    public String toString() {
        return String.format("Statistics: \n" +
                             "   Time (ms):       %f\n" +
                             "   Solution length: %d\n" +
                             "   States Created:  %d\n" +
                             "   States Visited:  %d\n",
                iTime, iSolutionLength, iStatesCreated, iStatesVisited);
    }

    public void setTime(final double time) {
        iTime = time;
    }
}
