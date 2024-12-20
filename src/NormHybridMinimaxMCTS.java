import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.distribution.BetaDistribution;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.context.Context;
import other.move.Move;
import utils.AIUtils;

public class NormHybridMinimaxMCTS extends AI {
    protected int player = -1;
    private Game game;

    private float mean = 1f;
    private float standardDeviation = 0f;

    private int evaluationPlayouts = 15;//15
    private int maxPlayoutDepth = 100;//100

    private List<Integer> reachedDepths = new ArrayList<>();
    private List<Long> spentTimes = new ArrayList<>();
    private int playouts = 0;
    private int prunedPlayouts = 0;
    private int movesAvoided = 0;

    public static final float MAX = 1;
    public static final float MIN = -MAX;
    public static final float NEUTRAL = (MIN + MAX)/2;
    
    private float alpha = 0.5f;
    private float beta = 0.5f;
    private boolean minMaxNormalize = false;

    public NormHybridMinimaxMCTS(float alpha, float beta, boolean minMaxNormalize) {
		this.friendlyName = "NormHybrid Minimax-MCTS" + " alpha:" + alpha + " beta:" + beta + " minMaxNormalize:" + minMaxNormalize;
		this.alpha = alpha;
		this.beta = beta;
		this.minMaxNormalize = minMaxNormalize;
	}
    
    private float getRandomOnBeta(float alpha, float beta) {
    	BetaDistribution betaDist = new BetaDistribution(alpha, beta);
    	double random = Math.random();
    	double beta_random = betaDist.inverseCumulativeProbability(random);
        return (float)(beta_random);
    }

    public void setMean(float mean) {
        this.mean = mean;
        this.friendlyName = "Hybrid Minimax-MCTS m:" + mean + " std:" + standardDeviation;
    }

    public void setStandardDeviation(float standardDeviation) {
        this.standardDeviation = standardDeviation;
        this.friendlyName = "Hybrid Minimax-MCTS m:" + mean + " std:" + standardDeviation;
    }

    public void setEvaluationPlayouts(int evaluationPlayouts) {
        this.evaluationPlayouts = evaluationPlayouts;
    }

    public void setMaxPlayoutDepth(int maxPlayoutDepth) {
        this.maxPlayoutDepth = maxPlayoutDepth;
    }

    public int getFirstReachedDepth() {
        return reachedDepths.get(0);
    }

    public float getMeanMovesAvoided() {
        return (float)movesAvoided/(float)playouts;
    }

    public float getMeanPrunedPlayouts() {
        return (float)prunedPlayouts/(float)playouts;
    }

    public int getMeanReachedDepth() {
        int reachedDepthSum = 0;
        for(int reachedDepth : reachedDepths) {
            reachedDepthSum += reachedDepth;
        }
        return reachedDepthSum/reachedDepths.size();
    }

