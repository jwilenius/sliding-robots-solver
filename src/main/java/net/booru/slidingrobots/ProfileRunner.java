package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.SlidingRobotsSearchAlgorithm;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.state.Board;
import net.booru.slidingrobots.state.Game;
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
    public static void profileRun(final int runCount, final String mapsFile,
                                  final Function<Board, SlidingRobotsSearchAlgorithm> algorithmFactory)
            throws IOException {
        cLogger.info("Running statistics gathering. runs = {}", runCount);
        final DescriptiveStatistics timeStats = new DescriptiveStatistics(runCount);

        final boolean isSaveMapStrings = runCount != 0 && mapsFile.isEmpty();

        final List<String> mapStrings = new ArrayList<>(runCount);
        final List<Integer> mapMoves = new ArrayList<>(runCount);
        int noSolutionCount = 0;

        if (mapsFile.isEmpty()) {
            for (int i = 0; i < runCount; i++) {
                mapStrings.add(MapStringGenerator.generate(8, 8));
            }
        } else {
            // map file has format "<mapString><space><moveCount>\n"
            Files.readAllLines(Path.of(mapsFile)).forEach(map -> mapStrings.add(map.split(" ")[0]));
        }

        final List<String> mapStringsToDump = new ArrayList<>(runCount);
        final int actualRunCount =
                runCount == 0
                        ? mapStrings.size()
                        : Math.min(runCount, mapStrings.size());

        for (int i = 0; i < actualRunCount; i++) {
            final Game game = Game.valueOf(mapStrings.get(i));
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
            final String path = "junk/";
            final String dumpFile = path + "maps_moves.txt";
            final Path outputDir = Path.of(path);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // map file has format "<mapString><space><moveCount>\n"
            final List<String> output = new ArrayList<>(mapStrings.size());
            for (int i = 0; i < mapStringsToDump.size(); i++) {
                output.add(mapStringsToDump.get(i) + " " + mapMoves.get(i));
            }

            Files.write(Path.of(dumpFile), output, Charset.defaultCharset());
            cLogger.info("Maps dumped to file {}", dumpFile);
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
