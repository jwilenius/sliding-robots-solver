package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.Point;
import net.booru.slidingrobots.state.seed.Seed;
import net.booru.slidingrobots.state.seed.SeedUtils;
import net.booru.slidingrobots.common.Timer;
import net.booru.slidingrobots.rank.GameRanker;
import net.booru.slidingrobots.rank.GameWithSolution;
import net.booru.slidingrobots.state.Game;
import net.booru.slidingrobots.state.Piece;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Generates simple random map-strings for use with different solvers.
 */
public class MapStringGenerator {
    private static final Logger cLogger = LoggerFactory.getLogger(MapStringGenerator.class);

    public static void generateToFile(final String mapsFile,
                                      final int mapDimX,
                                      final int mapDimY,
                                      final int mapsPerMove,
                                      final int mapsMinMoves,
                                      final int mapsMaxMoves,
                                      final boolean isOneWay)
            throws IOException {

        cLogger.info("Generating maps from {} to {} moves (inclusive), {} maps each. Total {} maps",
                mapsMinMoves, mapsMaxMoves, mapsPerMove, (mapsMaxMoves - mapsMinMoves + 1) * mapsPerMove);

        if (mapsFile.isEmpty()) {
            cLogger.info("Generating to stdout, no file specified.");
        }

        final Set<Integer> remainingMoveSolutions = new HashSet<>();
        for (int j = mapsMinMoves; j <= mapsMaxMoves; j++) {
            remainingMoveSolutions.add(j);
        }

        final Map<Integer, List<GameWithSolution>> movesMap = new HashMap<>();

        final var mutableStats = new MutableStats();
        do {
            mutableStats.tick();

            try {
                final String seedString = SeedUtils.generateSeedString(mapDimX, mapDimY, isOneWay);
                final Game game = Game.valueOfSeed(seedString);
                final var searchAlgorithm = new BreadthFirstSearchIterative(game.getBoard(), -1);
                final Solution solution = searchAlgorithm.run(game.getInitialRobotsState(), game.getEndCriteria());

                final int solutionMoveCount = solution.getStatistics().getSolutionLength();
                if (remainingMoveSolutions.contains(solutionMoveCount)) {
                    final List<GameWithSolution> gamesWithMoveCount =
                            movesMap.computeIfAbsent(solutionMoveCount, k -> new ArrayList<>(mapsPerMove));
                    gamesWithMoveCount.add(new GameWithSolution(game, solution, null));

                    mutableStats.increaseSavedMapsCount();

                    if (gamesWithMoveCount.size() == mapsPerMove) {
                        // if contains duplicate don't remove, rest to unique games
                        final HashSet<GameWithSolution> uniqueGames = new HashSet<>(gamesWithMoveCount);
                        if (uniqueGames.size() == mapsPerMove) {
                            remainingMoveSolutions.remove(solutionMoveCount);
                            cLogger.info("Maps of move count = {}, finished generating. Remaining: {}",
                                    solutionMoveCount, remainingMoveSolutions);
                        } else {
                            mutableStats.increaseNonUniqueCount();
                            cLogger.info("Unlikely event, NON-unique game(s) found ({}/{})", uniqueGames.size(), mapsPerMove);
                            gamesWithMoveCount.clear();
                            gamesWithMoveCount.addAll(uniqueGames);
                        }
                    }
                }
            } catch (NoSolutionException e) {
                mutableStats.increaseNoSolutionCount();
            }
        }
        while (!remainingMoveSolutions.isEmpty());


        final List<String> mapsToDump = getRankedMapStrings(movesMap);
        if (!mapsFile.isEmpty()) {
            Files.write(Path.of(mapsFile), mapsToDump, Charset.defaultCharset());
        } else {
            mapsToDump.forEach(System.out::println); //NOSONAR
        }

        cLogger.info("-----------------------------------------------------------");
        cLogger.info("Maps dumped to file {}", mapsFile);
        cLogger.info("Total maps saved =            {}", mapsToDump.size());
        cLogger.info("Total maps generated =        {}", mutableStats.generatedMapsCount);
        cLogger.info("Total maps with no solution = {}", mutableStats.noSolutionCount);
        cLogger.info("Total maps not unique =       {}", mutableStats.noSolutionCount);
    }

