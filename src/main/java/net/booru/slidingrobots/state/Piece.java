package net.booru.slidingrobots.state;

public enum Piece {
    helper_robot,
    blocker,
    main_robot,
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
