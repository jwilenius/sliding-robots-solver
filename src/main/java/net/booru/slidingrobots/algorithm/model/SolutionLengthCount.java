package net.booru.slidingrobots.algorithm.model;

public record SolutionLengthCount(int solutionMoves, int solutionCount) {
    @Override
    public String toString() {
        return String.format("(L=%d : #=%d)", solutionMoves, solutionCount);
    }
}
