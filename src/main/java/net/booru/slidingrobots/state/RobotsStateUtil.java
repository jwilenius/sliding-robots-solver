package net.booru.slidingrobots.state;

import net.booru.slidingrobots.algorithm.Node;
import net.booru.slidingrobots.common.Direction;
import net.booru.slidingrobots.common.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the current state of robot positions A RobotState is immutable.
 */
public final class RobotsStateUtil {

    public static RobotsState valueOf(final List<Pair<Point, Piece>> robots) {
        final Optional<Pair<Point, Piece>> mainRobot =
                robots.stream().filter(pair -> pair.second == Piece.main_robot).findFirst();
        if (mainRobot.isEmpty()) {
            throw new IllegalArgumentException("No main robot");
        }

        final List<Pair<Point, Piece>> helperRobots =
                robots.stream().filter(pair -> pair.second == Piece.helper_robot).collect(Collectors.toList());

        if (helperRobots.size() + 1 != robots.size()) {
            throw new IllegalArgumentException("Input contains other pieces than one main robot and helper robots");
        }

        final byte[] robotPositions = new byte[robots.size() * 2];
        robotPositions[0] = (byte) mainRobot.get().first.x;
        robotPositions[1] = (byte) mainRobot.get().first.y;

        for (int i = 0; i < helperRobots.size(); i++) {
            final Pair<Point, Piece> pointPiecePair = helperRobots.get(i);
            final int index = 2 * (i + 1);
            robotPositions[index] = (byte) pointPiecePair.first.x;
            robotPositions[index + 1] = (byte) pointPiecePair.first.y;
        }

        return new RobotsState(robotPositions);
    }

    /**
     * Nodes are a linked list, this method will extract the path of {@link RobotsState}s
     *
     * @param endNode a node
     *
     * @return the list of states from start state to the final state in endNode.
     */
    public static LinkedList<RobotsState> extractRobotStatesFromNodePath(final Node endNode) {
        final LinkedList<RobotsState> path = new LinkedList<>();
        for (Node node = endNode; node != null; node = node.getPreviousNode()) {
            path.addFirst(node.getState());
        }
        return path;
    }

    /**
     * Compute neighboring states to {@code currentState} and return them as long as they are not the same as {@code
     * currentState} and present in {@code seenStates}
     *
     * @param currentState the state to find the neighbors of
     * @param seenStates   the states that we have already seen
     *
     * @return the new unseen beighbors of {@code currentState}, may be empty.
     */
    public static List<RobotsState> getNeighbors(final Board board, final RobotsState currentState,
                                                 final Set<RobotsState> seenStates) {
        final int robotCount = currentState.getRobotCount();
        final Direction[] directions = Direction.values();

        final List<RobotsState> expandedState = new ArrayList<>(robotCount * directions.length);

        for (int robotIndex = 0; robotIndex < robotCount; robotIndex++) {
            for (Direction direction : directions) {
                //TODO possibly cache iBoard.makeMove
                final RobotsState state = board.makeMove(robotIndex, direction, currentState);
                if (state != currentState && !seenStates.contains(state)) {
                    expandedState.add(state);
                }
            }
        }
        return expandedState;
    }

    /**
     * Convert a list of RobotsState to a more easily readable format.
     * <p>
     * States are expected to be sequential where only one state position tuple (x,y) differs between two consecutive
     * states, else undefined behavior.
     *
     * @param moves a sequence of moves where for each pair of consecutive states only one robot has moved.
     *
     * @return a nice string representation of the moves
     */
    public static String toMovementsString(final List<RobotsState> moves) {
        final StringBuilder sb = new StringBuilder(100);

        sb.append("Moves: ");

        for (int i = 1; i < moves.size(); i++) {
            final RobotsState previousState = moves.get(i - 1);
            final RobotsState currentState = moves.get(i);
            final int robotIndex = indexOfFirstDifference(previousState, currentState);

            sb.append('(').append(currentState.getPositionX(robotIndex))
              .append(',').append(currentState.getPositionY(robotIndex)).append(')')
              .append(':').append(robotIndex)
              .append(' ');
        }

        return sb.toString();
    }

    /**
     * Convert a list of RobotsState to a more easily readable format.
     * <p>
     * States are expected to be sequential where only one state position tuple (x,y) differs between two consecutive
     * states, else undefined behavior.
     *
     * @param moves a sequence of moves where for each pair of consecutive states only one robot has moved.
     *
     * @return a nice string representation of the moves
     */
    public static String toMovementsStringVerbose(final List<RobotsState> moves) {
        final StringBuilder sb = new StringBuilder(100);

        sb.append("Moves: ");

        for (int i = 1; i < moves.size(); i++) {
            final RobotsState previousState = moves.get(i - 1);
            final RobotsState currentState = moves.get(i);
            final int robotIndex = indexOfFirstDifference(previousState, currentState);

            final String robotName = (robotIndex == 0) ? "MainRobot" : "HelperRobot-" + robotIndex;
            sb.append(robotName)
              .append(" -> (")
              .append(currentState.getPositionX(robotIndex))
              .append(", ")
              .append(currentState.getPositionY(robotIndex))
              .append(") ");
        }

        return sb.toString();
    }

    /**
     * Find the first robot index where the states differ.
     *
     * @param state1 the first state
     * @param state2 the second state != state1
     *
     * @return the first robot index where the states differ.
     */
    private static int indexOfFirstDifference(final RobotsState state1, final RobotsState state2) {
        for (int i = 0; i < state2.getRobotCount(); i++) {
            if (state1.getPositionX(i) != state2.getPositionX(i) ||
                state1.getPositionY(i) != state2.getPositionY(i)) {
                return i;
            }
        }

        throw new IllegalArgumentException("The two states do not differ");
    }
}
