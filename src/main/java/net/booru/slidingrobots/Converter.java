package net.booru.slidingrobots;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts a maps file into world files that are compatible with the backend.
 */
public class Converter {
    private static final Logger cLogger = LoggerFactory.getLogger(Converter.class);

    private final ObjectMapper iMapper = new ObjectMapper();

    /**
     * @param inputFile          the maps file to convert
     * @param outputFileBaseName the base name of the target files that will be generated
     */
    public void applyTo(final String inputFile, final String outputFileBaseName) throws IOException {
        if (!outputFileBaseName.endsWith(".json")) {
            throw new IllegalArgumentException("output file should be json");
        }

        final Path inputPath = Path.of(inputFile);

        if (inputFile.isEmpty() || !Files.exists(inputPath)) {
            throw new IllegalArgumentException("Need to provide an input maps file.");
        }

        final List<Puzzle> puzzles = Files.readAllLines(inputPath).stream()
                .flatMap(line -> Stream.ofNullable(parseJson(line, Puzzle.class)))
                .toList();

        convertToBackendFormat(Path.of(outputFileBaseName.replace(".json", "_all.json")), puzzles, puzzles.size() / 2);
        convertToBackendFormatOneWorldPerSize(outputFileBaseName, puzzles);
    }

    private void convertToBackendFormatOneWorldPerSize(final String outputFileBaseName, final List<Puzzle> puzzles) {
        final Map<Integer, List<Puzzle>> perSizeMap = puzzles.stream().collect(Collectors.groupingBy(Puzzle::solutionLength));
        final List<Integer> keysInOrder = perSizeMap.keySet().stream().sorted().toList();
        for (int key : keysInOrder) {
            final Path outputPath = Path.of(outputFileBaseName.replace(".json", "_world_" + key + ".json"));
            convertToBackendFormat(outputPath, perSizeMap.get(key), 30);
        }
    }

    private void convertToBackendFormat(final Path outputPath, final List<Puzzle> puzzles, int minStars) {
        final List<PuzzleWithStars> convertedMaps = puzzles.stream()
                .map(m -> PuzzleWithStars.of(m.seedString, m.solutionLength))
                .toList();

        final WorldSpec worldSpec = new WorldSpec(minStars, "1000 years of pain", convertedMaps);
        writeJsonFile(outputPath, worldSpec);
    }

    //-------------------------------------------------------------------------------------------------------------------------------------

    private void writeJsonFile(final Path outputPath, final Object data) {
        try (final var bufferedWriter = Files.newBufferedWriter(outputPath)) {
            final String json = iMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            bufferedWriter.write(json);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        cLogger.info("Data written to file: {}", outputPath);
    }

    private <T> T parseJson(final String line, final Class<T> myMapClass) {
        try {
            return iMapper.readValue(line, myMapClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    //-------------------------------------------------------------------------------------------------------------------------------------

    // INPUT
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Puzzle(
            String seedString,
            int solutionLength,
            List<SolutionLength> solutionLengths,
            List<RankValue> rankValues
    ) {}

    record SolutionLength(int solutionMoves, int solutionCount) {}

    record RankValue(String rankName, int rankValue) {}

    // OUTPUT
    record WorldSpec(
            int min_stars,
            String world_name,
            List<PuzzleWithStars> puzzles
    ) {}

    record PuzzleWithStars(
            String seed_string,
            int max_moves,
            int[] star_moves
    ) {
        /**
         * Note, I am using:
         * ThreeStars = Optimal,
         * TwoStars = lists:max([Optimal + Optimal div 2, ThreeStars + 1, 5]),
         * OneStar = lists:max([Optimal + (4 * Optimal) div 5, TwoStars + 1, 8]),
         * MaxMoves = lists:max([Optimal + Optimal, OneStar + 1, 10]),
         * StarMoves = [ThreeStars, TwoStars, OneStar],
         *
         * @param seedString
         * @param optimal
         */
        public static PuzzleWithStars of(String seedString, int optimal) {
            final int threeStars = optimal;
            final int twoStars = max(optimal + optimal / 2, threeStars + 1, 5);
            final int oneStar = max(optimal + (4 * optimal) / 5, twoStars + 1, 8);
            final int maxMoves = max(optimal + optimal, oneStar + 1, 10);

            return new PuzzleWithStars(seedString, maxMoves, new int[]{threeStars, twoStars, oneStar});
        }

        private static int max(int a, int b, int c) {
            return Math.max(a, Math.max(b, c));
        }
    }
}
