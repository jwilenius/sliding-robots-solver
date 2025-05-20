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
 * <li> Apply the first {@link Rank#valueFunction()} to each element in the List, and sort the elements according to these values.
 * <li> Split the sort result into so-called epsilon groups in the following way:
 * <ol>
 * <li> Take the first element in the List and create a (sub) tree of the elements in the List that are equal according to
 *      the {@link Rank#epsilonFunction()} (i.e., epsilon equal).
 * <li> Redo the first step on the remaining elements until the List is empty (each step creates a new (sub) tree).
 * </ol>
 * <li> Recursively apply the algorithm on each epsilon group larger than one element, with the remaining ranks.
 * </ol>
 * <br><br>
 * Each {@link Rank} will be applied to each element at most once.
 */
public final class MultiDimRanking<T> {
    private static final Logger cLog = Logger.getLogger(MultiDimRanking.class.getSimpleName());
    private static final boolean DEBUG = false;

    private final List<Rank<T>> iRankers;

    /**
     * Create a Multi dimensional ranker that does ranking in the natural order as specified
     * by the provided list of rankers. Epsilons in the rank are calculated on the first element
     * in each new group.
     *
     * @param rankers the rankers to use, see {@link Rank}
     */
    public MultiDimRanking(final List<Rank<T>> rankers) {
        if (rankers.isEmpty()) {
            throw new IllegalArgumentException("pre: Must supply at least one Rank!");
        }

        iRankers = rankers;
    }

    /**
     * This is most likely the method you want to call if you only want an ordered list (natural order, best-first) of
     * the elements ranked according the provided rankers.
     *
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
     * @return the result of all rankers (Rank values) for the provided element, returned as a {@link RankResult<T>}
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
     * @param elements the elements to sort
     * @return a List of sort trees where each element is an epsilon group of {@code List} of the first rank,
     * recursively sorted by the following ranks.
     */
    private List<SortTree<RankResult<T>>> applyToCore(final List<T> elements) {
        if (elements.isEmpty()) {
            return List.of();
        }

        // This is needed (and terrible) to get in-place sorting.
        //  Prepare a mutable array of mutable results, will be sorted
        //  and filled with values as we go deeper.
        @SuppressWarnings("unchecked") final RankResult<T>[] mutableResult = new RankResult[elements.size()];
        for (int i = 0; i < elements.size(); ++i) {
            mutableResult[i] = new RankResult<>(elements.get(i));
        }

        final List<SortTree<RankResult<T>>> sortResults = sort(mutableResult, 0, mutableResult.length, iRankers, 0);

        return sortResults;
    }

    private List<SortTree<RankResult<T>>> sort(final RankResult<T>[] mutableResults,
                                               final int lowerBoundInclusive,
                                               final int upperBoundExclusive,
                                               final List<Rank<T>> rankers,
                                               final int rankLevel) {
        assert lowerBoundInclusive <= upperBoundExclusive : "pre: empty sort interval!";

        final Rank<T> rank = rankers.get(rankLevel);
        calculateAllRankValues(mutableResults, rank, lowerBoundInclusive, upperBoundExclusive);

        final Comparator<RankResult<T>> valueComparator = Comparator.comparingDouble(result -> result.getValue(rankLevel));
        Arrays.sort(mutableResults, lowerBoundInclusive, upperBoundExclusive, valueComparator);

        final int nextLevel = rankLevel + 1;
        final List<SubGroup> subGroups = new ArrayList<>(upperBoundExclusive - lowerBoundInclusive);

        int fromIndexInclusive = lowerBoundInclusive;
        while (fromIndexInclusive < upperBoundExclusive) {
            final SubGroup subGroup =
                    calculateEpsilonGroupAtLevel(
                            rankLevel,
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

    /**
     * Update the {@link RankResult<T>} in the mutableResults given the current {@code rank} and sub-group range.
     *
     * @param mutableResults      the results to update
     * @param rank                current rank
     * @param lowerBoundInclusive start index inclusive in the mutableResults array
     * @param upperBoundExclusive end index exclusive in the mutableResults array
     */
    private void calculateAllRankValues(final RankResult<T>[] mutableResults,
                                        final Rank<T> rank,
                                        final int lowerBoundInclusive,
                                        final int upperBoundExclusive) {
        final ValueFunction<T> valueFunction = rank.valueFunction();

        for (int index = lowerBoundInclusive; index < upperBoundExclusive; index++) {
            final RankResult<T> rankResult = mutableResults[index];
            final double value = valueFunction.apply(rankResult.getElement());
            assert !Double.isInfinite(value) : "Infinite rank values is a problem!";
            // We can use add since "level"s are done in order,
            //  each added value is at the correct level.
            //  rank is added for debugging purposes (traceability)
            rankResult.addValue(value, rank);
            if (DEBUG) {
                cLog.info(rankResult.toString());
            }
        }
    }

    private SortTree<RankResult<T>> getRankResultSortNode(final RankResult<T>[] mutableResults,
                                                          final int upperBoundExclusive,
                                                          final List<Rank<T>> rankers,
                                                          final int nextLevel,
                                                          final SubGroup subGroup) {
        final RankResult<T> firstInGroup = mutableResults[subGroup.startInclusive()];

        final boolean isSingletonGroup = subGroup.endExclusive() - subGroup.startInclusive() == 1;
        final boolean areRankersLeft = nextLevel < rankers.size();
        final boolean isMoreWorkToPerform = !isSingletonGroup && areRankersLeft;

        if (isMoreWorkToPerform) {
            // recursive call to sort for nextLevel rank, of the current subgroup
            final var epsilonGroupsForInterval =
                    sort(mutableResults, subGroup.startInclusive(), subGroup.endExclusive(), rankers, nextLevel);
            return new Node<>(subGroup.epsilon(), firstInGroup, epsilonGroupsForInterval);
        } else {
            assert subGroup.endExclusive() <= upperBoundExclusive : "inv: too large interval created!";
            final int actualUpperBound = subGroup.endExclusive();

            final List<SortTree<RankResult<T>>> leafs = new ArrayList<>();
            for (int i = subGroup.startInclusive(); i < actualUpperBound; i++) {
                leafs.add(new Leaf<>(Double.NaN, mutableResults[i]));
            }

            return new Node<>(subGroup.epsilon(), firstInGroup, leafs);
        }
    }

    private SubGroup calculateEpsilonGroupAtLevel(final int rankLevel,
                                                  final EpsilonFunction epsilonFunction,
                                                  final RankResult<T>[] rankResults,
                                                  final int fromIndexInclusive,
                                                  final int upperBoundExclusive) {
        final double firstValueInGroup = rankResults[fromIndexInclusive].getValue(rankLevel);
        final double epsilon = Math.abs(epsilonFunction.apply(firstValueInGroup));

        int j = fromIndexInclusive + 1;
        while (j < upperBoundExclusive && epsilonEquals(rankResults[j].getValue(rankLevel), firstValueInGroup, epsilon)) {
            ++j;
        }

        return new SubGroup(epsilon, fromIndexInclusive, j);
    }

    private static boolean epsilonEquals(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    /**
     * Recursively flatten the tree of epsilon groups
     */
    private static <T> List<RankResult<T>> flatten(final List<SortTree<RankResult<T>>> results) {
        return results.stream().flatMap((SortTree<RankResult<T>> sortTree) -> {
            if (sortTree.isLeaf()) {
                return Stream.of(((Leaf<RankResult<T>>) sortTree).getValue());
            } else {
                return flatten(sortTree.getChildren()).stream();
            }
        }).toList();
    }

    /**
     * Container class for maintaining start/end indices of epsilon groups.
     */
    private record SubGroup(
            double epsilon,
            int startInclusive,
            int endExclusive
    ) {
    }
}

