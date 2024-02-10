import search.mcts.MCTS;
import search.mcts.backpropagation.BackpropagationStrategy;
import search.mcts.backpropagation.MonteCarloBackprop;
import search.mcts.finalmoveselection.FinalMoveSelectionStrategy;
import search.mcts.playout.PlayoutStrategy;
import search.mcts.playout.RandomPlayout;
import search.mcts.selection.SelectionStrategy;
import search.mcts.selection.UCB1;

public class ModifiedUCT extends MCTS {
    public ModifiedUCT(float mean, float standardDeviation) {
        super(
            new UCB1(Math.sqrt(2.0)), 
            new RandomPlayout(200), 
            new MonteCarloBackprop(), 
            new StochasticSelection(mean, standardDeviation)
            );
        this.friendlyName = "UCT m:" + mean + " std:" + standardDeviation;
    }

    public ModifiedUCT(SelectionStrategy selectionStrategy, PlayoutStrategy playoutStrategy,
            BackpropagationStrategy backpropagationStrategy, FinalMoveSelectionStrategy finalMoveSelectionStrategy) {
        super(selectionStrategy, playoutStrategy, backpropagationStrategy, finalMoveSelectionStrategy);
    }
}
