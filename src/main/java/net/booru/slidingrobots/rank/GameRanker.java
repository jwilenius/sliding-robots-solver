package net.booru.slidingrobots.rank;

import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.rank.multidim.MultiDimRanking;
import net.booru.slidingrobots.rank.multidim.Rank;
import net.booru.slidingrobots.state.Game;

import java.util.ArrayList;
import java.util.List;

/**
 * Counts the moves and the number of bumps against other robots.
 */
public class GameRanker {

    /**
     * @param games     The games to rank
     * @param solutions The solution to game at same index.
     * @return the ranked list of games, most difficult game last
     */
    public List<GameWithSolution> apply(final List<Game> games, final List<Solution> solutions) {
        if (games.size() != solutions.size()) {
            throw new IllegalArgumentException("Games and solutions must be same size!");
        }

        final List<GameWithSolution> gameWithSolutions = new ArrayList<>(games.size());
        for (int i = 0; i < games.size(); i++) {
            gameWithSolutions.add(new GameWithSolution(games.get(i), solutions.get(i), null));
        }

        return apply(gameWithSolutions);
    }

    public List<GameWithSolution> apply(final List<GameWithSolution> gameWithSolutions) {

        final var bumpsCounter = new BumpsCounter();

        final MultiDimRanking<GameWithSolution> mdRanker = new MultiDimRanking<>(List.of(
                new Rank<>("Moves",
                        gs -> gs.solution().getStatistics().getSolutionLength(),
                        v -> {
                            if (v <= 4) return 0; // epsilon = 0 for few moves
                            if (v <= 6) return 1; // epsilon = 1 for medium
                            return 2; // epsilon = 2
                        }),
                new Rank<>("Bumps",
                        gs -> bumpsCounter.apply(gs.solution()),
                        v -> 1),  // 0 epsilon
                new Rank<>("Solutions_0", // count number of solutions 1 additional moves
                        gs -> -gs.solution().getStatistics().getSolutionLengthCount(0),
                        v -> 0),  // 0 epsilon
                new Rank<>("Solutions_1", // count number of solutions 1 additional moves
                        gs -> -gs.solution().getStatistics().getSolutionLengthCount(1),
                        v -> v * 0.05),  // 10% epsilon
                new Rank<>("Solutions_2", // count number of solutions 2 additional moves
                        gs -> -gs.solution().getStatistics().getSolutionLengthCount(1),
                        v -> v * 0.10), // 15% epsilon
                new Rank<>("MovesFinal",
                        gs -> gs.solution().getStatistics().getSolutionLength(),
                        v -> 0)   // if same then moves
        ));

        final List<GameWithSolution> gameWithSolutionsRanked = mdRanker.applyRank(gameWithSolutions);
        return gameWithSolutionsRanked.stream()
                .map(g -> new GameWithSolution(g.game(), g.solution(), mdRanker.getResultForElement(g)))
                .toList();
    }
}
