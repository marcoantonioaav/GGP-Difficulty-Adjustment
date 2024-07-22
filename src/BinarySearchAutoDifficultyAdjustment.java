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

public class BinarySearchAutoDifficultyAdjustment {

    //calculate the maximum possible variance for the distribution list
	
	private BinarySearchAutoDifficultyAdjustment() {}

    //game tournament variables
    static float win_value = 1.0f;
    static float draw_value = 0.5f;
    static float loss_value = 0.0f;

    //statistical test variables
    static float STAT_P_VALUE = 0.05f;
    static int STAT_NUM_GAMES = 20;

    //variables that will not automatically adjust
    static double THINKING_TIME = 1.0;
    static int MAX_STEPS = 100;

    static List<List<Float>> no_statistically_different_bigger_config = new ArrayList<>();
    static List<List<Float>> no_statistically_different_smaller_config = new ArrayList<>();
    static List<Distribution> baseDistributions = new ArrayList<>();

	public static void main(final String[] args) {

        //variables that will automatically adjust
        float ALPHA_BETA_SUM = 1000.0f;
        int MAX_ITERATIONS = 3;

        float ALPHA_START = ALPHA_BETA_SUM/2;
        float BETA_START = ALPHA_BETA_SUM/2;
        float ALPHA_STEP_START = ALPHA_BETA_SUM/4;
        float BETA_STEP_START = ALPHA_BETA_SUM/4;

        //game list
		List<String> GAME_NAMES = List.of("Tic-tac-toe.lud", "Tapatan.lud", "Alquerque.lud", "Reversi.lud");

		for(String gameName : GAME_NAMES) {
			System.out.println("New game.");
            System.out.println("Game : " + gameName);

            float alpha = ALPHA_START;
            float beta = BETA_START;
            float alphaStep = ALPHA_STEP_START;
            float betaStep = BETA_STEP_START;

            List<List<Float>> all_ai_configs = new ArrayList<>();
            List<List<Float>> statistically_different_ai_configs = new ArrayList<>();
            no_statistically_different_bigger_config = new ArrayList<>();
            no_statistically_different_smaller_config = new ArrayList<>();
            baseDistributions = new ArrayList<>();

            //add start config to list
            List<Float> start_config = new ArrayList<>();
            start_config.add(alpha);
            start_config.add(beta);
            statistically_different_ai_configs.add(start_config);
            all_ai_configs.add(start_config);

            List<Float> start_config2 = new ArrayList<>();
            float alpha2 = ALPHA_START/ALPHA_BETA_SUM;
            float alpha2Step = alpha2/2;
            for(int i = 0; i < MAX_ITERATIONS; i++){
                alpha2 += alpha2Step;
                alpha2Step = alpha2Step/2;
            }
            start_config2.add(ALPHA_BETA_SUM*alpha2);
            start_config2.add(ALPHA_BETA_SUM*(1-alpha2));
            all_ai_configs.add(start_config2);

            Game game_start = GameLoader.loadGameFromName(gameName);
            boolean start_config_distributions2 = getDistributionsIfStatisticallyDifferent(statistically_different_ai_configs, start_config2, game_start);

            if(start_config_distributions2){
                statistically_different_ai_configs.add(start_config2);
            }

            List<Float> start_config3 = new ArrayList<>();
            start_config3.add(ALPHA_BETA_SUM*(1-alpha2));
            start_config3.add(ALPHA_BETA_SUM*alpha2);
            all_ai_configs.add(start_config3);

            boolean start_config_distributions3 = getDistributionsIfStatisticallyDifferent(statistically_different_ai_configs, start_config3, game_start);

            if(start_config_distributions3){
                statistically_different_ai_configs.add(start_config3);
            }

            if(!start_config_distributions2 && !start_config_distributions3){
                boolean start_config_distributions = compareConfigs(start_config2, start_config3, game_start);
                if(!start_config_distributions){
                    System.out.println("No statistically different configurations found for start config.");
                }
                else{
                    statistically_different_ai_configs.add(start_config2);
                    statistically_different_ai_configs.add(start_config3);

                    System.out.println("Two statistically different configurations found for start config.");
                }
                continue;
            }
            
            //generate the rest of the configs
            for(int i = 0; i < MAX_ITERATIONS; i++){
                List<List<Float>> statistically_different_ai_configs_copy = new ArrayList<>(statistically_different_ai_configs);
                boolean new_config_generated = false;
                for(List<Float> config : statistically_different_ai_configs_copy){

                    //print all generated ai configs
                    System.out.println("All stats different ai configs:");
                    for(List<Float> generated_config : statistically_different_ai_configs){
                        System.out.println("Generated config: " + generated_config.get(0) + " " + generated_config.get(1));
                    }

                    alpha = config.get(0);
                    beta = config.get(1);

                    alpha += alphaStep;
                    beta -= betaStep;

                    List<Float> new_config1 = new ArrayList<>();
                    new_config1.add(alpha);
                    new_config1.add(beta);

                    //if new config is not already in the list, add it
                    if(alpha < ALPHA_BETA_SUM && alpha > 0 && beta < ALPHA_BETA_SUM && beta > 0 && !all_ai_configs.contains(new_config1)){
                        //make statistical test
                        Game game = GameLoader.loadGameFromName(gameName);
                        boolean config1_distributions = getDistributionsIfStatisticallyDifferent(statistically_different_ai_configs, new_config1, game);

                        all_ai_configs.add(new_config1);
                        if(config1_distributions){
                            statistically_different_ai_configs.add(new_config1);
                            new_config_generated = true;
                        }
                    }

                    alpha = config.get(0);
                    beta = config.get(1);

                    alpha -= alphaStep;
                    beta += betaStep;

                    List<Float> new_config2 = new ArrayList<>();
                    new_config2.add(alpha);
                    new_config2.add(beta);

                    //if new config is not already in the list, add it
                    if(alpha < ALPHA_BETA_SUM && alpha > 0 && beta < ALPHA_BETA_SUM && beta > 0 && !all_ai_configs.contains(new_config2)){
                        //make statistical test
                        Game game = GameLoader.loadGameFromName(gameName);
                        boolean config2_distributions = getDistributionsIfStatisticallyDifferent(statistically_different_ai_configs, new_config2, game);

                        all_ai_configs.add(new_config2);
                        if(config2_distributions){
                            statistically_different_ai_configs.add(new_config2);
                            new_config_generated = true;
                        }
                    }
                }
                if(!new_config_generated){
                    System.out.println("No new config generated.");
                    System.out.println("Skipping to next game.");
                    break;
                }
                alphaStep /= 2;
                betaStep /= 2;
            }
            //print all generated ai configs
            System.out.println("All ai configs:");
            for(List<Float> generated_config : all_ai_configs){
                System.out.println("Generated config: " + generated_config.get(0) + " " + generated_config.get(1));
            }
            //print all statistically different ai configs
            System.out.println("Statistically different ai configs:");
            for(List<Float> generated_config : statistically_different_ai_configs){
                System.out.println("Generated config: " + generated_config.get(0) + " " + generated_config.get(1));
            }
		}
        System.out.println("End of program.");
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

    private static boolean equalAIConfigurations(BetaStochasticUCT ai1, BetaStochasticUCT ai2){
        return ai1.getAlpha() == ai2.getAlpha() && ai1.getBeta() == ai2.getBeta();
    }

    private static boolean equalAIConfigurations(String ai1, String ai2){
        String[] ai1Parts = ai1.split(" ");
        String[] ai2Parts = ai2.split(" ");

        if(ai1Parts[0].equals(ai2Parts[0]) && ai1Parts[1].equals(ai2Parts[1])){
            return true;
        }
        if(ai1Parts[0].equals(ai2Parts[1]) && ai1Parts[1].equals(ai2Parts[0])){
            return true;
        }
        return false;
    }

    private static List<Float> findSmallestConfigBiggerThan(List<List<Float>> statistically_different_ai_configs, List<Float> current_config){
        List<Float> smallest_config = null;
        for(List<Float> config : statistically_different_ai_configs){
            if(config.get(0) > current_config.get(0)){
                if(smallest_config == null){
                    smallest_config = config;
                }
                else if(config.get(0) < smallest_config.get(0)){
                    smallest_config = config;
                }
            }
        }
        return smallest_config;
    }

    private static List<Float> findBiggestConfigSmallerThan(List<List<Float>> statistically_different_ai_configs, List<Float> current_config){
        List<Float> biggest_config = null;
        for(List<Float> config : statistically_different_ai_configs){
            if(config.get(0) < current_config.get(0)){
                if(biggest_config == null){
                    biggest_config = config;
                }
                else if(config.get(0) > biggest_config.get(0)){
                    biggest_config = config;
                }
            }
        }
        return biggest_config;
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

        String distributionName1v2 = getDistributionName(agent1Name, agent2Name);
        String distributionName2v1 = getDistributionName(agent2Name, agent1Name);

        Distribution distribution1v2 = new Distribution(distributionName1v2, data1v2);
        Distribution distribution2v1 = new Distribution(distributionName2v1, data2v1);
        
        List<Distribution> distributions = new ArrayList<>();
        distributions.add(distribution1v2);
        distributions.add(distribution2v1);

        return distributions;
    }

    //does the same thing as getDistribution, but makes a test every numGamesStep games and stops if the p value is less than STAT_P_VALUE
    private static boolean getDistributionUntilStatDiff(AI agent1, AI agent2, int numGames, Game game, int MAX_STEPS, double THINKING_TIME, List<Distribution> baseDistributions, int numGamesStep){
        System.out.println("Performing statistical test.");
        List<Distribution> distributions = new ArrayList<>();

        boolean statisticallyDifferent = false;
        int iterations = 0;

        while(!statisticallyDifferent && iterations < numGames){
            List<Float> data1v2 = new ArrayList<>();
            List<Float> data2v1 = new ArrayList<>();

            for(int i = 0; i < Math.ceil(numGamesStep/2); i++){
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

            for(int i = 0; i < Math.ceil(numGamesStep/2); i++){
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

            String distributionName1v2 = getDistributionName(agent1Name, agent2Name);
            String distributionName2v1 = getDistributionName(agent2Name, agent1Name);

            Distribution distribution1v2 = new Distribution(distributionName1v2, data1v2);
            Distribution distribution2v1 = new Distribution(distributionName2v1, data2v1);
            
            distributions.add(distribution1v2);
            distributions.add(distribution2v1);

            Distribution unifiedDistribution1 = new Distribution(distributionName1v2, new ArrayList<>());
            Distribution unifiedDistribution2 = new Distribution(distributionName2v1, new ArrayList<>());

            for(Distribution distribution : distributions){
                if(distribution.getName().equals(distributionName1v2)){
                    unifiedDistribution1.getData().addAll(distribution.getData());
                }
                else if(equalAIConfigurations(distribution.getName(), distributionName1v2)){
                    Distribution invertedDistribution = new Distribution("", new ArrayList<>());
                    for(float value : distribution.getData()){
                        invertedDistribution.getData().add(win_value-value+loss_value);
                    }
                    unifiedDistribution1.getData().addAll(invertedDistribution.getData());
                }
                if(distribution.getName().equals(distributionName2v1)){
                    unifiedDistribution2.getData().addAll(distribution.getData());
                }
                else if(equalAIConfigurations(distribution.getName(), distributionName2v1)){
                    Distribution invertedDistribution = new Distribution("", new ArrayList<>());
                    for(float value : distribution.getData()){
                        invertedDistribution.getData().add(win_value-value+loss_value);
                    }
                    unifiedDistribution2.getData().addAll(invertedDistribution.getData());
                }
            }

            String baseDistributionName1 = getDistributionName(agent1Name, agent1Name);
            String baseDistributionName2 = getDistributionName(agent2Name, agent2Name);

            //find base distributions
            Distribution baseDistribution1 = new Distribution(baseDistributionName1, new ArrayList<>());
            Distribution baseDistribution2 = new Distribution(baseDistributionName2, new ArrayList<>());

            for(Distribution distribution : baseDistributions){
                if(distribution.getName().equals(baseDistributionName1)){
                    baseDistribution1.getData().addAll(distribution.getData());
                }
                else if(equalAIConfigurations(distribution.getName(), baseDistributionName1)){
                    Distribution invertedDistribution = new Distribution("", new ArrayList<>());
                    for(float value : distribution.getData()){
                        invertedDistribution.getData().add(win_value-value+loss_value);
                    }
                    baseDistribution1.getData().addAll(invertedDistribution.getData());
                }
                if(distribution.getName().equals(baseDistributionName2)){
                    baseDistribution2.getData().addAll(distribution.getData());
                }
                else if(equalAIConfigurations(distribution.getName(), baseDistributionName2)){
                    Distribution invertedDistribution = new Distribution("", new ArrayList<>());
                    for(float value : distribution.getData()){
                        invertedDistribution.getData().add(win_value-value+loss_value);
                    }
                    baseDistribution2.getData().addAll(invertedDistribution.getData());
                }
            }

            //perform statistical test
            float p_value1 = baseDistribution1.p_value(unifiedDistribution1);
            float p_value2 = baseDistribution2.p_value(unifiedDistribution2);

            if(p_value1 < STAT_P_VALUE || p_value2 < STAT_P_VALUE){
                if(p_value1 < STAT_P_VALUE){
                    System.out.println("P value statistically significant: " + p_value1);
                    System.out.println("Base distribution:" + baseDistributionName1);
                }
                if(p_value2 < STAT_P_VALUE){
                    System.out.println("P value 2 statistically significant: " + p_value2);
                    System.out.println("Base distribution:" + baseDistributionName2);
                }
                statisticallyDifferent = true;
            }
            else{
                //System.out.println("P value: " + p_value1);
                //System.out.println("P value 2: " + p_value2);
            }
            iterations += numGamesStep;
        }

        if(iterations < numGames){
            System.out.println("Saved " + (numGames-iterations) + " iterations.");
        }

        return statisticallyDifferent;
    }

    private static String getDistributionName(String agent1, String agent2){
        return agent1 + " " + agent2;
    }

    private static boolean compareConfigs(List<Float> current_config, List<Float> smallest_config, Game game){
        List<Distribution> current_config_distributions = new ArrayList<>();

        BetaStochasticUCT baseAi1 = new BetaStochasticUCT(current_config.get(0), current_config.get(1));
        BetaStochasticUCT baseAi2 = new BetaStochasticUCT(current_config.get(0), current_config.get(1));

        BetaStochasticUCT newAi = new BetaStochasticUCT(smallest_config.get(0), smallest_config.get(1));

        String unifiedBaseDistributionName = getDistributionName(baseAi1.friendlyName(), baseAi2.friendlyName());
        String unifiedBaseDistributionName2 = getDistributionName(newAi.friendlyName(), newAi.friendlyName());

        String unifiedNewDistributionName = getDistributionName(newAi.friendlyName(), baseAi2.friendlyName());

        List<Distribution> existingBaseDistribution = new ArrayList<>();
        List<Distribution> existingBaseDistribution2 = new ArrayList<>();

        boolean baseAlreadyExists = false;
        boolean baseAlreadyExists2 = false;
        for(Distribution distribution : baseDistributions){
            if(distribution.getName().equals(unifiedBaseDistributionName)){
                baseAlreadyExists = true;
                existingBaseDistribution.add(distribution);
            }
            if(distribution.getName().equals(unifiedBaseDistributionName2)){
                baseAlreadyExists2 = true;
                existingBaseDistribution2.add(distribution);
            }
        }

        List<Distribution> baseDistribution = new ArrayList<>();
        List<Distribution> baseDistribution2 = new ArrayList<>();

        if(baseAlreadyExists){
            System.out.println("Base distribution already exists.");
            baseDistribution = existingBaseDistribution;
        }
        else{
            System.out.println("Base distribution does not exist.");
            System.out.println("Generating base distribution.");
            baseDistribution = getDistribution(baseAi1, baseAi2, STAT_NUM_GAMES, game, MAX_STEPS, THINKING_TIME);

            //save base distribution
            for(Distribution distribution : baseDistribution){
                baseDistributions.add(distribution);
            }
        }

        if(baseAlreadyExists2){
            System.out.println("Base distribution 2 already exists.");
            baseDistribution2 = existingBaseDistribution2;
        }
        else{
            System.out.println("Base distribution 2 does not exist.");
            System.out.println("Generating base distribution 2.");
            baseDistribution2 = getDistribution(newAi, newAi, STAT_NUM_GAMES, game, MAX_STEPS, THINKING_TIME);

            //save base distribution
            for(Distribution distribution : baseDistribution2){
                baseDistributions.add(distribution);
            }
        }

        boolean newDistribution = getDistributionUntilStatDiff(newAi, baseAi2, STAT_NUM_GAMES, game, MAX_STEPS, THINKING_TIME, baseDistributions, 2);

        return newDistribution;
    }

    private static boolean getDistributionsIfStatisticallyDifferent(List<List<Float>> statistically_different_ai_configs, List<Float> current_config, Game game){
        System.out.println("Current config: " + current_config.get(0) + " " + current_config.get(1));
        List<Distribution> current_config_distributions = new ArrayList<>();

        //get smallest config bigger than current config
        List<Float> smallest_config = findSmallestConfigBiggerThan(statistically_different_ai_configs, current_config);

        //get biggest config smaller than current config
        List<Float> biggest_config = findBiggestConfigSmallerThan(statistically_different_ai_configs, current_config);

        if(smallest_config == null && biggest_config == null){
            return false;
        }

        //check if current config is already in the list of no statistically different configs
        if(smallest_config != null){
            if(no_statistically_different_bigger_config.contains(current_config) || no_statistically_different_smaller_config.contains(smallest_config)){
                return false;
            }
        }
        if(biggest_config != null){
            if(no_statistically_different_smaller_config.contains(current_config) || no_statistically_different_bigger_config.contains(biggest_config)){
                return false;
            }
        }

        boolean statistically_different = false;

        //compare current config with smallest and biggest config
        if(smallest_config != null){
            System.out.println("Smallest config bigger than current config: " + smallest_config.get(0) + " " + smallest_config.get(1));
            boolean distributions = compareConfigs(current_config, smallest_config, game);
            if(distributions){
                statistically_different = true;
            }
            else{
                no_statistically_different_bigger_config.add(current_config);
                System.out.println("Config: " + current_config.get(0) + " " + current_config.get(1) + " added to no statistically different bigger config.");
                no_statistically_different_smaller_config.add(smallest_config);
                System.out.println("Config: " + smallest_config.get(0) + " " + smallest_config.get(1) + " added to no statistically different smaller config.");
            }
        }
        if(biggest_config != null){
            System.out.println("Biggest config smaller than current config: " + biggest_config.get(0) + " " + biggest_config.get(1));
            boolean distributions = compareConfigs(current_config, biggest_config, game);
            if(distributions){
                statistically_different = true;
            }
            else{
                no_statistically_different_smaller_config.add(current_config);
                System.out.println("Config: " + current_config.get(0) + " " + current_config.get(1) + " added to no statistically different smaller config.");
                no_statistically_different_bigger_config.add(biggest_config);
                System.out.println("Config: " + biggest_config.get(0) + " " + biggest_config.get(1) + " added to no statistically different bigger config.");
            }
        }

        return statistically_different;
    }
}