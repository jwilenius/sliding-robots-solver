package net.booru.slidingrobots;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Converter {

    private ObjectMapper iMapper = new ObjectMapper();

    /**
     * @param inputFile
     * @param outputFile
     * @throws IOException
     */
    public void applyTo(final String inputFile, final String outputFile) throws IOException {
        final Path inputPath = Path.of(inputFile);
        final Path outputPath = Path.of(outputFile);

        if (inputFile.isEmpty() || !Files.exists(inputPath)) {
            throw new IllegalArgumentException("Need to provide an input maps file.");
        }

        if (outputFile.isEmpty()) {
            throw new IllegalArgumentException("Need to provide an output json file.");
        }

        final List<MapWithStars> convertedMaps = Files.readAllLines(inputPath).stream()
                .map(line -> parseJson(line, Map.class))
                .map(m -> MapWithStars.of(m.seedString, m.solutionLength))
                .toList();

        final WorldSpec worldSpec = new WorldSpec(convertedMaps.size() / 2, "1000 years of pain", convertedMaps);
        final String worldSpecJson = iMapper.writerWithDefaultPrettyPrinter().writeValueAsString(worldSpec);
        try (final var bufferedWriter = Files.newBufferedWriter(outputPath)) {
            bufferedWriter.write(worldSpecJson);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
    }

    private Map parseJson(final String line, final Class<Map> myMapClass) {
        try {
            return iMapper.readValue(line, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // partial input format
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Map(String seedString, int solutionLength) {
    }

    // output format
    record WorldSpec(int min_stars, String world_name, List<MapWithStars> puzzles) {
    }

    record MapWithStars(String seed_string, int max_moves, int[] star_moves) {

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
        public static MapWithStars of(String seedString, int optimal) {
            final int threeStars = optimal;
            final int twoStars = max(optimal + optimal / 2, threeStars + 1, 5);
            final int oneStar = max(optimal + (4 * optimal) / 5, twoStars + 1, 8);
            final int maxMoves = max(optimal + optimal, oneStar + 1, 10);

            return new MapWithStars(seedString, maxMoves, new int[]{threeStars, twoStars, oneStar});
        }

        private static int max(int a, int b, int c) {
            return Math.max(a, Math.max(b, c));
        }
    }

}
