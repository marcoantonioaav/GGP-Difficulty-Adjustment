import java.util.ArrayList;
import java.util.List;

import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import search.minimax.AlphaBetaSearch;

public class AutoDifficultyAdjustment {

	private AutoDifficultyAdjustment() {}

    //game tournament variables
    static float win_value = 1.0f;
    static float draw_value = 0.5f;
    static float loss_value = 0.0f;

	public static void main(final String[] args) {
        //statistical test variables
        int STAT_NUM_GAMES = 20;
        float STAT_P_VALUE = 0.05f;

        //variables that will not automatically adjust
        double THINKING_TIME = 1.0;
        int MAX_STEPS = 100;
        int NUM_GAMES = 100;
        int MIN_GAMES = 3;
        float STEP_MULTIPLIER = 2.0f;

        //heuristic variables that will automatically adjust
        float CUTOFF_P_VALUE_START = 1.0f;
        float MAX_CUTOFF_P_VALUE = STAT_P_VALUE;

        //variables that will automatically adjust
        float ALPHA_START = 1;
        float ALPHA_STEP_START = 1;
        //float ALPHA_MAX = 20;
        float BETA_START = 1;
        float BETA_STEP_START = 1;
        //float BETA_MAX = 20;

        //game list
		List<String> GAME_NAMES = List.of("Tic-tac-toe.lud", "Tapatan.lud", "Alquerque.lud", "Reversi.lud");

		for(String gameName : GAME_NAMES) {
			System.out.println("New game.");
            System.out.println("Game : " + gameName);
			
            boolean isStatPValueSignificant = false;

            float alpha1 = ALPHA_START;
            float beta1 = BETA_START;
            float betaStep1 = BETA_STEP_START;
            float alphaStep1 = ALPHA_STEP_START;
            
            float alpha2 = ALPHA_START;
            float beta2 = BETA_START;
            float betaStep2 = BETA_STEP_START;
            float alphaStep2 = ALPHA_STEP_START;

            List<Distribution> distributions = new ArrayList<>();

            int game_count = 0;
            int current_config_game_count = 0;
            float lastP = 0;

            float cutoff_p_value = CUTOFF_P_VALUE_START;
            float cutoff_p_value_step = (MAX_CUTOFF_P_VALUE - CUTOFF_P_VALUE_START)/NUM_GAMES;

            while(true){
                if(isStatPValueSignificant){
                    System.out.println("Statistical p-value is significant.");
                    System.out.println("alpha1: " + alpha1);
                    System.out.println("beta1: " + beta1);
                    System.out.println("alpha2: " + alpha2);
                    System.out.println("beta2: " + beta2);
                    System.out.println("p-value: " + lastP);

                    //print distributions list
                    for(Distribution distribution : distributions){
                        System.out.println(distribution.getName());
                        System.out.println("Mean: " + distribution.getMean());
                        System.out.println("Variance: " + distribution.getVariance());
                        System.out.println("N: " + distribution.getN());
                        System.out.println("Data:");
                        for(float value : distribution.getData()){
                            System.out.println(value);
                        }
                    }

                    break;
                }
                if(game_count >= NUM_GAMES){
                    break;
                }
                isStatPValueSignificant = false;

                if(alpha1 == alpha2 && beta1 == beta2){
                    alpha2 += alphaStep2;
                    beta1 += betaStep1;
                }

                final AI agent1 = new BetaStochasticUCT(alpha1, beta1);
                final AI agent2 = new BetaStochasticUCT(alpha2, beta2);
                final Game game = GameLoader.loadGameFromName(gameName);

                List<AI> ais = new ArrayList<>();
                ais.add(agent1);
                ais.add(agent2);
                
                //check for base distributions for later comparison
                //if there are not enough games for a base distribution, add more games
                for(int i = 0; i < ais.size(); i++){
                    AI ai = ais.get(i);
                    String aiFriendlyName = ai.friendlyName();
                    int baseGameCount = 0;
                    for (Distribution distribution : distributions) {
                        if(distribution.getName().contains(aiFriendlyName + " " + aiFriendlyName)){
                            baseGameCount += distribution.getN();
                        }
                    }

                    if(baseGameCount < STAT_NUM_GAMES){
                        List<Distribution> baseDistributions = getDistribution(ai, ai, STAT_NUM_GAMES-baseGameCount, game, MAX_STEPS, THINKING_TIME);

                        Distribution unifiedDistribution = new Distribution(aiFriendlyName + " " + aiFriendlyName, new ArrayList<>());

                        for(Distribution distribution : baseDistributions){
                            unifiedDistribution.getData().addAll(distribution.getData());
                        }

                        distributions.add(unifiedDistribution);
                    }
                }

                //get distributions for comparison between agents
                List<Distribution> newDistributions = getDistribution(agent1, agent2, 2, game, MAX_STEPS, THINKING_TIME);
                game_count += 2;
                current_config_game_count += 2;

                for(Distribution distribution : newDistributions){
                    distributions.add(distribution);
                }

                //compare distributions
                String expectedBaseDistributionName1 = agent1.friendlyName() + " " + agent1.friendlyName();
                String expectedBaseDistributionName2 = agent2.friendlyName() + " " + agent2.friendlyName();

                Distribution baseDistribution1 = new Distribution(expectedBaseDistributionName1, new ArrayList<>());
                Distribution baseDistribution2 = new Distribution(expectedBaseDistributionName2, new ArrayList<>());

                String expectedDistributionName1 = agent1.friendlyName() + " " + agent2.friendlyName();
                String expectedDistributionName2 = agent2.friendlyName() + " " + agent1.friendlyName();
                
                Distribution unifiedDistribution1 = new Distribution(expectedDistributionName1, new ArrayList<>());
                Distribution unifiedDistribution2 = new Distribution(expectedDistributionName2, new ArrayList<>());
                
                for(Distribution distribution : distributions){
                    String distributionName = distribution.getName();
                    if(distributionName.contains(expectedBaseDistributionName1) || distributionName.contains(expectedBaseDistributionName2) || distributionName.contains(expectedDistributionName1) || distributionName.contains(expectedDistributionName2)){
                        Distribution invertedDistribution = new Distribution(distributionName, new ArrayList<>());
                        for(float value : distribution.getData()){
                            invertedDistribution.getData().add(win_value-value+loss_value);
                        }
                        if(distributionName.contains(expectedDistributionName1)){
                            unifiedDistribution1.getData().addAll(distribution.getData());
                            unifiedDistribution2.getData().addAll(invertedDistribution.getData());
                        }
                        if(distributionName.contains(expectedDistributionName2)){
                            unifiedDistribution2.getData().addAll(distribution.getData());
                            unifiedDistribution1.getData().addAll(invertedDistribution.getData());
                        }
                        if(distributionName.contains(expectedBaseDistributionName1)){
                            baseDistribution1.getData().addAll(distribution.getData());
                            baseDistribution2.getData().addAll(invertedDistribution.getData());
                        }
                        if(distributionName.contains(expectedBaseDistributionName2)){
                            baseDistribution2.getData().addAll(distribution.getData());
                            baseDistribution1.getData().addAll(invertedDistribution.getData());
                        }
                    }
                }

                float p1 = unifiedDistribution1.p_value(baseDistribution1);
                float p2 = unifiedDistribution2.p_value(baseDistribution2);

                if(p1 < STAT_P_VALUE || p2 < STAT_P_VALUE){
                	float newP = p1;
                    if(p2 < p1){
                        newP = p2;
                    }
                    lastP = newP;
                    isStatPValueSignificant = true;
                }
                else{
                    if(p1 < cutoff_p_value || p2 < cutoff_p_value || current_config_game_count < MIN_GAMES){
                        cutoff_p_value = CUTOFF_P_VALUE_START + cutoff_p_value_step*game_count;
                    }
                    else{
                        //reset heuristic auto-adjustment variables
                        cutoff_p_value = CUTOFF_P_VALUE_START;
                        current_config_game_count = 0;

                        //heuristic adjustment
                        float newP = p1;
                        if(p2 < p1){
                            newP = p2;
                        }
                        float pDifference = newP - lastP;
                        lastP = newP;

                        if(pDifference > 0){
                            if(p1 < p2){
                                alpha1 += (1-pDifference)*alphaStep1 * STEP_MULTIPLIER;
                                beta2 += (1-pDifference)*betaStep2 * STEP_MULTIPLIER;
                            }
                            else{
                                alpha2 += (1-pDifference)*alphaStep2 * STEP_MULTIPLIER;
                                beta1 += (1-pDifference)*betaStep1 * STEP_MULTIPLIER;
                            }
                        }
                        else{
                            alpha1 += (1+pDifference)*alphaStep1 * STEP_MULTIPLIER;
                            beta1 += (1+pDifference)*betaStep1 * STEP_MULTIPLIER;
                            alpha2 += (1+pDifference)*alphaStep2 * STEP_MULTIPLIER;
                            beta2 += (1+pDifference)*betaStep2 * STEP_MULTIPLIER;
                        }
                    }
                }
            }

            printDistributions(distributions);
            
            System.out.println("===============================");
            System.out.println("Game ended.");
            System.out.println("===============================");
		}
		
		System.out.println("===============================");
		System.out.println("End.");
		System.out.println("===============================");
	}

