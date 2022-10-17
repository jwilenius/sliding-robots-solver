package net.booru.slidingrobots.rank.multidim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Rank a list of elements according to a supplied ordered list of {@link Rank}.
 *
 * <ol>
 * <li>Apply the first {@link Rank#valueFunction()} to each element in the List, and sort the elements according to these values.
 * <li>Split the sort result into so-called epsilon groups in the following way:
 * <ol>
 * <li>Take the first element in the List and create a (sub) tree of the elements in the List that are equal according to
 *     the {@link Rank#epsilonFunction()} (i.e., epsilon equal).
 * <li>Redo the first step on the remaining elements until the List is empty (each step creates a new (sub) tree).
 * </ol>
 * <li>Recursively apply the algorithm on each epsilon group larger than one element, with the remaining ranks.
 * </ol>
 * <br><br>
 * Each {@link Rank} will be applied to each element at most once.
 */
public final class MultiDimRanking<T> {
    private static final Logger cLog = Logger.getLogger(MultiDimRanking.class.getSimpleName());
    private static final boolean DEBUG = false;

    private final List<Rank<T>> iRankers;

    public MultiDimRanking(final List<Rank<T>> rankers) {
        if (rankers.isEmpty()) {
            throw new IllegalArgumentException("pre: Must supply at least one Rank!");
        }

        iRankers = rankers;
    }

    /**
     * @return the flattened result of applying this multidimensional sorter algorithm to {@code List}
     */
    public List<T> applyRank(final List<T> elements) {
        final List<SortTree<RankResult<T>>> resultAsATree = applyToCore(elements);
        return flatten(resultAsATree).stream().map(RankResult::getElement).toList();
    }

    /**
     * A final group is either the single value of a Leaf, or the List of values of all
     * leaf children of a Node.
     *
     * @return {@code List} sorted according to this multidimensional ranking algorithm,
     * and flattened down to the final groups
     */
    public List<List<T>> getFinalGroups(final List<T> elements) {
        final List<SortTree<RankResult<T>>> groups = applyToCore(elements);

        return groups.stream().flatMap((SortTree<RankResult<T>> epsilonGroup) ->
                epsilonGroup.getInseparableGroups().stream().map((List<RankResult<T>> results) ->
                        results.stream().map(RankResult::getElement).toList())).toList();
    }

    /**
     * @return the result (Rank values) for the provided element.
     */
    public RankResult<T> getResultForElement(final T element) {
        final Function<Rank<T>, Double> valueFunction = (Rank<T> rank) -> rank.valueFunction().apply(element);

        final List<Double> values = iRankers.stream().map(valueFunction).toList();

        final RankResult<T> elementResult = new RankResult<>(element);
        for (int i = 0; i < values.size(); i++) {
            elementResult.addValue(values.get(i), iRankers.get(i));
        }

        return elementResult;
    }

    /**
     * @param elements
     * @return a List of sort trees where each element is an epsilon group of {@code List} of the first rank,
     * recursively sorted by the following ranks.
     */
    private List<SortTree<RankResult<T>>> applyToCore(final List<T> elements) {
        if (elements.isEmpty()) {
            return List.of();
        }

        // This is needed (and terrible) to get in-place sorting,
        // also somewhat unnecessary now that we are building a tree.
        @SuppressWarnings("unchecked") final RankResult<T>[] mutableResult = new RankResult[elements.size()];
        for (int i = 0; i < elements.size(); ++i) {
            mutableResult[i] = new RankResult<>(elements.get(i));
        }

        final List<SortTree<RankResult<T>>> sortResults = sort(mutableResult, 0, mutableResult.length, iRankers, 0);

        return sortResults;
    }

    private List<RankResult<T>> flatten(final List<SortTree<RankResult<T>>> results) {
        return results.stream().flatMap((SortTree<RankResult<T>> sortTree) -> {
            if (sortTree.isLeaf()) {
                return Stream.of(((Leaf<RankResult<T>>) sortTree).getValue());
            } else {
                return flatten(sortTree.getChildren()).stream();
            }
        }).toList();
    }

    private List<SortTree<RankResult<T>>> sort(final RankResult<T>[] mutableResults,
                                               final int lowerBoundInclusive,
                                               final int upperBoundExclusive,
                                               final List<Rank<T>> rankers,
                                               final int level) {
        assert lowerBoundInclusive <= upperBoundExclusive : "pre: empty sort interval!";

        final Rank<T> rank = rankers.get(level);
        final ValueFunction<T> valueFunction = rank.valueFunction();

        // Do all value calculations in parallel before doing (sequential) sorting
        for (int index = lowerBoundInclusive; index < upperBoundExclusive; index++) {
            final RankResult<T> rankResult = mutableResults[index];
            final double value = valueFunction.apply(rankResult.getElement());
            assert !Double.isInfinite(value) : "Infinite rank values is a problem!";

            rankResult.addValue(value, rank); // add rank for debugging purposes (traceability)
            if (DEBUG) {
                cLog.info(rankResult.toString());
            }
        }

        final Comparator<RankResult<T>> valueComparator = Comparator.comparingDouble(result -> result.getValue(level));
        Arrays.sort(mutableResults, lowerBoundInclusive, upperBoundExclusive, valueComparator);

        final int nextLevel = level + 1;
        final List<SubGroup> subGroups = new ArrayList<>(upperBoundExclusive - lowerBoundInclusive);

        int fromIndexInclusive = lowerBoundInclusive;
        while (fromIndexInclusive < upperBoundExclusive) {
            final SubGroup subGroup =
                    calculateGroupStartingAtIndexEndAtLevel(
                            level,
                            rank.epsilonFunction(),
                            mutableResults,
                            fromIndexInclusive,
                            upperBoundExclusive);

            subGroups.add(subGroup);
            fromIndexInclusive = subGroup.endExclusive(); // Skip all elements included in the epsilon group
        }

        return subGroups.stream()
                .map(subGroup -> getRankResultSortNode(mutableResults, upperBoundExclusive, rankers, nextLevel, subGroup))
                .toList();
    }

    private SortTree<RankResult<T>> getRankResultSortNode(final RankResult<T>[] mutableResults,
                                                          final int upperBoundExclusive,
                                                          final List<Rank<T>> rankers,
                                                          final int nextLevel,
                                                          final SubGroup subGroup) {
        final RankResult<T> intervalWinner = mutableResults[subGroup.startInclusive()];

        final boolean isSingletonGroup = subGroup.endExclusive() - subGroup.startInclusive() == 1;
        final boolean areRankersLeft = nextLevel < rankers.size();

        final boolean isMoreWorkToPerform = !isSingletonGroup && areRankersLeft;
        if (isMoreWorkToPerform) {
            final var epsilonGroupsForInterval =
                    sort(mutableResults, subGroup.startInclusive(), subGroup.endExclusive(), rankers, nextLevel);

            return new Node<>(subGroup.epsilon(), intervalWinner, epsilonGroupsForInterval);
        } else {
            assert subGroup.endExclusive() <= upperBoundExclusive : "inv: too large interval created!";
            final int actualUpperBound = subGroup.endExclusive();

            final List<SortTree<RankResult<T>>> leafs = new ArrayList<>();
            for (int i = subGroup.startInclusive(); i < actualUpperBound; i++) {
                leafs.add(new Leaf<>(Double.NaN, mutableResults[i]));
            }

            return new Node<>(subGroup.epsilon(), intervalWinner, leafs);
        }
    }

    private SubGroup calculateGroupStartingAtIndexEndAtLevel(final int level,
                                                             final EpsilonFunction epsilonFunction,
                                                             final RankResult<T>[] rankResults,
                                                             final int fromIndexInclusive,
                                                             final int upperBoundExclusive) {
        final double resultValues = rankResults[fromIndexInclusive].getValue(level);
        final double epsilon = Math.abs(epsilonFunction.apply(resultValues));

        int j = fromIndexInclusive + 1;
        while (j < upperBoundExclusive && epsilonEquals(rankResults[j].getValue(level), resultValues, epsilon)) {
            ++j;
        }

        return new SubGroup(epsilon, fromIndexInclusive, j);
    }

    private static boolean epsilonEquals(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Container class for maintaining start/end indices of epsilon groups.
     */
    private record SubGroup(
            double epsilon,
            int startInclusive,
            int endExclusive) {
    }
}

