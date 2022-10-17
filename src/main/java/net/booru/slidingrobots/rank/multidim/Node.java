package net.booru.slidingrobots.rank.multidim;

import java.util.List;

/**
 * An internal node in a sort tree.
 */
class Node<T> implements SortTree<T> {
    /**
     * The epsilon for this tree. Defines the scope of the tree given measure values.
     */
    private final double iEpsilon;

    /**
     * The first element of this group given the corresponding measure.
     */
    private final T iWinner;

    /**
     * The children of this group node, where all children are considered equal to
     * {@code iWinner} according to the corresponding measure.
     */
    private final List<SortTree<T>> iChildren;

    public Node(final double epsilon, final T winner, final List<SortTree<T>> children) {
        iEpsilon = epsilon;
        iWinner = winner;
        iChildren = children;
    }

    @Override
    public String toString() {
        return "{W=%s, ε=%s, [%s]}".formatted(iWinner, iEpsilon, iChildren);
    }

    @Override
    public List<SortTree<T>> getChildren() {
        return iChildren;
    }

    /**
     * @return all groups below this node flattened
     */
    @Override
    public List<List<T>> getInseparableGroups() {
        if (getChildren().stream().allMatch(SortTree::isLeaf)) {
            return List.of(getChildren().stream().map(child -> ((Leaf<T>) child).getValue()).toList());
        } else {
            return getChildren().stream().flatMap(sortTree -> sortTree.getInseparableGroups().stream()).toList();
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
