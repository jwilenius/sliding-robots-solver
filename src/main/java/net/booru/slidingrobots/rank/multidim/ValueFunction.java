package net.booru.slidingrobots.rank.multidim;

public interface ValueFunction<T> {
    double apply(T t);
}
