package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.SlidingRobotsSearchAlgorithm;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.seed.SeedUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class ProfileRunner {
    private static final Logger cLogger = LoggerFactory.getLogger(ProfileRunner.class);

    private ProfileRunner() {
    }

    /**
     * For getting stats on average speed and running profilers, also for generating maps in order of difficulty
     */
    public static void profileRun(final int runCount, final String mapsFileName,
                                  final Function<Board, SlidingRobotsSearchAlgorithm> algorithmFactory,
                                  final int dimX, final int dimY)
            throws IOException {
        final boolean isOneWay = false;

        cLogger.info("Running statistics gathering (dimx={} dimy={}). runs = {}", dimX, dimY, runCount);
        final DescriptiveStatistics timeStats = new DescriptiveStatistics(runCount);


        final List<String> mapStrings = new ArrayList<>(runCount);
        final List<String> mapSeedStrings = new ArrayList<>(runCount);
        final List<Integer> mapMoves = new ArrayList<>(runCount);
        int noSolutionCount = 0;

        final Path mapsFilePath = Path.of(mapsFileName);
        final boolean isSaveMapStrings = mapsFileName.isEmpty() || !Files.exists(mapsFilePath);
        if (isSaveMapStrings) {
            for (int i = 0; i < runCount; i++) {
                final String seedString = SeedUtils.generateSeedString(dimX, dimY, isOneWay);
                mapSeedStrings.add(seedString);
                mapStrings.add(MapStringGenerator.generateFromSeed(seedString));
            }
        } else {
            // map file has format "<seedString><space><mapString><space><moveCount>\n"
            for (String line : Files.readAllLines(mapsFilePath)) {
                final String[] lineSplit = line.split(" ");
                mapSeedStrings.add(lineSplit[0]);
                mapStrings.add(lineSplit[1]);
            }
        }

        final List<String> mapStringsToDump = new ArrayList<>(runCount);
        final int actualRunCount =
                runCount == 0
                        ? mapStrings.size()
                        : Math.min(runCount, mapStrings.size());

        for (int i = 0; i < actualRunCount; i++) {
            final Game game = Game.valueOfMap(mapStrings.get(i));
            try {
                final Timer timer = new Timer();
                final SlidingRobotsSearchAlgorithm searchAlgorithm = algorithmFactory.apply(game.getBoard());
                final Solution solution = searchAlgorithm.run(game.getInitialRobotsState(), game.getEndCriteria());
                timer.stop();
                final double time = timer.getDurationMillis();
                timeStats.addValue(time);
                if (i % 20 == 0) {
                    cLogger.info("Run {} took {} ms", i, time);
                }
                mapMoves.add(solution.getStatistics().getSolutionLength());
                mapStringsToDump.add(mapStrings.get(i));
            } catch (NoSolutionException e) {
                noSolutionCount++;
            }
        }

        if (isSaveMapStrings) {
            if (!Files.exists(mapsFilePath)) {
                Files.createDirectories(mapsFilePath.getParent());
            }

            // map file has format "<mapString><space><moveCount>\n"
            final List<String> output = new ArrayList<>(mapStrings.size());
            for (int i = 0; i < mapStringsToDump.size(); i++) {
                output.add("%s %s %d"
                        .formatted(
                                mapSeedStrings.get(i),
                                mapStringsToDump.get(i),
                                mapMoves.get(i))
                );
            }

            Files.write(mapsFilePath, output, Charset.defaultCharset());
            cLogger.info("Maps dumped to file {}", mapsFilePath);
        }
        cLogger.info("Total run count =   {}", mapStringsToDump.size());
        cLogger.info("  no solution # =   {}", noSolutionCount);
        cLogger.info("Time Statistics (ms)");
        cLogger.info("      time tot =   {}", timeStats.getSum());
        cLogger.info("      time avg =   {}", timeStats.getMean());
        cLogger.info("      time med =   {}", timeStats.getPercentile(50));
        cLogger.info("      time std =   {}", timeStats.getStandardDeviation());
        cLogger.info("      time var =   {}", timeStats.getVariance());
    }
}
