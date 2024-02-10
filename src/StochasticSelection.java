import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import other.move.Move;
import other.state.State;
import search.mcts.finalmoveselection.FinalMoveSelectionStrategy;
import search.mcts.nodes.BaseNode;

public class StochasticSelection implements FinalMoveSelectionStrategy {
    private float mean = 1f;
    private float standardDeviation = 0f;

    public static final float MAX = 1;
    public static final float MIN = -MAX;
    public static final float NEUTRAL = (MIN + MAX)/2;

    public StochasticSelection(float mean, float standardDeviation) {
        this.mean = mean;
        this.standardDeviation = standardDeviation;
    }

    @Override
    public Move selectMove(BaseNode rootNode) {
        State rootState = rootNode.contextRef().state();
        int moverAgent = rootState.playerToAgent(rootState.mover());
        int numChildren = rootNode.numLegalMoves();

        HashMap<Move, Float> ratedMoves = new HashMap<>();
        for(int i = 0; i < numChildren; ++i) {
            BaseNode child = rootNode.childForNthLegalMove(i);
            //int numVisits = child == null ? 0 : child.numVisits();
            double score = child == null ? 0.0 : child.expectedScore(moverAgent);
            Move move = rootNode.nthLegalMove(i);
            ratedMoves.put(move, normalizeTo0To1((float)score));
        }

        return selectMoveByScore(ratedMoves);
    }

    private Move selectMoveByScore(HashMap<Move, Float> ratedMoves) {
        ArrayList<Move> moves = new ArrayList<>(ratedMoves.keySet());
        float moveQuality = getRandomOnGaussian(mean, standardDeviation);
        Move selectedMove = null;
        float selectedQualityDifference = Float.POSITIVE_INFINITY;
        for(Move move : moves) {
            float qualityDifference = Math.abs(ratedMoves.get(move) - moveQuality);
            if(qualityDifference < selectedQualityDifference) {
                selectedMove = move;
                selectedQualityDifference = qualityDifference;
            }
        }
        return selectedMove;
    }

    private float normalizeTo0To1(float score) {
        return (score - MIN) / (MAX - MIN);
    }

    private float getRandomOnGaussian(float mean, float standardDeviation) {
        return (float)(new Random().nextGaussian()*standardDeviation)+mean;
    }

    @Override
    public void customise(String[] arg0) { 

    }
}
