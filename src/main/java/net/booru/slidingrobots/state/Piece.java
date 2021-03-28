package net.booru.slidingrobots.state;

public enum Piece {
    main_robot,
    helper_robot,
    blocker,
    start,
    goal,
    empty;

    public boolean isBlocking() {
        return this == helper_robot || this == blocker || this == main_robot;
    }

    public boolean isImmovable() {
        return this == Piece.blocker || this == Piece.goal || this == Piece.start;
    }
}
