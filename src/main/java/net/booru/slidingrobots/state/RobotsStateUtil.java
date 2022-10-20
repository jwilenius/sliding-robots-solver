package net.booru.slidingrobots.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.booru.slidingrobots.algorithm.model.Node;
import net.booru.slidingrobots.common.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the current state of robot positions A RobotState is immutable.
 */
public final class RobotsStateUtil {
    private static final Logger cLogger = LoggerFactory.getLogger(RobotsStateUtil.class);

    private RobotsStateUtil() {
    }

    /**
     * Nodes are a linked list, this method will extract the path of {@link RobotsState}s
     *
     * @param endNode a node
     * @return the list of states from start state to the final state in endNode.
     */
    public static List<RobotsState> extractRobotStatesFromNodePath(final Node endNode) {
        final LinkedList<RobotsState> path = new LinkedList<>();
        for (Node node = endNode; node != null; node = node.previousNode()) {
            path.addFirst(node.state());
        }
        return path;
    }

    /**
     * Convert a list of RobotsState to a more easily readable format.
     * <p>
     * States are expected to be sequential where only one state position tuple (x,y) differs between two consecutive
     * states, else undefined behavior. That is it shows the target position and which piece was moved.
     *
     * @param moves a sequence of moves where for each pair of consecutive states only one robot has moved.
     * @return a nice string representation of the moves
     */
    public static String toStringShortMove(final List<RobotsState> moves) {
        final StringBuilder sb = new StringBuilder(100);

        sb.append("Moves \"piece_index to (to_pos_X, to_pos_Y)\":   ");

        for (int i = 1; i < moves.size(); i++) {
            final RobotsState previousState = moves.get(i - 1);
            final RobotsState currentState = moves.get(i);
            final int robotIndex = getRobotIndexOfFirstDifference(previousState, currentState);

            sb.append(robotIndex).append(" -> ")
                    .append('(').append(currentState.getPositionX(robotIndex))
                    .append(',').append(currentState.getPositionY(robotIndex)).append(')')
                    .append("  |  ");
        }

        return sb.toString();
    }

    // These have short names due to json export format
    public static class Pos {
        public final int x;
        public final int y;

        public Pos(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "{x=" + x + ", y=" + y + "}";
        }
    }

    // These have short names due to json export format
    public static class Dir {
        public final int dx;
        public final int dy;

        public Dir(final int dx, final int dy) {
            this.dx = (int) Math.signum(dx);
            this.dy = (int) Math.signum(dy);
        }

        @Override
        public String toString() {
            return "{dx=" + dx + ", dy=" + dy + "}";
        }
    }

    // These have short names due to json export format
    public static class Move {
        public final Dir dir;
        public final Pos pos;
        @JsonIgnore
        public final int robot;

        public Move(final Dir dir, final Pos pos, final int robot) {
            this.dir = dir;
            this.pos = pos;
            this.robot = robot;
        }

        @Override
        public String toString() {
            return "{Pos " + pos + " -> " + "Dir " + dir + "}";
        }
    }

    /**
     * This is the result the backend expects.
     *
     * @param states
     * @return the json string
     */
    public static String toStringJsonResult(final List<RobotsState> states) {
        /*
         [ {
             "dir": {"dx": 1,"dy": 0},
             "pos": {"x": 0,"y": 6}
           },
           ...
          ],
        */

        final List<Move> moves = getMoveList(states);

        final String listOfJson;
        try {
            listOfJson = new ObjectMapper().writeValueAsString(moves);
        } catch (JsonProcessingException e) {
            cLogger.error("Unexpected fail to export to json");
            return "";
        }
        return listOfJson;
    }

    public static List<String> toStringHumanReadable(final List<RobotsState> states) {
        final List<Move> moves = getMoveList(states);
        int step = 0;
        List<String> descriptions = new ArrayList<>();
        for (var move : moves) {
            descriptions.add(String.format("%2s: move %s %s", step++, move.robot, Direction.valueOf(move.dir.dx, move.dir.dy)));
        }
        return descriptions;
    }

    public static List<Move> getMoveList(final List<RobotsState> states) {
        final List<Move> moves = new ArrayList<>(states.size() - 1);

        for (int i = 1; i < states.size(); i++) {
            final RobotsState previousState = states.get(i - 1);
            final RobotsState currentState = states.get(i);
            final int robotIndex = getRobotIndexOfFirstDifference(previousState, currentState);

            final int x = previousState.getPositionX(robotIndex);
            final int y = previousState.getPositionY(robotIndex);
            final Pos pos = new Pos(x, y);

            final int newX = currentState.getPositionX(robotIndex);
            final int newY = currentState.getPositionY(robotIndex);
            final Dir dir = new Dir(newX - x, newY - y);

            final Move move = new Move(dir, pos, robotIndex);
            moves.add(move);
        }
        return moves;
    }

    /**
     * Find the first robot index where the states differ.
     *
     * @param state     the first state
     * @param nextState the second state != state1
     * @return the first robot index where the states differ.
     */
    public static int getRobotIndexOfFirstDifference(final RobotsState state, final RobotsState nextState) {
        for (int i = 0; i < nextState.getRobotCount(); i++) {
            if (state.getPositionX(i) != nextState.getPositionX(i) ||
                    state.getPositionY(i) != nextState.getPositionY(i)) {
                return i;
            }
        }

        throw new IllegalArgumentException("The two states do not differ");
    }


    public static Direction getRobotMovementDirection(final RobotsState state, final RobotsState nextState) {
        for (int i = 0; i < nextState.getRobotCount(); i++) {
            final int xDiff = nextState.getPositionX(i) - state.getPositionX(i);
            final int yDiff = nextState.getPositionY(i) - state.getPositionY(i);
            if (xDiff != 0 || yDiff != 0) {
                return Direction.valueOf(xDiff, yDiff);
            }
        }

        throw new IllegalArgumentException("The two states do not differ");
    }

}
