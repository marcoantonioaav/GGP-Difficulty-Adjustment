import search.mcts.MCTS;
import search.mcts.backpropagation.BackpropagationStrategy;
import search.mcts.backpropagation.MonteCarloBackprop;
import search.mcts.finalmoveselection.FinalMoveSelectionStrategy;
import search.mcts.playout.PlayoutStrategy;
import search.mcts.playout.RandomPlayout;
import search.mcts.selection.SelectionStrategy;
import search.mcts.selection.UCB1;

public class BetaStochasticUCT extends MCTS {
    public BetaStochasticUCT(float alpha, float beta) {
        super(
            new UCB1(Math.sqrt(2.0)), 
            new RandomPlayout(200), 
            new MonteCarloBackprop(), 
            new BetaStochasticSelection(alpha, beta)
            );
        this.friendlyName = "UCT alpha:" + alpha + " beta:" + beta;
    }

    public BetaStochasticUCT(SelectionStrategy selectionStrategy, PlayoutStrategy playoutStrategy,
            BackpropagationStrategy backpropagationStrategy, FinalMoveSelectionStrategy finalMoveSelectionStrategy) {
        super(selectionStrategy, playoutStrategy, backpropagationStrategy, finalMoveSelectionStrategy);
    }
}
