package net.booru.slidingrobots.algorithm;

import net.booru.slidingrobots.common.Point;
import net.booru.slidingrobots.state.Piece;

import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * Generates simple random map-strings for use with different solvers.
 */
public class MapStringGenerator {
    public static String generate(int size) {
        //example: "map:blocker:0:0:blocker:0:4:blocker:2:4:blocker:2:5:blocker:3:1:helper_robot:3:4:helper_robot:4:1
        // :blocker:4:6:main_robot:5:5:blocker:5:7:blocker:7:0:goal:6:7";

        int blockerCount = size;
        int helperRobotCount = (int) Math.round(size / 4.0);
        final int mainRobotCount = 1; // also means start.
        final int goalCount = 1;
        final int pieceCount = blockerCount + helperRobotCount + mainRobotCount + goalCount;

        final Stack<Piece> pieces = new Stack<>();
        IntStream.range(0, blockerCount).forEach(i -> pieces.push(Piece.blocker));
        IntStream.range(0, helperRobotCount).forEach(i -> pieces.push(Piece.helper_robot));
        pieces.push(Piece.main_robot);
        pieces.push(Piece.goal);

        final HashMap<Point, Piece> map = new HashMap<>(pieceCount * 2);
        final Random random = new Random();
        for (int i = 0; i < pieceCount; i++) {
            Point point = null;
            do {
                point = new Point(random.nextInt(size), random.nextInt(size));
            } while (map.containsKey(point));
            map.put(point, pieces.pop());
        }

        final StringBuilder mapString = new StringBuilder(200);
        mapString.append("map:").append(size).append(":").append(size);
        for (var keyValue : map.entrySet()) {
            final Piece piece = keyValue.getValue();
            final Point position = keyValue.getKey();
            mapString.append(":").append(piece.name())
                     .append(":").append(position.getX())
                     .append(":").append(position.getY());
        }

        return mapString.toString();
    }
}
