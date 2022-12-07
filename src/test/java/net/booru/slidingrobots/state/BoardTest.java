package net.booru.slidingrobots.state;

import net.booru.slidingrobots.common.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardTest {
    @Test
    void testMakeMove() {
        final Game game = Game.valueOf2DMap("""
                r . b .
                . . b .
                h b b .
                . . . g
                """);
        final Board board = game.getBoard();
        final int state = game.getInitialRobotsState();
        System.out.println(board.toBoardString(state));

        assertEquals(1, board.findPositionBeforeCollision(0, Direction.down, state));
        assertEquals(0, board.findPositionBeforeCollision(0, Direction.up, state));
        assertEquals(1, board.findPositionBeforeCollision(0, Direction.right, state));
        assertEquals(0, board.findPositionBeforeCollision(0, Direction.left, state));
    }

    @Test
    void testMakeMove2() {
        final Game game = Game.valueOf2DMap("""
                . . . . . . .
                . . . b . . .
                . . . . . . .
                . . . r . h .
                . . . . . . .
                . . . . . . .
                . . . b . . g
                """);
        final Board board = game.getBoard();
        final int state = game.getInitialRobotsState();
        System.out.println(board.toBoardString(state));

        assertEquals(5, board.findPositionBeforeCollision(0, Direction.down, state));
        assertEquals(2, board.findPositionBeforeCollision(0, Direction.up, state));
        assertEquals(4, board.findPositionBeforeCollision(0, Direction.right, state));
        assertEquals(0, board.findPositionBeforeCollision(0, Direction.left, state));
    }
}