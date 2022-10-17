package net.booru.slidingrobots.rank.multidim;

import java.util.ArrayList;
import java.util.List;

public class RankResult<T> {
    private final T iElement;
    private final List<Double> iValues = new ArrayList<>();
    private final List<Rank<T>> iValueFunctions = new ArrayList<>(); // for debugging

    public RankResult(final T element) {
        iElement = element;
    }

    public T getElement() {
        return iElement;
    }

    public double getValue(final int level) {
        return iValues.get(level);
    }

    public void addValue(final double value, final Rank<T> valueFunction) {
        iValues.add(value);
        iValueFunctions.add(valueFunction);
    }

    @Override
    public String toString() {
        return "V=%s (R=%s)".formatted(iValues, iValueFunctions.stream().map(Rank::name).toList());
    }
}
