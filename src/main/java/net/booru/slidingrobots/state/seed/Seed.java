package net.booru.slidingrobots.state.seed;

public record Seed(int dimX, int dimY, boolean isOneWay, String seed, int hash) {
    @Override
    public String toString() {
        final String oneWay = isOneWay ? "oneway:" : "";
        return "seed:%d:%d:%s%s".formatted(dimX, dimY, oneWay, seed);
    }

}