    public float getMeanSpentTimeSeconds() {
        long spentTimeSum = 0;
        for(long spentTime : spentTimes) {
            spentTimeSum += spentTime;
        }
        return (spentTimeSum/spentTimes.size())/1000f;
    }

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);

        HashMap<Move, Float> ratedMoves = new HashMap<>();

        final long SEARCH_TIME_MILLIS = (long)(maxSeconds*1000);
        long startTime = System.currentTimeMillis();
        long timeSpent = 0;
        int depth = 0;
        while(timeSpent < SEARCH_TIME_MILLIS) {
            for(Move move : legalMoves) {
                Context newContext = new Context(context);
                newContext.game().apply(newContext, move);
                float score = minimax(newContext, depth, MIN, MAX, false);
                ratedMoves.put(move, normalizeTo0To1(score));
                timeSpent = System.currentTimeMillis() - startTime;
                if(timeSpent >= SEARCH_TIME_MILLIS)
                    break;
            }
            depth++;
        }
        reachedDepths.add(depth);
        spentTimes.add(timeSpent);
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
        
        float moveQuality = getRandomOnBeta(this.alpha, this.beta);
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

    private float minimax(Context context, int depth, float alpha, float beta, boolean isMaximizing) {
        if(depth == 0 || context.trial().over())
            return evaluate(context, isMaximizing);
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);
        if(isMaximizing) {
            float maxValue = MIN;
            for(Move move : legalMoves) {
                Context newContext = new Context(context);
				newContext.game().apply(newContext, move);
                float newValue = minimax(newContext, depth - 1, alpha, beta, false);
                maxValue = Math.max(maxValue, newValue);
                if(maxValue >= beta)
                    break;
                alpha = Math.max(alpha, maxValue);
            }
            return maxValue;
        }
        else {
            float minValue = MAX;
            for(Move move : legalMoves) {
                Context newContext = new Context(context);
                newContext.game().apply(newContext, move);
                float newValue = minimax(newContext, depth - 1, alpha, beta, true);
                minValue = Math.min(minValue, newValue);
                if(minValue <= alpha)
                    break;
                beta = Math.min(beta, minValue);
            }
            return minValue;
        }
    }
    private float evaluate(Context context, boolean isMaximizing) {
        if(context.trial().over())
            return evaluateTerminalState(context);
        int startingPlayer = player;
        if(!isMaximizing)
            startingPlayer = 1 - player;
        return evaluateWithPlayouts(context, startingPlayer);
    }

    private float evaluateTerminalState(Context context) {
        if(context.winners().contains(this.player))
            return MAX;
        if(!context.winners().isEmpty())
            return MIN;
        return NEUTRAL;
    }

    private float evaluateWithPlayouts(Context context, int startingPlayer) {
        float evaluation = 0f;
        for(int p = 0; p < evaluationPlayouts; p++)
            evaluation += makePlayout(context, startingPlayer);
        return evaluation/evaluationPlayouts;
    }

    private float makePlayout(Context context, int startingPlayer) {
        playouts++;
		Context newContext = new Context(context);
		int currentPlayer = startingPlayer;
        HashSet<Long> visitedStates = new HashSet<>();
        int depth = 0;
		while(!newContext.trial().over() && depth < maxPlayoutDepth) {
			Move move = getRandomUniqueMove(newContext, currentPlayer, visitedStates);
            if(move == null) {
                prunedPlayouts++;
                return NEUTRAL;
            }
			newContext.game().apply(newContext, move);
            visitedStates.add(newContext.state().fullHash());
			currentPlayer = 1 - currentPlayer;
            depth++;
		}
        if(depth == maxPlayoutDepth)
            prunedPlayouts++;
		return evaluateTerminalState(newContext);
	}

    private Move getRandomUniqueMove(Context context, int player, HashSet<Long> visitedStates) {
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        while(!legalMoves.isEmpty()) {
            int r = ThreadLocalRandom.current().nextInt(legalMoves.size());
            Move move = legalMoves.get(r);
            Context newContext = new Context(context);
            newContext.game().apply(newContext, move);
            long stateHash = newContext.state().fullHash();
            if(visitedStates.contains(stateHash) && !move.actions().get(0).isForced()) {
                legalMoves.remove(r);
                movesAvoided++;
            }
            else
                return move;
        }
        return null;
    }

    @Deprecated
    public Move getRandomMove(Context context, int player) {
		FastArrayList<Move> legalMoves = game.moves(context).moves();
		
		if (!game.isAlternatingMoveGame())
			legalMoves = AIUtils.extractMovesForMover(legalMoves, player);
		
		final int r = ThreadLocalRandom.current().nextInt(legalMoves.size());
		return legalMoves.get(r);
	}

    @Override
	public void initAI(final Game game, final int playerID)
	{
        this.game = game;
		this.player = playerID;
        this.reachedDepths.clear();
        this.spentTimes.clear();
        this.movesAvoided = 0;
        this.playouts = 0;
        this.prunedPlayouts = 0;
	}

	public float getAlpha() {
		return this.alpha;
	}

	public float getBeta() {
		return this.beta;
	}
}
