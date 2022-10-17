package net.booru.slidingrobots.rank.multidim;

import java.util.List;

/**
 * Interface for nodes in a tree structure generated by {@link MultiDimRanking}.
 */
interface SortTree<T> {
    /**
     * @return the children of this group
     */
    List<SortTree<T>> getChildren();

    /**
     * @return all groups below this node flattened
     */
    List<List<T>> getInseparableGroups();

    /**
     * @return {@code true} iff this group is a leaf
     */
    boolean isLeaf();
}
