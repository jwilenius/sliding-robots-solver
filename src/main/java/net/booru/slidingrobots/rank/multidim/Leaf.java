package net.booru.slidingrobots.rank.multidim;

import java.util.List;

/**
 * A leaf (a node without any children) in a sort tree.
 */
class Leaf<T> implements SortTree<T> {

    /**
     * The epsilon for this tree. Defines the scope of the tree given measure values.
     */
    private final double iEpsilon;
    private final T iValue;

    public Leaf(final double epsilon, final T value) {
        iEpsilon = epsilon;
        iValue = value;
    }

    @Override
    public List<SortTree<T>> getChildren() {
        return List.of();
    }

    /**
     * The flattened groups of a leaf is the group containing the single value of the leaf.
     *
     * @return all groups below this node flattened
     */
    @Override
    public List<List<T>> getInseparableGroups() {
        return List.of(List.of(iValue));
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    public T getValue() {
        return iValue;
    }

    @Override
    public String toString() {
        if (Double.isNaN(iEpsilon)) {
            return iValue.toString();
        } else {
            return iValue.toString() + " (ε=" + iEpsilon + ")";
        }
    }
}
