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

public class GaussianDifficultyAdjustmentTournament {

	private GaussianDifficultyAdjustmentTournament() {}

	public static void main(final String[] args) {
		List<Float> means = List.of(0.4f, 0.6f, 1.0f);
		List<Float> std_devs = List.of(0.3f, 0.3f, 0.3f);
		
		List<String> GAME_NAMES = List.of(/*"Tic-tac-toe.lud", "Tapatan.lud",*/ "Alquerque.lud", "Reversi.lud");
		//List<String> GAME_NAMES = List.of("Reversi.lud");
		int MAX_STEPS = 100;
		int NUM_GAMES = 20;
		double THINKING_TIME = 1.0;
		
		for(String gameName : GAME_NAMES) {

			for (int agent1_index = 0; agent1_index < 3; agent1_index++) {
				float mean1 = means.get(agent1_index);
				float std_dev1 = std_devs.get(agent1_index);

				for (int agent2_index = 0; agent2_index <= agent1_index; agent2_index++) {
					float mean2 = means.get(agent2_index);
					float std_dev2 = std_devs.get(agent2_index);

				
					float win_rate_sum = 0;
					float lose_rate_sum = 0;
					float game_count = 0;

					//System.out.println("multiplier: " + multiplier);
					
					final StochasticUCT agent1 = new StochasticUCT(mean1, std_dev1);
					final StochasticUCT agent2 = new StochasticUCT(mean2, std_dev2);
					
					final Game game = GameLoader.loadGameFromName(gameName);
		
					final Trial trial = new Trial(game);
					final Context context = new Context(game, trial);
					
					for (int gameCounter = 0; gameCounter < NUM_GAMES; ++gameCounter)
					{
						game.start(context);
						System.out.println("Game : " + game.name());
		
						final List<AI> ais = listAI(agent1, agent2, gameCounter);
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
						System.out.println("===============================");
						System.out.println("Game ended.");
						System.out.println("===============================");
						game_count += 1;
						if(context.trial().status() != null){
							if((""+context.trial().status()).contains("1") && game_count % 2 == 1) {
								win_rate_sum += 1;
							}
							else if((""+context.trial().status()).contains("2") && game_count % 2 == 0) {
								win_rate_sum += 1;
							}
							if((""+context.trial().status()).contains("1") && game_count % 2 == 0) {
								lose_rate_sum += 1;
							}
							else if((""+context.trial().status()).contains("2") && game_count % 2 == 1) {
								lose_rate_sum += 1;
							}
						}
					}
					
					float win_rate = win_rate_sum/game_count;
					float lose_rate = lose_rate_sum/game_count;
					System.out.println("Win rate:" + win_rate);
					System.out.println("Lose rate:" + lose_rate);
					System.out.println("===============================");
					System.out.println("Game batch ended.");
					System.out.println("===============================");
				}
				
			}
		}
	}

	private static List<AI> listAI(AI ai1, AI ai2, int gameCounter) {
		List<AI> ais = new ArrayList<>();
		ais.add(null);
		ais.add(null);
		ais.add(null);
		ais.set(gameCounter%2 +1, ai1);
		ais.set((1 - gameCounter%2) + 1, ai2);
		return ais;
	}
}