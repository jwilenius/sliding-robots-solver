package net.booru.slidingrobots;

import net.booru.slidingrobots.algorithm.BreadthFirstSearchIterative;
import net.booru.slidingrobots.algorithm.NoSolutionException;
import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.common.Point;
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
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Generates simple random map-strings for use with different solvers.
 */
public class MapStringGenerator {
    public static final Random RANDOM = new Random();
    private static final Logger cLogger = LoggerFactory.getLogger(MapStringGenerator.class);

    public static void generateToFile(final String mapsFile,
                                      final int mapDimX,
                                      final int mapDimY,
                                      final int mapsPerMove,
                                      final int mapsMinMoves,
                                      final int mapsMaxMoves)
            throws IOException {

        cLogger.info("Generating maps from {} to {} moves (inclusive), {} maps each. Total {} maps",
                mapsMinMoves, mapsMaxMoves, mapsPerMove, (mapsMaxMoves - mapsMinMoves + 1) * mapsPerMove);

        if (mapsFile.isEmpty()) {
            cLogger.info("Generating to stdout, no file specified.");
        }

        int iterations = 0;
        int generatedMapsCount = 0;
        int savedMapsCount = 0;
        int noSolutionCount = 0;

        final Set<Integer> remainingMoveSolutions = new HashSet<>();
        for (int j = mapsMinMoves; j <= mapsMaxMoves; j++) {
            remainingMoveSolutions.add(j);
        }

        final Map<Integer, List<GameWithSolution>> movesMap = new HashMap<>();

        Timer timer = new Timer();
        do {
            try {
                final String mapString = generate(mapDimX, mapDimY);
                final Game game = Game.valueOf(mapString);

                final var searchAlgorithm = new BreadthFirstSearchIterative(game.getBoard(), -1);
                final Solution solution = searchAlgorithm.run(game.getInitialRobotsState(), game.getEndCriteria());

                { // bookkeeping and logging //NOSONAR
                    generatedMapsCount++;
                    iterations++;
                    timer.stop();
                    final double time = timer.getDurationMillis();
                    if (iterations % 100 == 0) {
                        cLogger.info("Last 100 runs took avg = {} ms, Maps generated = {}, no-solutions = {}, saved = {}",
                                time / 100.0, generatedMapsCount, noSolutionCount, savedMapsCount);
                    }
                    timer = new Timer();
                }

                final int solutionMoveCount = solution.getStatistics().getSolutionLength();
                if (remainingMoveSolutions.contains(solutionMoveCount)) {
                    final List<GameWithSolution> maps =
                            movesMap.computeIfAbsent(solutionMoveCount, k -> new ArrayList<>(mapsPerMove));
                    maps.add(new GameWithSolution(game, solution, null));
                    savedMapsCount++;

                    if (maps.size() == mapsPerMove) {
                        remainingMoveSolutions.remove(solutionMoveCount);
                        cLogger.info("Maps of move count = {}, finished generating. Remaining: {}",
                                solutionMoveCount, remainingMoveSolutions);
                    }
                }

            } catch (NoSolutionException e) {
                noSolutionCount++;
            }
        }
        while (!remainingMoveSolutions.isEmpty());

        final List<GameWithSolution> mapsToRank = movesMap.values().stream()
                .flatMap(games -> games.stream()
                        .map(game -> new GameWithSolution(game.game(), game.solution(), null)))
                .toList();

        final List<GameWithSolution> rankedMaps = new GameRanker().apply(mapsToRank);
        var mapsToDump = rankedMaps.stream()
                .map(g -> g.game().getMapString() + " " + g.solution().getStatistics().getSolutionLength())
                .toList();

        if (!mapsFile.isEmpty()) {
            Files.write(Path.of(mapsFile), mapsToDump, Charset.defaultCharset());
        } else {
            mapsToDump.forEach(System.out::println); //NOSONAR
        }

        cLogger.info("Maps dumped to file {}", mapsFile);
        cLogger.info("Total maps saved =            {}", mapsToDump.size());
        cLogger.info("Total maps generated =        {}", generatedMapsCount);
        cLogger.info("Total maps with no solution = {}", noSolutionCount);
    }

    private record MapGameSolution(String map, Game game, Solution solution) {
    }

    public static String generate(final int sizeX, final int sizeY) {
        final int averageSize = (int) Math.round((sizeX + sizeY) / 2.0);
        int blockerCount = averageSize;
        int helperRobotCount = (int) Math.round(averageSize / 4.0);
        final int mainRobotCount = 1; // also means start.
        final int goalCount = 1;
        final int pieceCount = blockerCount + helperRobotCount + mainRobotCount + goalCount;

        final Deque<Piece> pieces = new LinkedList<>();
        IntStream.range(0, blockerCount).forEach(i -> pieces.push(Piece.blocker));
        IntStream.range(0, helperRobotCount).forEach(i -> pieces.push(Piece.helper_robot));
        pieces.push(Piece.main_robot);
        pieces.push(Piece.goal);

        final HashMap<Point, Piece> map = new HashMap<>(pieceCount * 2);
        for (int i = 0; i < pieceCount; i++) {
            Point point = null;
            do {
                point = new Point(RANDOM.nextInt(sizeX), RANDOM.nextInt(sizeY));
            } while (map.containsKey(point));
            map.put(point, pieces.pop());
        }

        final StringBuilder mapString = new StringBuilder(200);
        mapString.append("map:").append(sizeX).append(":").append(sizeY);
        for (var keyValue : map.entrySet()) {
            final Piece piece = keyValue.getValue();
            final Point position = keyValue.getKey();
            mapString.append(":").append(piece.name())
                    .append(":").append(position.getX())
                    .append(":").append(position.getY());
        }

        return mapString.toString();
    }
}
