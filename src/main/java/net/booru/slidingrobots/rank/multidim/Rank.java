package net.booru.slidingrobots.rank.multidim;

/**
 * Contains a double-valued function and an associated epsilon value.
 * If two elements have the rank values within epsilon they will be
 * considered equal according to this comparator, and end up below the same node in the sort tree
 * (the elements may still be non-equal according to a later comparator).
 */
public record Rank<T>(
        String name,
        ValueFunction<T> valueFunction,
        EpsilonFunction epsilonFunction) {
}
