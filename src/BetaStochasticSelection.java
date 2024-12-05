import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import other.move.Move;
import other.state.State;
import search.mcts.finalmoveselection.FinalMoveSelectionStrategy;
import search.mcts.nodes.BaseNode;
import org.apache.commons.math3.distribution.BetaDistribution;

public class BetaStochasticSelection implements FinalMoveSelectionStrategy {
	
    private float alpha = 0.5f;
    private float beta = 0.5f;
    private boolean minMaxNormalize = false;

    public static final float MAX = 1;
    public static final float MIN = -MAX;
    public static final float NEUTRAL = (MIN + MAX)/2;
    
    public BetaStochasticSelection(float alpha, float beta) {
        this.alpha = alpha;
        this.beta = beta;
        this.minMaxNormalize = false;
    }

    public BetaStochasticSelection(float alpha, float beta, boolean minMaxNormalize) {
        this.alpha = alpha;
        this.beta = beta;
        this.minMaxNormalize = minMaxNormalize;
    }

    @Override
    public Move selectMove(BaseNode rootNode) {
    	System.out.println("test");
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

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        if(this.minMaxNormalize && moves.size() > 1) {
            for(Move move : moves) {
                float score = ratedMoves.get(move);
                if(score < min) {
                    min = score;
                }
                if(score > max) {
                    max = score;
                }
            }
            if(min != max && min != Float.POSITIVE_INFINITY && max != Float.NEGATIVE_INFINITY) {
                for(Move move : moves) {
                    float score = ratedMoves.get(move);
                    ratedMoves.put(move, (score - min) / (max - min));
                }
            }
        }

        float moveQuality = getRandomOnBeta(alpha, beta);
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
    
    private float getRandomOnBeta(float alpha, float beta) {
    	BetaDistribution betaDist = new BetaDistribution(alpha, beta);
    	double random = Math.random();
    	double beta_random = betaDist.inverseCumulativeProbability(random);
        return (float)(beta_random);
    }

    @Override
    public void customise(String[] arg0) { 

    }

    public float getAlpha() {
        return alpha;
    }

    public float getBeta() {
        return beta;
    }
}