    private static List<String> getRankedMapStrings(final Map<Integer, List<GameWithSolution>> movesMap) {
        final List<GameWithSolution> mapsToRank = movesMap.values().stream()
                .flatMap(games -> games.stream().map(game ->
                        new GameWithSolution(game.game(), game.solution(), null)))
                .toList();

        final List<GameWithSolution> rankedMaps = new GameRanker().apply(mapsToRank);
        var mapsToDump = rankedMaps.stream()
                .map(g -> g.game().getSeedString() + " " + g.game().getMapString() + " " + g.solution().getStatistics().getSolutionLength())
                .toList();
        return mapsToDump;
    }

    private record MapGameSolution(String map, Game game, Solution solution) {
    }

    public static String generateFromSeed(final String seedString) {
        // The seed string concept is from the js/erlang implementation of this game, we need the same to be compatible
        final Seed seed = SeedUtils.parseSeedString(seedString);
        final int dimX = seed.dimX();
        final int dimY = seed.dimY();

        // Import do not change these, they are defined elsewhere, we need them to be compatible.
        final int blockerCount = Math.max(dimX, dimY);
        final int helperRobotCount = 2;
        final int mainRobotCount = 1;
        final int goalCount = 1;
        final int pieceCount = blockerCount + helperRobotCount + mainRobotCount + goalCount;

        // The order of pieces needs to match the one in preexisting implementations.
        // [main, helper, helper, goal] ++ lists:duplicate(max(Width, Height), blocker).
        final Deque<Piece> pieces = new LinkedList<>();
        IntStream.range(0, blockerCount).forEach(i -> pieces.push(Piece.blocker));
        pieces.push(Piece.goal);
        IntStream.range(0, helperRobotCount).forEach(i -> pieces.push(Piece.helper_robot));
        pieces.push(Piece.main_robot);

        // Generate non-conflicting positions until all pieces are placed
        int currentSeed = seed.hash();
        final HashMap<Point, Piece> locationPieceMap = new HashMap<>(pieceCount * 2);
        for (int i = 0; i < pieceCount; i++) {
            Point point;
            do {
                final var resultX = SeedUtils.xorshift32(currentSeed, dimX);
                final var resultY = SeedUtils.xorshift32(resultX.seed(), dimY);
                currentSeed = resultY.seed();
                point = new Point(resultX.randomNumber(), resultY.randomNumber());
            } while (locationPieceMap.containsKey(point));
            locationPieceMap.put(point, pieces.pop());
        }

        return piecesToMapString(dimX, dimY, locationPieceMap, seed.isOneWay());
    }

    private static String piecesToMapString(final int dimX, final int dimY,
                                            final HashMap<Point, Piece> locationPieceMap,
                                            final boolean isOneWay) {
        final StringBuilder mapString = new StringBuilder(200);
        mapString.append("map:").append(dimX).append(":").append(dimY);
        if (isOneWay) {
            mapString.append("oneway");
        }

        for (var entry : locationPieceMap.entrySet()) {
            final Point position = entry.getKey();
            final Piece piece = locationPieceMap.get(position);
            if (piece != null) {
                mapString.append(":").append(piece.name())
                        .append(":").append(position.getX())
                        .append(":").append(position.getY());
            }
        }

        return mapString.toString();
    }

    private static class MutableStats {
        int iterations = 0;
        int generatedMapsCount = 0;
        int savedMapsCount = 0;
        int noSolutionCount = 0;
        int nonUniqueCount = 0;
        Timer timer = new Timer();

        public void tick() {
            generatedMapsCount++;
            iterations++;
            timer.stop();
            if (iterations % 100 == 0) {
                cLogger.info("Last 100 runs took avg = {} ms, Maps generated = {}, no-solutions = {}, saved = {}",
                        timer.getDurationMillis() / 100.0, generatedMapsCount, noSolutionCount, savedMapsCount);
            }
            timer = new Timer();
        }

        public void increaseSavedMapsCount() {
            savedMapsCount++;
        }

        public void increaseNoSolutionCount() {
            noSolutionCount++;
        }

        public void increaseNonUniqueCount() {
            nonUniqueCount++;
        }
    }

}
