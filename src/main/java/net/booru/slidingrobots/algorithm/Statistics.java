package net.booru.slidingrobots.algorithm;

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
