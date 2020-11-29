package net.booru.slidingrobots;

import java.awt.*;
import java.util.HashMap;

public class Main {
    private enum Piece {
        helper_robot,
        blocker,
        main_robot,
        goal
    }

    public static class Board {
        private final int iWidth;
        private final int iHeight;
        private final HashMap<Point, Piece> iPieces;

        public Board(final HashMap<Point, Piece> pieces, final int width, final int height) {
            iWidth = width;
            iHeight = height;
            iPieces = pieces;
        }

        /**
         * Parse a string description of a board setup.
         * <p>
         * <pre>
         * Example: map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:
         *          blocker:2:3:blocker:4:0:helper_robot:5:6:blocker:6:1:"blocker:7:7:goal:3:0
         * </pre>
         *
         * @param value a string representation of a {@link Board}.
         * @return the Board corresponding to {@code value}.
         */
        public static Board valueOf(final String value) {
            final HashMap<Point, Piece> pieces = new HashMap<>(20);
            final String[] tokens = value.split(":");
            final int start = tokens[0].equals("map") ? 1 : 0;

            for (int i = start; i < tokens.length; i += 3) {
                pieces.put(new Point(Integer.parseInt(tokens[i + 1]), Integer.parseInt(tokens[i + 2])),
                        Piece.valueOf(tokens[i]));
            }
            return new Board(pieces, 8, 8);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (int y = 0; y < iHeight; y++) {
                for (int x = 0; x < iWidth; x++) {
                    final Point point = new Point(x, y);
                    final Piece piece = iPieces.get(point);
                    if (piece == null) {
                        sb.append('.');
                        continue;
                    }

                    switch (piece) {
                        case helper_robot:
                            sb.append('h');
                            break;
                        case blocker:
                            sb.append('#');
                            break;
                        case main_robot:
                            sb.append('R');
                            break;
                        case goal:
                            sb.append('@');
                            break;
                    }
                } // end row
                sb.append('\n');
            }

            return sb.toString();
        }
    }

    public static void main(String[] args) {
        final String boardStr =
                "map:helper_robot:1:4:blocker:1:6:blocker:2:0:main_robot:2:1:blocker:2:3:blocker:4:0:helper_robot:5:6" +
                ":blocker:6:1:blocker:7:7:goal:3:0";

        final Board board = Board.valueOf(boardStr);
        System.out.println(board.toString());
    }
}
