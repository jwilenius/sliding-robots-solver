package net.booru.slidingrobots.rank;

import net.booru.slidingrobots.algorithm.model.Solution;
import net.booru.slidingrobots.rank.multidim.RankResult;
import net.booru.slidingrobots.state.Game;

public record GameWithSolution(
        Game game,
        Solution solution,
        RankResult<GameWithSolution> rank
) {
}
