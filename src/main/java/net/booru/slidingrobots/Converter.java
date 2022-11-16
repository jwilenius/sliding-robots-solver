package net.booru.slidingrobots;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class Converter {

    private ObjectMapper iMapper = new ObjectMapper();

    /**
     * @param inputFile
     * @param outputFile
     * @throws IOException
     */
    public void applyTo(final String inputFile, final String outputFile) throws IOException {
        if (!outputFile.endsWith(".json")) {
            throw new IllegalArgumentException("output file should be json");
        }

        final Path inputPath = Path.of(inputFile);
        final Path outputPath = Path.of(outputFile);
        final Path outputPathData = Path.of(outputFile.replace(".json", "_data.json"));

        if (inputFile.isEmpty() || !Files.exists(inputPath)) {
            throw new IllegalArgumentException("Need to provide an input maps file.");
        }

        final List<Map> maps = Files.readAllLines(inputPath).stream()
                .flatMap(line -> Stream.ofNullable(parseJson(line, Map.class)))
                .toList();

        convertToBackendFormat(outputPath, maps);
        convertToDataFormat(outputPathData, maps);
    }

    private void convertToBackendFormat(final Path outputPath, final List<Map> maps) throws IOException {
        final List<MapWithStars> convertedMaps = maps.stream()
                .map(m -> MapWithStars.of(m.seedString, m.solutionLength))
                .toList();

        final WorldSpec worldSpec = new WorldSpec(convertedMaps.size() / 2, "1000 years of pain", convertedMaps);
        writeJsonFile(outputPath, worldSpec);
    }

    private void convertToDataFormat(final Path outputPath, final List<Map> maps) throws IOException {
        final List<Data> data = maps.stream().map(
                map -> new Data(
                        map.seedString,
                        map.solutionLength,
                        getSolutionsCount(0, map),
                        getSolutionsCount(1, map),
                        getSolutionsCount(2, map),
                        map.rankValues.get(1).rankValue
                )
        ).toList();

        writeJsonFile(outputPath, data);
    }

    int getSolutionsCount(int index, Map map) {
        if (index >= map.solutionLengths.size()) {
            return 0;
        }
        return map.solutionLengths.get(index).solutionCount;
    }

    private void writeJsonFile(final Path outputPath, final Object data) throws IOException {
        final String json = iMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        try (final var bufferedWriter = Files.newBufferedWriter(outputPath)) {
            bufferedWriter.write(json);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
    }

    private Map parseJson(final String line, final Class<Map> myMapClass) {
        try {
            return iMapper.readValue(line, myMapClass);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // INPUT
    // partial input format
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Map(
            String seedString,
            int solutionLength,
            List<SolutionLength> solutionLengths,
            List<RankValue> rankValues
    ) {
    }

    // INPUT
    record SolutionLength(
            int solutionMoves,
            int solutionCount
    ) {
    }

    // INPUT
    record RankValue(
            String rankName,
            int rankValue
    ) {
    }


    // WORLD SPEC OUTPUT
    // output format
    record WorldSpec(
            int min_stars,
            String world_name,
            List<MapWithStars> puzzles
    ) {
    }

    // WORLD SPEC OUTPUT
    record MapWithStars(
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


    // DATA OUTPUT
    record Data(
            String seedString,
            int solutionLength,
            int solutuonCount0,
            int solutuonCount1,
            int solutuonCount2,
            int bumps
    ) {
    }
}