    private static void printDistributions(List<Distribution> distributions){
        System.out.println("===============================");
        System.out.println("Distributions");
        for(Distribution distribution : distributions){
            System.out.println("Name: " + distribution.getName());
            System.out.println("Mean: " + distribution.getMean());
            System.out.println("Variance: " + distribution.getVariance());
            System.out.println("N: " + distribution.getN());
            System.out.println("Data:");
            for(float value : distribution.getData()){
                System.out.println(value);
            }
        }
        System.out.println("===============================");
    }

    private static String playGame(AI agent1, AI agent2, Game game, int MAX_STEPS, double THINKING_TIME){
        final Trial trial = new Trial(game);
        final Context context = new Context(game, trial);

        game.start(context);

        final List<AI> ais = new ArrayList<>();
        ais.add(null);
        ais.add(agent1);
        ais.add(agent2);
        
        for (int player = 1; player < ais.size(); ++player)
        {
            System.out.println("Player " + player + " : " + ais.get(player).friendlyName());
            ais.get(player).initAI(game, player);
        }
        
        int steps = 0;

        final Model model = context.model();

        while (!context.trial().over() && steps <= MAX_STEPS)
        {
            model.startNewStep(context, ais, THINKING_TIME);
            steps++;
        }
        
        System.out.println("Outcome = " + context.trial().status());

        if(context.trial().status() != null){
            return ""+context.trial().status();
        }
        else{
            return "null";
        }
    }

