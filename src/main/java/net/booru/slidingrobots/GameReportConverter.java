package net.booru.slidingrobots;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a converter that can convert game reports to a plain data format to use for experimenting with ANN.
 * Currently, this is just for fun. There is an algorithmic difficulty that is pointless for real life.
 * In reality, we most want to extract the difficulty function from the data and also do some unsupervised learning on level clustering.
 */
public class GameReportConverter {
    private static final Logger cLogger = LoggerFactory.getLogger(GameReportConverter.class);

    private final ObjectMapper iMapper;

    public GameReportConverter() {
        iMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * @param inputFile
     * @param outputDataFile
     * @throws IOException
     */
    public void applyTo(final String inputFile, final String outputDataFile) throws IOException {
        if (!outputDataFile.endsWith(".json")) {
            throw new IllegalArgumentException("output file should be json");
        }

        final Path inputPath = Path.of(inputFile);
        final Path outputPathData = Path.of(outputDataFile);

        if (inputFile.isEmpty() || !Files.exists(inputPath)) {
            throw new IllegalArgumentException("Need to provide an input maps file.");
        }

        final List<Puzzle> puzzles = Files.readAllLines(inputPath).stream()
                .flatMap(line -> Stream.ofNullable(parseJson(line, Puzzle.class)))
                .toList();

        convertToDataFormat(outputPathData, puzzles);
    }

    private void convertToDataFormat(final Path outputPath, final List<Puzzle> allPuzzles) throws IOException {
        // we need the mapping from seed to puzzle_id
        final Map<String, String> hashes = readSeedStringHashes("junk/hashes.json");
        final Map<String, List<GameReport>> gameReportMap = readGameReports("junk/game_reports.json");

        final List<Puzzle> availablePuzzles = allPuzzles.stream().flatMap(
                puzzle -> {
                    final String puzzleId = hashes.get(puzzle.seedString);
                    final List<GameReport> gameReports = gameReportMap.get(puzzleId);

                    if (puzzleId == null || gameReports == null) {
                        return Stream.empty(); // this puzzle is not in the data
                    }
                    return Stream.of(puzzle);
                }).toList();

        final Map<String, Float> puzzleDifficultyMap = computeDifficulty(availablePuzzles, hashes, gameReportMap);

        final List<Data> data = availablePuzzles.stream().flatMap(
                puzzle -> {
                    final String puzzleId = hashes.get(puzzle.seedString);
                    final List<GameReport> gameReports = gameReportMap.get(puzzleId);
                    final float difficulty = puzzleDifficultyMap.get(puzzle.seedString);
                    return gameReports.stream().map(
                            report -> new Data(
                                    puzzle.seedString,
                                    puzzleId,
                                    puzzle.solutionLength,
                                    getSolutionsCount(0, puzzle),
                                    getSolutionsCount(1, puzzle),
                                    getSolutionsCount(2, puzzle),
                                    puzzle.rankValues.get(1).rankValue,
                                    report.getDurationS(),
                                    report.getMoveCount(),
                                    difficulty
                            ));
                }
        ).toList();

        writeJsonFile(outputPath, data);
    }

    // make up some difficulty
    private Map<String, Float> computeDifficulty(final List<Puzzle> availablePuzzles, final Map<String, String> hashes,
                                                 final Map<String, List<GameReport>> gameReportMap) {
        record PuzzleDifficulty(Puzzle puzzle, float difficulty) {}
        final double maxMoves = 20;

        return availablePuzzles.stream()
                .map(puzzle -> {
                            final String puzzleId = hashes.get(puzzle.seedString);
                            final List<GameReport> gameReports = gameReportMap.get(puzzleId);
                            final var timeStats = new DescriptiveStatistics();
                            final var movesStats = new DescriptiveStatistics();

                            int optimalAttempts = 0;
                            for (GameReport gameReport : gameReports) {
                                final int moveCount = gameReport.getMoveCount();
                                switch (gameReport.exit_status) {
                                    case win -> {
                                        if (moveCount == puzzle.solutionLength) {
                                            optimalAttempts++;
                                        }
                                        timeStats.addValue(gameReport.getDurationS());
                                        movesStats.addValue(moveCount);
                                    }

                                    case reset, loss -> {
                                        movesStats.addValue(puzzle.solutionLength * 2);
                                        timeStats.addValue(10.0 * puzzle.solutionLength);
                                    }
                                }
                            }

                            final double attempts = optimalAttempts == 0 ? 0.01 : optimalAttempts;
                            final double attemptsScore = 1.0 + Math.log(gameReports.size() / attempts);
                            final double moveScore = movesStats.getMean() / puzzle.solutionLength;
                            final double timeScore = Math.sqrt(timeStats.getMean() / puzzle.solutionLength);

                            final float difficulty = (float) (moveScore * timeScore * attemptsScore);
                            return new PuzzleDifficulty(puzzle, difficulty);
                        }
                ).collect(Collectors.toMap(p -> p.puzzle.seedString, PuzzleDifficulty::difficulty));
    }

    @SuppressWarnings("ConstantConditions")
    private Map<String, String> readSeedStringHashes(final String hashesJson) throws IOException {
        // I converted Max's worlds.json like this:  (json lines)
        //    cat worlds.json | jq '.[].puzzles | flatten'  | jq -c '.[] | {seed_string, puzzle_id} ' > hashes.json

        record SeedHash(
                String seed_string,
                String puzzle_id
        ) {}

        final Map<String, String> seedHashMap = Files.readAllLines(Path.of(hashesJson)).stream()
                .map(line -> parseJson(line, SeedHash.class))
                .collect(Collectors.toMap(SeedHash::seed_string, SeedHash::puzzle_id));

        return seedHashMap;
    }

    private Map<String, List<GameReport>> readGameReports(final String gameReportFile) throws IOException {
        final List<GameReport> gameReports = iMapper.readValue(Files.readString(Path.of(gameReportFile)), new TypeReference<>() {
        });

        final Map<String, List<GameReport>> map = new HashMap<>();
        for (GameReport gameReport : gameReports) {
            final String key = gameReport.puzzle_id();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(gameReport);
        }

        return map;
    }

    int getSolutionsCount(int index, Puzzle puzzle) {
        if (index >= puzzle.solutionLengths.size()) {
            return 0;
        }
        return puzzle.solutionLengths.get(index).solutionCount;
    }

    //---------------------------------------------

    private void writeJsonFile(final Path outputPath, final Object data) throws IOException {
        final String json = iMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        try (final var bufferedWriter = Files.newBufferedWriter(outputPath)) {
            bufferedWriter.write(json);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }

        cLogger.info("Data written to file: {}", outputPath);
    }

    private <T> T parseJson(final String line, final Class<T> myMapClass) {
        try {
            return iMapper.readValue(line, myMapClass);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    //---------------------------------------------

    // INPUT
    // partial input format
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Puzzle(
            String seedString,
            int solutionLength,
            List<SolutionLength> solutionLengths,
            List<RankValue> rankValues
    ) {}

    // INPUT
    record SolutionLength(
            int solutionMoves,
            int solutionCount
    ) {}

    // INPUT
    record RankValue(
            String rankName,
            int rankValue
    ) {}

    // DATA OUTPUT
    record Data(
            String seedString,
            String puzzleId,
            int solutionLength,
            int solutionCount0,
            int solutionCount1,
            int solutionCount2,
            int bumps,
            int durationS,
            int moves,
            float difficulty
    ) {}

    // INPUT partial game report
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GameReport(
            String puzzle_id,
            String user_id,
            String start_time,
            String end_time,
            ExitStatus exit_status,
            List<Move> moves
    ) {
        public int getDurationS() {
            final var startTime = OffsetDateTime.parse(start_time + "Z").toInstant();
            final var endTime = OffsetDateTime.parse(end_time + "Z").toInstant();
            return (int) Math.round(Duration.between(startTime, endTime).toMillis() / 1000.);
        }

        public int getMoveCount() {
            return moves.size();
        }
    }

    record Move(Dir dir, Pos pos) {}

    record Dir(int dx, int dy) {}

    record Pos(int x, int y) {}

    enum ExitStatus {
        exit,   //   man slutar
        loss,   //   slut på move energy
        replay, //   efter att man klarat bana
        reset,  //   trycker reset
        win     //   om klarat den på något sätt
    }
}