    private static List<Distribution> getDistribution(AI agent1, AI agent2, int numGames, Game game, int MAX_STEPS, double THINKING_TIME){
        List<Float> data1v2 = new ArrayList<>();
        List<Float> data2v1 = new ArrayList<>();

        for(int i = 0; i < Math.ceil(numGames/2); i++){
            String result = playGame(agent1, agent2, game, MAX_STEPS, THINKING_TIME);
            if(result.contains("1")){
                data1v2.add(win_value);
            }
            else if(result.contains("2")){
                data1v2.add(loss_value);
            }
            else{
                data1v2.add(draw_value);
            }
        }

        for(int i = 0; i < Math.ceil(numGames/2); i++){
            String result = playGame(agent2, agent1, game, MAX_STEPS, THINKING_TIME);
            if(result.contains("1")){
                data2v1.add(win_value);
            }
            else if(result.contains("2")){
                data2v1.add(loss_value);
            }
            else{
                data2v1.add(draw_value);
            }
        }
        
        String agent1Name = agent1.friendlyName();
        String agent2Name = agent2.friendlyName();

        Distribution distribution1v2 = new Distribution(agent1Name + " " + agent2Name, data1v2);
        Distribution distribution2v1 = new Distribution(agent2Name + " " + agent1Name, data2v1);
        
        List<Distribution> distributions = new ArrayList<>();
        distributions.add(distribution1v2);
        distributions.add(distribution2v1);

        return distributions;
    }
}